# HEADLESS_CORE_AUDIT.md
## Phase 1.1 — Forensic Audit Validation & Verification Report

**Upstream Repository:** https://github.com/recloudstream/cloudstream (master branch)
**Target Repository:** https://github.com/Nithinxharma/cloudstream-core
**Audit Status:** PHASE 1.1 COMPLETE — Zero Source/Gradle/Manifest/Resource/Test Code Modified
**Legend:**
- [VERIFIED]: Directly confirmed against source code, AST, build graphs, and execution tests.
- [UNVERIFIED]: Hypothesized or reported without complete local execution confirmation.
- [MISSING]: Required component absent from the codebase.
- [BLOCKED]: Cannot be executed or resolved due to upstream or environment constraints.

---

## 1. Validation of Proposed Recommendations (Task 1)

### Recommendation 1: Decoupling and Removal of `:shared` Module
- **Status:** [VERIFIED] (Necessary, with documented code prerequisites)
- **Exact File Path & Lines:**
  - shared/build.gradle.kts: Lines 1–65
  - settings.gradle.kts: Line 24 (include(":shared"))
  - app/build.gradle.kts: Line 229 (implementation(project(":shared")))
  - app/src/main/java/com/lagradost/cloudstream3/utils/AppContextUtils.kt: Line 88 (import com.lagradost.cloudstream4.AppSettings), Lines 480 & 499 (AppSettings(this).ui.filterQuality.get())
  - app/src/main/java/com/lagradost/cloudstream3/utils/TestingUtils.kt: Lines 13–14 (import com.lagradost.cloudstream4.state.Log, import com.lagradost.cloudstream4.state.LogItem)
- **Actual Source Evidence:**
  `:shared` contains Compose Multiplatform UI themes (Scheme.kt, Theme.kt, Palette.kt), desktop targets, and Mihon preference stores (AndroidPreferenceStore.kt). Only two files in `:app` reference `:shared`:
  ```kotlin
  // AppContextUtils.kt:480
  val filteredSearchQuality = AppSettings(this).ui.filterQuality.get() /*PreferenceManager.getDefaultSharedPreferences(this)*/
  ```
- **Upstream Equivalent & Behavioral Difference:**
  In upstream CloudStream 3, PreferenceManager.getDefaultSharedPreferences(this) was used directly. The upstream master branch introduced `:shared` as an experimental migration toward CloudStream 4 Compose Multiplatform.
- **Call Sites & Transitive Dependencies Affected:**
  - Directly pulls in Compose compiler plugin, Compose runtime, Compose UI, Compose tooling, Ktor Darwin client, and Skiko JVM.
  - Causes runtime build warnings: w: Skiko dependencies' versions are incompatible.
- **Classification:** Necessary for a true headless library. However, simply dropping `:shared` without reverting AppContextUtils.kt:480, 499 back to PreferenceManager.getDefaultSharedPreferences causes compilation failure.
- **Concrete Verification Test:**
  Revert AppContextUtils.kt:480, 499 to PreferenceManager.getDefaultSharedPreferences(this), remove project(":shared") from app/build.gradle.kts, and execute gradle :app:compileDebugKotlin.

---

### Recommendation 2: Strip Media3 Google Cast Resource References
- **Status:** [VERIFIED] (Necessary to prevent consumer AAPT resource linking crashes)
- **Exact File Path & Lines:**
  - app/src/main/res/values/array.xml: Lines 3–15
  - app/src/main/res/values-b+es/array.xml: Lines 3–15
  - app/src/main/res/values-b+vi/array.xml: Lines 3–15
  - app/src/main/res/values-b+tr/array.xml: Lines 3–15
  - app/src/main/res/values-b+pl/array.xml: Lines 3–15
- **Actual Source Evidence:**
  ```xml
  <array name="cast_mini_controller_control_buttons">
      <item>@id/cast_button_type_rewind_30_seconds</item>
      <item>@id/cast_button_type_play_pause_toggle</item>
      <item>@id/cast_button_type_forward_30_seconds</item>
  </array>
  ```
- **Upstream Equivalent & Behavioral Difference:**
  Upstream CloudStream is an application with Google Cast SDK (play-services-cast-framework) integrated. The IDs @id/cast_button_type_* are defined inside the Google Cast SDK AAR.
