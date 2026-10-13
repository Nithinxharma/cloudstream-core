# Implementation Plan - Single AAR (`cs3core.aar`) & Test App APK (`cs3core-test.apk`)

## User Requirements Summary
1. **Single AAR Packaging**: Unify the multi-module AAR build outputs (`:app`, `:library`, `:shared`) into ONE single archive: `cs3core.aar`.
2. **Standalone Test App APK**: Build a lightweight developer test harness module (`:testApp`) consuming `cs3core.aar` that generates `cs3core-test.apk`.
3. **CI Pipeline Integration**: Update `.github/workflows/android.yml` to compile both artifacts and output exactly two artifacts: `cs3core.aar` and `cs3core-test.apk`.

---

## Proposed Architectural Changes

### 1. Unified AAR Packaging (`app/build.gradle.kts`)
- Create and maintain the `bundleSingleAar` Gradle task in `app/build.gradle.kts`.
- Task dependencies: `:app:assembleDebug`, `:library:assemble`, `:shared:assemble`.
- Extracts `classes.jar` from `:app`, `:library`, and `:shared`, merges them into a single `classes.jar` without duplicate class collisions, and packages the base archive as `app/build/outputs/aar/cs3core.aar`.
- Fully compatible with Gradle Configuration Cache and zero script object leaks.

### 2. Standalone Test App Module (`:testApp`)
- `testApp/build.gradle.kts`: Configured with `base { archivesName.set("cs3core-test") }` so built APKs are named `cs3core-test-debug.apk` or `cs3core-test.apk`.
- Task dependency: `builtBy(":app:bundleSingleAar")` on `files("../app/build/outputs/aar/cs3core.aar")` ensuring `cs3core.aar` is generated before testApp tasks run.
- `CoreTestActivity.kt` & `activity_core_test.xml`: Standalone test screen with 6 async test routines (Network, Plugins, Search, Details, Stream Link Extraction, Auth/Sync) and a live scrollable monospace execution log console.

### 3. CI Workflow Update (`.github/workflows/android.yml`)
- Single unified GitHub Actions workflow running JDK 17.
- Workflow Steps:
  1. Checkout repository & setup JDK 17.
  2. Run `./gradlew :app:bundleSingleAar --no-configuration-cache` -> generates `cs3core.aar`.
  3. Run `./gradlew :testApp:assembleDebug --no-configuration-cache` -> generates `cs3core-test.apk`.
  4. Upload Artifact 1: `cs3core-aar` (`app/build/outputs/aar/cs3core.aar`).
  5. Upload Artifact 2: `cs3core-test-apk` (`testApp/build/outputs/apk/debug/*.apk`).

---

## Verification Plan

### Automated Build Verification
1. Run `gradle :app:bundleSingleAar` to confirm `app/build/outputs/aar/cs3core.aar` is generated (~11.1 MB).
2. Run `gradle :testApp:assembleDebug` to confirm `testApp/build/outputs/apk/debug/` builds successfully (~19.4 MB).
3. Run `compile_applet` to verify full compilation across all modules.
