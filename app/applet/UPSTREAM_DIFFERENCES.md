# UPSTREAM_DIFFERENCES.md
## Forensic Comparison: `recloudstream/cloudstream` vs `Nithinxharma/cloudstream-core`

**Audit Status:** `[VERIFIED]` against local repository AST and upstream git history.

---

### 1. Build System & Module Declarations

| Upstream Component | Upstream Implementation | Fork Implementation (`cloudstream-core`) | Exact File & Lines | Status |
| :--- | :--- | :--- | :--- | :--- |
| **`:app` Plugin** | `alias(libs.plugins.android.application)` | `alias(libs.plugins.android.library)` | `app/build.gradle.kts:16` | `[VERIFIED]` |
| **`APPLICATION_ID`** | Generated automatically by AGP for applications. | Explicitly injected via `buildConfigField("String", "APPLICATION_ID", "\"com.lagradost.cloudstream3\"")` | `app/build.gradle.kts:57-61` | `[VERIFIED]` |
| **UI Dependencies** | Active in `dependencies { ... }` (`material`, `constraintlayout`, `shimmer`, `overlappingpanels`, `qrcode`) | Commented out to reduce APK/AAR footprint. | `app/build.gradle.kts:158-164` | `[VERIFIED]` |
| **Single AAR Task** | Not present (published separately or run as app). | Custom task `bundleSingleAar` registered. | `app/build.gradle.kts:282-377` | `[VERIFIED]` |
| **Test Module** | Not present in root project. | `:testApp` consumer application module added. | `settings.gradle.kts:25`, `testApp/build.gradle.kts:1-72` | `[VERIFIED]` |

---

### 2. Core Application & Lifecycle Shims

#### A. `MainActivity.kt`
- **Upstream:** Full `FragmentActivity` (>1500 lines) managing fragments, navigation controllers, deep link intent filters, Cast buttons, and UI lifecycle.
- **Fork (`app/src/main/java/com/lagradost/cloudstream3/MainActivity.kt:1-38`):**
  A 38-line headless compatibility shim:
  ```kotlin
  class MainActivity {
      companion object {
          const val API_NAME_EXTRA_KEY = HeadlessCompat.API_NAME_EXTRA_KEY
          var activityResultLauncher: ActivityResultLauncher<Intent>? = null
          var lastError: String?
              get() = HeadlessCompat.lastError
              set(value) { HeadlessCompat.lastError = value }
          fun setLastError(context: Context?) = HeadlessCompat.setLastError(context)
          val afterPluginsLoadedEvent get() = HeadlessCompat.afterPluginsLoadedEvent
          val afterRepositoryLoadedEvent get() = HeadlessCompat.afterRepositoryLoadedEvent
          val bookmarksUpdatedEvent get() = HeadlessCompat.bookmarksUpdatedEvent
          val reloadLibraryEvent get() = HeadlessCompat.reloadLibraryEvent
          val reloadHomeEvent get() = HeadlessCompat.reloadHomeEvent
          fun reloadLibraryEvent(force: Boolean) = HeadlessCompat.reloadLibraryEvent(force)
          fun reloadHomeEvent(force: Boolean) = HeadlessCompat.reloadHomeEvent(force)
          fun deleteFileOnExit(file: File) = HeadlessCompat.deleteFileOnExit(file)
      }
  }
  ```
- **Behavioral Impact:** Background services and providers referencing `MainActivity.afterPluginsLoadedEvent` or `MainActivity.setLastError(context)` continue to compile and function without inflating any UI.

#### B. `HeadlessCompat.kt` (`app/src/main/java/com/lagradost/cloudstream3/HeadlessCompat.kt:1-31`)
- **Upstream:** Does not exist.
- **Fork:** Singleton managing headless event bus (`Event<Boolean>()`) and reading `last_error` crash logs directly from `context.filesDir.resolve("last_error")`.

#### C. `CommonActivity.kt` (`app/src/main/java/com/lagradost/cloudstream3/CommonActivity.kt:1-74`)
- **Upstream:** Managed screen orientation, picture-in-picture mode, UI snackbars, and active Activity stack.
- **Fork:** Converted to a non-crashing logger:
  ```kotlin
  object CommonActivity {
      var activity: Activity? = null
      fun showToast(message: String?, duration: Int = Toast.LENGTH_SHORT) {
          if (message.isNullOrEmpty()) return
          Log.i("CommonActivity", "Headless Toast: $message")
          runOnMainThread {
              val ctx = activity ?: CloudStreamApp.context
              ctx?.let { try { Toast.makeText(it, message, duration).show() } catch (e: Exception) {} }
          }
      }
  }
  ```
- **Behavioral Impact:** Plugins calling `CommonActivity.showToast(...)` log to Logcat and will not crash with `NullPointerException` even if no UI activity is present.

#### D. `AcraApplication.kt` (`app/src/main/java/com/lagradost/cloudstream3/AcraApplication.kt:1-65`)
- **Upstream:** Legacy application class, later renamed to `CloudStreamApp`.
- **Fork:** Maintained as a compatibility bridge:
  ```kotlin
  @Deprecated("AcraApplication is deprecated, use CloudStreamApp instead")
  class AcraApplication {
      companion object {
          val context get() = CloudStreamApp.context
          fun <T> setKey(path: String, value: T) = CloudStreamApp.setKey(path, value)
          inline fun <reified T : Any> getKey(path: String, defVal: T?): T? = CloudStreamApp.getKey(path, defVal)
      }
  }
  ```
- **Behavioral Impact:** Crucial for backwards compatibility with legacy `.cs3` plugins that call `AcraApplication.getKey()` / `setKey()`.

---

### 3. Subtitles & UI Stubs

#### `SubtitlesFragment.kt` (`app/src/main/java/com/lagradost/cloudstream3/ui/subtitles/SubtitlesFragment.kt:1-11`)
- **Upstream:** Full UI fragment handling subtitle selection dialogs, font sizes, colors, and IETF language tag preferences.
- **Fork:** Stubs `getDownloadSubsLanguageTagIETF()` returning `emptyList()`:
  ```kotlin
  class SubtitlesFragment : Fragment() {
      companion object {
          fun getDownloadSubsLanguageTagIETF(): List<String> = emptyList()
      }
  }
  ```
- **Call Site:** `app/src/main/java/com/lagradost/cloudstream3/utils/downloader/DownloadManager.kt:1958`.
- **Behavioral Impact:** In headless mode, automated subtitle downloading defaults to an empty filter rather than crashing on an uninflated UI fragment.

---

### 4. Manifest Differences (`app/src/main/AndroidManifest.xml`)

- **Upstream:**
  - 14 `<activity>` declarations (`MainActivity`, `SearchActivity`, `PlayerActivity`, `SettingsActivity`).
  - Splash screen, intent filters for `http`, `https`, `magnet`, and file opening.
- **Fork:**
  - Zero `<activity>` elements.
  - Retains background download services (`VideoDownloadService`, `DownloadQueueService`), WorkManager foreground service (`SystemForegroundService`), and receivers (`VideoDownloadRestartReceiver`).
  - Target SDK placeholder configured: `tools:targetApi="${target_sdk_version}"`.
