# RUNTIME_DEPENDENCY_GRAPH.md
## Runtime Execution Flows & Call-Graph Trace

**Audit Status:** `[VERIFIED]` through source tracing and runtime invocation in `:testApp`.

---

### Flow A: Host Initialization → Context → Storage & Network

```
[Host App / Consumer]
       │
       ▼
CloudStreamApp.attachBaseContext(context) / CloudStreamCore.init(context)
       │
       ├─► WeakReference(context) -> CloudStreamApp.context
       │        │
       │        └─► DataStore.kt
       │                 └─► PreferenceManager.getDefaultSharedPreferences(context)
       │
       └─► initClient(OkHttpClient)
                ├─► addCloudFlareDns()
                ├─► addGoogleDns()
                └─► global app = NiceHttpClient(client)
```

- **Entry Point:** `CloudStreamApp.attachBaseContext(base)` / `CloudStreamApp.onCreate()`.
- **Involved Classes:** `CloudStreamApp.kt:70–160`, `DataStore.kt:1–150`, `CommonRequests.kt:1–100`.
- **Required Dependencies:** `preference-ktx`, `nicehttp`, `conscrypt-android`.
- **Error Paths:**
  - Invoking `DataStore` before `attachBaseContext` or without a valid `Context` returns null or throws `NullPointerException`.
  - DNS resolution failure on custom network configurations falls back to system DNS.
- **Classification:** **Core Mandatory**.

---

### Flow B: Repository Management & Extension Download

```
[Repository URL]
       │
       ▼
RepositoryManager.parseRepository(url)
       │ (HTTP GET via app.get(url))
       ▼
RepositoryManager.getRepoPlugins(repositoryData)
       │ (Parses JSON plugin list -> List<SitePlugin>)
       ▼
RepositoryManager.downloadPluginToFile(sitePlugin.url, tempFile)
       │ (HTTP GET byteStream)
       ▼
RepositoryManager.sha256(tempFile) == sitePlugin.sha256?
       │
       ├─► [MATCH] -> Moves file to ONLINE_PLUGINS_FOLDER/{sanitizedName}.cs3
       │              Persists SitePlugin in DataStore (REPOSITORIES_KEY)
       │
       └─► [MISMATCH] -> Throws Exception, deletes temp file
```

- **Entry Point:** `RepositoryManager.addRepository(repositoryData)` (`RepositoryManager.kt:248`).
- **Involved Classes:** `RepositoryManager.kt:1–266`, `PluginManager.kt:747–790`, `NiceHttp`.
- **Required Dependencies:** `nicehttp`, `kotlinx-serialization-json`, `safefile`.
- **Error Paths:**
  - Corrupt download or SHA-256 hash mismatch throws an error and deletes the downloaded file.
  - Network timeouts during large repository index retrieval.
- **Classification:** **Core Mandatory**. (UI notification for download progress is **Optional**).

---

### Flow C: Dynamic Extension Loading & Provider Registration

```
[.cs3 File on Disk]
       │
       ▼
PluginManager.loadPlugin(context, file, data)
       │
       ├─► file.setReadOnly() (Android 14+ security compliance)
       │
       ├─► val loader = PathClassLoader(file.absolutePath, context.classLoader)
       │
       ├─► loader.getResourceAsStream("manifest.json")
       │        └─► Deserializes BasePlugin.Manifest (pluginClassName, version, requiresResources)
       │
       ├─► val clazz = loader.loadClass(manifest.pluginClassName)
       ├─► val pluginInstance = clazz.getDeclaredConstructor().newInstance() as BasePlugin
       │
       ├─► [If manifest.requiresResources]
       │        └─► AssetManager.addAssetPath(file.absolutePath) -> pluginInstance.resources
       │
       ├─► pluginInstance.load(context)
       │        │
       │        ├─► registerMainAPI(provider)
       │        │        ├─► APIHolder.allProviders.add(provider)
       │        │        └─► APIHolder.addPluginMapping(provider)
       │        │
       │        └─► registerExtractorAPI(extractor)
       │                 └─► extractorApis.add(extractor)
       │
       └─► Cache plugin in PluginManager.plugins[filePath]
```

