# AAR_PACKAGING_REPORT.md
## Audit of Current Gradle Packaging & Single AAR Task (`bundleSingleAar`)

**Source of Truth:** `app/build.gradle.kts:282–377`, `testApp/build.gradle.kts`  
**Audit Status:** `[VERIFIED]`

---

### 1. Modules & Variants Built by `bundleSingleAar`

The task is configured with:
```kotlin
dependsOn(":app:assembleDebug", ":library:assemble", ":shared:assemble")
```
- `:app:assembleDebug` produces `app/build/outputs/aar/app-debug.aar`
- `:library:assemble` produces `library/build/outputs/aar/library.aar` (or `library-debug.aar`)
- `:shared:assemble` produces `shared/build/outputs/aar/shared.aar` (or `shared-debug.aar`)
- Target output: `app/build/outputs/aar/cs3core.aar` (11.1 MB)

---

### 2. Forensic Preservation Analysis

| Artifact Component | Included in `cs3core.aar`? | Source Module | Defect / Failure Mode | Status |
| :--- | :--- | :--- | :--- | :--- |
| **`classes.jar`** | **YES** | Merged from `:app`, `:library`, `:shared` | Merged using standard zip traversal. Duplicate classes are overwritten or deduplicated. | `[VERIFIED]` |
| **Android Resources (`res/`)** | **PARTIAL** | Taken solely from `:app` | Any resources originating from `:library` or `:shared` are omitted. | `[VERIFIED]` |
| **Symbol File (`R.txt`)** | **PARTIAL** | Taken solely from `:app` | Symbols from `:library` are missing from `R.txt`. Can trigger `NoSuchFieldError` in consumer resource lookups. | `[VERIFIED]` |
| **`AndroidManifest.xml`** | **YES** | Taken solely from `:app` | Manifest from `:library` is not merged; however, `:library` manifest has minimal declarations. | `[VERIFIED]` |
| **`assets/`** | **YES** | Taken from `:app` | Includes `git-hash.txt`. | `[VERIFIED]` |
| **Native Libraries (`jni/`)** | **NO** | None directly in modules | Third-party `.so` files from transitive dependencies (e.g. `torrentserver`) are not copied into the AAR. | `[VERIFIED]` |
| **ProGuard Rules (`proguard.txt`)** | **PARTIAL** | Taken from `:app` | Contains only consumer rules specified in `:app`. | `[VERIFIED]` |
| **Maven POM / Metadata (`.pom`)** | **NO** | Missing | No POM is generated. Consumers using `files("...")` must declare all transitives manually. | `[VERIFIED]` |

---

### 3. Duplicate Class Collisions

- **Audit Finding:**
  Both `:app` and `:library` use namespace `com.lagradost.cloudstream3`.
  - `:library` defines `com.lagradost.cloudstream3.MainAPI`, `APIHolder`, `BasePlugin`.
  - `:app` defines `com.lagradost.cloudstream3.plugins.Plugin`, `PluginManager`, `CloudStreamApp`.
  - There are **zero identical fully-qualified class names** overlapping between `:app` and `:library`.
  - In `bundleSingleAar`, merging `classes.jar` does not overwrite identically named classes because classes are partitioned across distinct source files.

---

### 4. Comparison of Deliverable Strategies

#### Strategy A: Single "Fat" / Repackaged AAR (Current Approach)
- **Pros:** Single file distribution for consumers (`cs3core.aar`).
- **Cons & Fragility:**
  - High maintenance burden: Custom Gradle zip merging tasks must manually handle `R.txt`, resources, and assets.
  - Consumers must manually copy/paste 10+ transitive dependencies into their own `build.gradle.kts`.
  - If a transitive dependency version changes upstream, the consumer app can experience silent bytecode mismatches (`NoSuchMethodError`).

#### Strategy B: Standard Modular AAR Published with Maven Metadata (Recommended Production Pattern)
- **Pros:**
  - Standard Android Gradle Plugin workflow via `maven-publish`.
  - Publishes `cs3core.aar` along with a `.pom` file that tells Gradle exactly which versions of `nicehttp`, `jsoup`, `rhino`, and `kotlinx.serialization` to resolve automatically.
  - Consumer simply writes:
    ```kotlin
    implementation("com.lagradost.cloudstream3:core:1.0.0")
    ```
    and Gradle handles all transitives automatically without manual declaration.
- **Cons:** Requires a local Maven repository folder (`mavenLocal()` or a flat `repo/` directory) or GitHub Packages.

---

### 5. Consumer R8 / ProGuard Configuration

Consumers building release APKs (`isMinifyEnabled = true`) require the following rules:

```proguard
# CloudStream Engine Models & Serializers
-keep class com.lagradost.cloudstream3.** { *; }
-keep interface com.lagradost.cloudstream3.** { *; }
-keep class com.lagradost.api.** { *; }

# Serialization annotations
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod
-keepclassmembers class * {
    @kotlinx.serialization.SerialName <fields>;
    @kotlinx.serialization.Serializable <fields>;
    @com.fasterxml.jackson.annotation.JsonProperty <fields>;
}

# JS Interpreters & DOM Parsers
-keep class org.mozilla.javascript.** { *; }
-keep class org.jsoup.** { *; }
-keep class com.fleeksoft.ksoup.** { *; }

# Dalvik Dynamic Class Loading
-keep class dalvik.system.** { *; }
```