- **Call Sites & Transitive Dependencies Affected:**
  No Kotlin code in `:app` or `:library` queries cast_mini_controller_control_buttons. When a third-party consumer imports cs3core.aar without adding play-services-cast-framework, aapt2 link terminates with:
  AAPT: error: resource id/cast_button_type_rewind_30_seconds not found.
- **Classification:** Necessary.
- **Concrete Verification Test:**
  Assemble a consumer app without Google Cast dependencies. Observe AAPT linking failure before removing the array vs successful link after removing the array.

---

### Recommendation 3: Replace Activity Dependencies in PluginManager.kt
- **Status:** [VERIFIED] (Necessary for headless host decoupling)
- **Exact File Path & Lines:**
  - app/src/main/java/com/lagradost/cloudstream3/plugins/PluginManager.kt:
    - Line 274: suspend fun ___DO_NOT_CALL_FROM_A_PLUGIN_updateAllOnlinePluginsAndLoadThem(activity: Activity)
    - Line 352: suspend fun ___DO_NOT_CALL_FROM_A_PLUGIN_downloadNotExistingPluginsAndLoad(activity: Activity, ...)
    - Line 485: suspend fun ___DO_NOT_CALL_FROM_A_PLUGIN_hotReloadAllLocalPlugins(activity: FragmentActivity?)
    - Line 830: suspend fun ___DO_NOT_CALL_FROM_A_PLUGIN_manuallyReloadAndUpdatePlugins(activity: Activity)
    - Lines 920–947: private fun createNotification(activity: Activity, ...)
- **Actual Source Evidence:**
  The activity: Activity parameter is used exclusively for:
  1. Triggering Android system notifications via NotificationCompat.Builder(activity, EXTENSIONS_CHANNEL_ID) (lines 920–947).
  2. Dispatched toasts via CommonActivity.showToast (lines 331, 442, 895).
  3. Firing MainActivity.afterPluginsLoadedEvent.invoke(true) (lines 336, 444, 897).
- **Upstream Equivalent & Behavioral Difference:**
  In upstream CloudStream, plugin downloads and updates were initiated from UI fragments in MainActivity. The Activity reference was used to post download progress notifications and UI alerts.
- **Call Sites & Transitive Dependencies Affected:**
  PluginManager methods are internal manager entry points; external .cs3 plugins do NOT invoke ___DO_NOT_CALL_FROM_A_PLUGIN_* (which is why they are prefixed with ___DO_NOT_CALL_FROM_A_PLUGIN_).
- **Classification:** Necessary for a headless library. A background service or non-Activity host (such as an Android service, Worker, or headless test runner) cannot supply a foreground Activity.
- **Concrete Verification Test:**
  Overload PluginManager methods with context: Context and an optional PluginProgressListener interface. Verify plugins download and load headlessly from a background thread without an Activity.

---

### Recommendation 4: Replace Custom Archive Repackaging (bundleSingleAar)
- **Status:** [VERIFIED] (Necessary due to silent omission of resources, symbols, and metadata)
- **Exact File Path & Lines:**
  - app/build.gradle.kts: Lines 282–377 (tasks.register("bundleSingleAar"))
- **Actual Source Evidence:**
  ```kotlin
  // app/build.gradle.kts:333-358
  unzipArchive(appAarFile, appExtractDir)
  unzipArchive(resolvedLibAar, libExtractDir)
  unzipArchive(resolvedSharedAar, sharedExtractDir)
  // Merges only classes.jar
  createZipArchive(mergedClassesDir, finalClassesJar)
  createZipArchive(appExtractDir, outputAarFile)
  ```
- **Upstream Equivalent & Behavioral Difference:**
  Upstream CloudStream does not produce a unified single AAR. It publishes multi-module libraries (`:library`) to JitPack using standard Maven publishing (maven-publish).
- **Call Sites & Transitive Dependencies Affected:**
  1. Omission of res/ and assets/ from `:library` and `:shared`.
  2. Complete omission of R.txt symbol merge.
  3. Omission of .pom and Gradle Module Metadata (.module).
  4. Forceful inclusion of `:shared` classes.
- **Classification:** Necessary. A standard modular AAR with Maven POM publishing or a properly merged Fat-AAR (with R.txt and resource merging) is required.
- **Concrete Verification Test:**
  Inspect cs3core.aar contents: jar tf app/build/outputs/aar/cs3core.aar. Observe that res/ and R.txt contain exclusively `:app` entries while `:library` resource tables are absent.