- **Entry Point:** `PluginManager.loadPlugin(context, file, data)` (`PluginManager.kt:593`).
- **Involved Classes:** `PluginManager.kt:593–670`, `PathClassLoader`, `BasePlugin.kt:1–70`, `Plugin.kt:1–40`, `APIHolder`.
- **Required Dependencies:** `dalvik.system.PathClassLoader`, `kotlin-reflect`, `kotlinx-serialization-json`.
- **Error Paths:**
  - `ClassNotFoundException`: Occurs if the plugin references host classes not exposed in `context.classLoader`.
  - `NoSuchMethodException`: Occurs if method signatures in `BasePlugin` / `MainAPI` differ from upstream.
  - Corrupt zip/jar structure: `manifest.json` not found returns `false` without crashing the host.
- **Classification:** **Core Mandatory**.

---

### Flow D: Provider Operations (Catalog, Search, Metadata)

```
[Provider Invocation]
       │
       ├─► provider.getMainPage(page, request)
       │        └─► Returns List<HomePageList> (SearchResponse items)
       │
       ├─► provider.search(query)
       │        └─► Returns List<SearchResponse> (name, url, posterUrl, type)
       │
       └─► provider.load(url)
                └─► Returns LoadResponse:
                         ├─► MovieLoadResponse (title, plot, dataUrl)
                         ├─► TvSeriesLoadResponse (title, episodes: List<Episode>)
                         └─► AnimeLoadResponse (episodes, dub/sub status)
```

- **Entry Point:** `MainAPI.getMainPage()`, `MainAPI.search()`, `MainAPI.load()` (`MainAPI.kt:430–480`).
- **Involved Classes:** `MainAPI.kt`, `SearchResponse`, `LoadResponse`, `Episode`, `NiceHttp`.
- **Required Dependencies:** `nicehttp`, `jsoup`, `ksoup`, `kotlinx-serialization-json`.
- **Error Paths:**
  - Provider anti-scraping / Cloudflare 403: handled via `NiceHttp` exception wrapping.
  - DOM structure changes on target website: caught by `safeAsync` / `safe` error handlers in `ArchComponentExt.kt`.
- **Classification:** **Core Mandatory**.

---

### Flow E: Link Extraction Engine

```
[Media URL / Content Page]
       │
       ▼
provider.loadLinks(data, isCasting, subtitleCallback, callback)
       │
       ▼
loadExtractor(url, referer, subtitleCallback, callback)
       │
       ▼
Iterate extractorApis -> find matching ExtractorApi (canExtract(url) == true)
       │
       ▼
extractor.getSafeUrl(url, referer, subtitleCallback, callback)
       │
       ├─► HTTP requests via app.get() / app.post()
       ├─► Decrypt obfuscated code via JsInterpreter (Rhino) / JsUnpacker
       │
       ├─► Emits ExtractorLink (url, quality, isM3u8, headers) -> callback(link)
       └─► Emits SubtitleFile (lang, url) -> subtitleCallback(sub)
```

- **Entry Point:** `loadExtractor(url, referer, subtitleCallback, callback)` (`ExtractorApi.kt:32`).
- **Involved Classes:** `ExtractorApi.kt`, `extractors/*.kt`, `JsInterpreter.kt`, `JsUnpacker.kt`, `M3u8Helper.kt`.
- **Required Dependencies:** `rhino`, `nicehttp`, `jsoup`, `ksoup`.
- **Error Paths:**
  - Stream offline or deleted: returns without emitting `ExtractorLink`.
  - JS eval syntax error in obfuscated payload: safely caught by `JsInterpreter` try-catch.
- **Classification:** **Core Mandatory**.

---

### Flow F: Process Restart & State Hydration

```
[Process Restart]
       │
       ▼
CloudStreamApp.onCreate()
       │
       ├─► Read persisted repositories:
       │        RepositoryManager.getRepositories()
       │        (queries DataStore for REPOSITORIES_KEY)
       │
       └─► Hydrate local extensions:
                PluginManager.___DO_NOT_CALL_FROM_A_PLUGIN_loadAllLocalPlugins(context, forceReload = false)
                ├─► Lists files in context.filesDir/ONLINE_PLUGINS_FOLDER
                └─► Executes Flow C for each .cs3 file
                         └─► Repopulates APIHolder.apis and extractorApis
```

- **Entry Point:** `PluginManager.___DO_NOT_CALL_FROM_A_PLUGIN_loadAllLocalPlugins(context)`.
- **Involved Classes:** `PluginManager.kt:506–550`, `RepositoryManager.kt:242`, `DataStore.kt`.
- **Required Dependencies:** `preference-ktx`, `safefile`, `kotlinx-serialization-json`.
- **Error Paths:**
  - Plugin file deleted externally: skipped cleanly with warning log.
  - Corrupted shared preferences: resets to `PREBUILT_REPOSITORIES`.
- **Classification:** **Core Mandatory**.
