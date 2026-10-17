# HEADLESS_SDK_CONTRACT.md
## Proposed Minimal Headless CloudStream Core SDK Contract

**Design Goal:** Expose a clean, idiomatic Kotlin API boundary for any external Android host without fragment, Activity, or UI coupling, while reusing upstream data models directly to guarantee 100% binary compatibility with existing `.cs3` plugins.

---

### 1. Main SDK Entry Point: `CloudStreamCore`

```kotlin
package com.lagradost.cloudstream3.core

import android.content.Context
import com.lagradost.cloudstream3.APIHolder
import com.lagradost.cloudstream3.MainAPI
import com.lagradost.cloudstream3.plugins.PluginManager
import com.lagradost.cloudstream3.plugins.RepositoryManager
import com.lagradost.cloudstream3.ui.settings.extensions.RepositoryData
import kotlinx.coroutines.flow.StateFlow
import okhttp3.OkHttpClient

interface CloudStreamCore {

    /**
     * Initializes the headless runtime with the host Application context.
     * Sets up DNS-over-HTTPS, local storage, and loads persisted plugins.
     */
    suspend fun initialize(
        context: Context,
        customOkHttpClient: OkHttpClient? = null,
        autoLoadPlugins: Boolean = true
    )

    /** Closes runtime resources, clears active in-memory providers and caches. */
    fun shutdown()

    // Subsystem Accessors
    val repositories: RepositorySubsystem
    val extensions: ExtensionSubsystem
    val providers: ProviderSubsystem
    val media: MediaExtractionSubsystem
    val events: CoreEventBus

    companion object {
        val instance: CloudStreamCore by lazy { CloudStreamCoreImpl() }
    }
}
```

---

### 2. Subsystem Contracts

#### A. Repository Subsystem (`repositories`)
```kotlin
interface RepositorySubsystem {
    /** Returns all configured repositories (persisted + prebuilt). */
    fun getRepositories(): List<RepositoryData>

    /** Fetches and adds a new repository by URL. */
    suspend fun addRepository(url: String): Result<RepositoryData>

    /** Removes a repository and optionally uninstalls its installed plugins. */
    suspend fun removeRepository(context: Context, repository: RepositoryData): Boolean

    /** Refreshes online plugin lists across all repositories. */
    suspend fun refreshRepositories(): Result<List<RepositoryData>>
}
```

#### B. Extension Subsystem (`extensions`)
```kotlin
interface ExtensionSubsystem {
    /** Lists all locally installed and active plugins. */
    fun getInstalledPlugins(): List<PluginData>

    /** Queries remote repositories for all available extensions. */
    suspend fun getAvailableOnlinePlugins(): List<PluginWrapper>

    /** Downloads, verifies SHA-256, installs, and loads an extension (.cs3). */
    suspend fun installPlugin(
        context: Context,
        sitePlugin: SitePlugin,
        onProgress: ((progress: Float) -> Unit)? = null
    ): Result<Boolean>

    /** Unloads an active extension from runtime and deletes its local file. */
    suspend fun uninstallPlugin(pluginData: PluginData): Boolean

    /** Reloads all installed local plugins from disk. */
    suspend fun reloadAllLocalPlugins(context: Context, force: Boolean = false)
}
```

#### C. Provider Subsystem (`providers`)
```kotlin
interface ProviderSubsystem {
    /** Returns all currently active and loaded MainAPI providers. */
    fun getActiveProviders(): List<MainAPI>

    /** Looks up an active provider by exact name. */
    fun getProviderByName(name: String): MainAPI?

    /** Searches a specific provider or across all providers. */
    suspend fun search(
        provider: MainAPI,
        query: String
    ): Result<List<SearchResponse>>

    /** Fetches the homepage catalog for a provider. */
    suspend fun getHomePage(
        provider: MainAPI,
        page: Int = 1,
        request: MainPageRequest? = null
    ): Result<List<HomePageList>>

    /** Fetches full content details, seasons, and episodes. */
    suspend fun loadDetails(
        provider: MainAPI,
        url: String
    ): Result<LoadResponse?>
}
```

#### D. Media Extraction Subsystem (`media`)
```kotlin
interface MediaExtractionSubsystem {
    /**
     * Resolves all stream links and subtitles for a given episode/content URL.
     * Invokes callbacks in real time as links and subtitles are discovered.
     */
    suspend fun loadLinks(
        provider: MainAPI,
        contentData: String,
        onLinkFound: (ExtractorLink) -> Unit,
        onSubtitleFound: (SubtitleFile) -> Unit
    ): Result<Boolean>
}
```

#### E. Core Event Bus (`events`)
```kotlin
interface CoreEventBus {
    /** Stream of runtime diagnostic logs and status events. */
    val logEvents: StateFlow<CoreLogEvent?>

    /** Emitted whenever the registered provider list changes. */
    val providersChangedEvent: StateFlow<List<MainAPI>>

    /** Emitted on uncaught provider or extractor errors. */
    val errorEvents: StateFlow<CoreErrorEvent?>
}
```

---

### 3. Separation of Concerns Matrix

| Capability | Core Headless Contract | Optional Add-on / Delegated Host Service |
| :--- | :--- | :--- |
| **Plugin Loading & ClassLoader** | **MANDATORY CORE** | — |
| **Repository Sync & Checksum** | **MANDATORY CORE** | — |
| **Search & Catalog** | **MANDATORY CORE** | — |
| **Link Extraction (M3U8/MP4)** | **MANDATORY CORE** | — |
| **Subtitle Parsing** | **MANDATORY CORE** | — |
| **Local Persistence (DataStore)** | **MANDATORY CORE** | Optional external SQL store |
| **Background Video Downloader** | — | **Optional Service** (`DownloadManager.kt`) |
| **Third-Party Sync (Trakt/MAL)** | — | **Optional Service** (`AccountManager.kt`) |
| **System Notifications** | — | **Host Responsibility** via callbacks |
| **ExoPlayer Video Playback** | — | **Host Responsibility** (Media3 in Host) |
| **UI Presentation & Fragments** | — | **Host Responsibility** (Compose/Views) |
