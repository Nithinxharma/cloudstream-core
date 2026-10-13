# CloudStream Core (.AAR) Integration Guide

This guide provides step-by-step instructions for integrating the headless `cloudstream-core` Android Library (`.aar`) into any new or existing Android application.

---

## 1. Importing the `.aar` File

### Step 1: Copy the Library File
After building (`./gradlew :app:assembleRelease`), locate `app-release.aar` in `app/build/outputs/aar/`. 
Rename and copy it to your host project's `libs/` folder:
```
YourApp/
├── app/
│   ├── libs/
│   │   └── cloudstream-core.aar
│   └── build.gradle.kts
```

### Step 2: Configure `build.gradle.kts`
In your consumer application module `app/build.gradle.kts`, configure the dependency repository and add the `.aar` reference alongside its required transitive runtime libraries:

```kotlin
repositories {
    google()
    mavenCentral()
    maven("https://jitpack.io")
    flatDir {
        dirs("libs")
    }
}

dependencies {
    // 1. Core AAR Dependency
    implementation(files("libs/cloudstream-core.aar"))
    // Alternatively if using flatDir:
    // implementation(name = "cloudstream-core", ext = "aar")

    // 2. Transitive Networking & DoH
    implementation("com.squareup.okhttp3:okhttp:5.0.0-alpha.14")
    implementation("com.squareup.okhttp3:okhttp-dnsoverhttps:5.0.0-alpha.14")
    implementation("org.conscrypt:conscrypt-android:2.5.2")

    // 3. HTML Parsing & JS Engine (Rhino)
    implementation("org.jsoup:jsoup:1.17.2")
    implementation("org.mozilla:rhino:1.7.15")

    // 4. Serialization & Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.17.1")

    // 5. Image & Caching (Coil 3)
    implementation("io.coil-kt.coil3:coil:3.0.0-rc01")
    implementation("io.coil-kt.coil3:coil-network-okhttp:3.0.0-rc01")

    // 6. AndroidX Lifecycle & WorkManager
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.4")
    implementation("androidx.preference:preference-ktx:1.2.1")
    implementation("androidx.work:work-runtime-ktx:2.9.1")

    // 7. Video Playback (Optional for consumer UI)
    implementation("androidx.media3:media3-exoplayer:1.4.1")
    implementation("androidx.media3:media3-exoplayer-hls:1.4.1")
    implementation("androidx.media3:media3-ui:1.4.1")
}
```

---

## 2. Permissions & Manifest Setup

Add the required permissions in your consumer application's `AndroidManifest.xml`:

```xml
<manifest xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:tools="http://schemas.android.com/tools">

    <!-- Essential Internet & Network Connectivity -->
    <uses-permission android:name="android.permission.INTERNET" />
    <uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />

    <!-- Storage & Downloads -->
    <uses-permission android:name="android.permission.READ_MEDIA_VIDEO" />
    <uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
    <uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
    <uses-permission android:name="android.permission.FOREGROUND_SERVICE_DATA_SYNC" />

    <application
        android:name=".MyApplication"
        android:usesCleartextTraffic="true"
        android:largeHeap="true">
        
        <!-- Registration for Developer Test Screen (Optional) -->
        <activity
            android:name="com.lagradost.cloudstream3.test.CoreTestActivity"
            android:exported="true">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>
    </application>
</manifest>
```

---

## 3. Engine Initialization

Initialize the core client inside your application's `Application.onCreate()`:

```kotlin
package com.example.myapp

import android.app.Application
import com.lagradost.cloudstream3.app
import com.lagradost.cloudstream3.network.initClient

class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        // 1. Initialize HTTP client with Conscrypt SSL and cache
        app.initClient(applicationContext)

        // 2. (Optional) Initialize plugins in background if using extensions
        // PluginManager.___DO_NOT_CALL_FROM_A_PLUGIN_loadAllLocalPlugins(this, false)
    }
}
```

---

## 4. End-to-End Code Examples (Wiring the Connections)

### Connection A: Plugins & Repository Manager
Manage repositories and load dynamic `.cs3` plugins:

```kotlin
import com.lagradost.cloudstream3.plugins.RepositoryManager
import com.lagradost.cloudstream3.plugins.RepositoryData
import com.lagradost.cloudstream3.plugins.PluginManager

// 1. Fetch available plugins from a remote repository URL
suspend fun fetchPluginsFromRepo(repoUrl: String) {
    val repoData = RepositoryData(
        name = "Community Repository",
        url = repoUrl
    )
    val plugins = RepositoryManager.getRepoPlugins(repoData)
    plugins?.forEach { pluginWrapper ->
        println("Found plugin: ${pluginWrapper.name} (${pluginWrapper.url})")
    }
}

// 2. Download and install a plugin
suspend fun installPlugin(context: Context, pluginUrl: String) {
    val success = RepositoryManager.downloadPluginToFile(context, pluginUrl)
    if (success) {
        // Load downloaded plugins into runtime DexClassLoader
        PluginManager.___DO_NOT_CALL_FROM_A_PLUGIN_loadAllLocalPlugins(context, false)
    }
}
```

---

### Connection B: Media Search (`search()`)
Search content across providers and map to UI items:

```kotlin
import com.lagradost.cloudstream3.metaproviders.TmdbProvider
import com.lagradost.cloudstream3.SearchResponse

data class MediaCardItem(
    val title: String,
    val posterUrl: String?,
    val mediaUrl: String,
    val type: String
)

suspend fun searchMedia(query: String): List<MediaCardItem> {
    val provider = TmdbProvider() // or any registered MainAPI instance
    val searchResults: List<SearchResponse>? = provider.search(query)

    return searchResults?.map { result ->
        MediaCardItem(
            title = result.name,
            posterUrl = result.posterUrl,
            mediaUrl = result.url,
            type = result.type.name
        )
    } ?: emptyList()
}
```

---

### Connection C: Media Details & Episodes (`load()`)
Retrieve synopses, season/episode lists, and stream identifiers:

```kotlin
import com.lagradost.cloudstream3.LoadResponse
import com.lagradost.cloudstream3.TvSeriesLoadResponse
import com.lagradost.cloudstream3.MovieLoadResponse

suspend fun loadMediaDetails(url: String) {
    val provider = TmdbProvider()
    val details: LoadResponse? = provider.load(url)

    when (details) {
        is TvSeriesLoadResponse -> {
            println("Series: ${details.name}, Total Episodes: ${details.episodes.size}")
            details.episodes.forEach { ep ->
                println("S${ep.season}E${ep.episode}: ${ep.name} (Data/URL: ${ep.data})")
            }
        }
        is MovieLoadResponse -> {
            println("Movie: ${details.name}, Duration: ${details.duration} min")
            println("Stream Data ID: ${details.data}")
        }
        else -> {
            println("Loaded media: ${details?.name}")
        }
    }
}
```

---

### Connection D: Stream Link Extraction (`loadLinks()`)
Extract direct `.m3u8` or `.mp4` URLs and stream directly with ExoPlayer:

```kotlin
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import com.lagradost.cloudstream3.utils.loadExtractor
import com.lagradost.cloudstream3.utils.ExtractorLink

suspend fun playVideoLink(
    videoUrl: String,
    player: ExoPlayer
) {
    val streamLinks = mutableListOf<ExtractorLink>()

    loadExtractor(
        url = videoUrl,
        referer = "https://google.com",
        subtitleCallback = { subtitleFile ->
            println("Found subtitle: [${subtitleFile.lang}] ${subtitleFile.url}")
        },
        callback = { link ->
            println("Found video stream: ${link.name} (${link.quality}p) -> ${link.url}")
            streamLinks.add(link)
        }
    )

    // Select highest quality or first resolved stream
    val bestStream = streamLinks.maxByOrNull { it.quality } ?: streamLinks.firstOrNull()

    bestStream?.let { link ->
        val mediaItem = MediaItem.Builder()
            .setUri(link.url)
            .build()
        
        player.setMediaItem(mediaItem)
        player.prepare()
        player.play()
    }
}
```

---

### Connection E: Accounts & Sync (Trakt / MyAnimeList)
Connect user tracking accounts without UI popups:

```kotlin
import com.lagradost.cloudstream3.syncproviders.AccountManager
import com.lagradost.cloudstream3.syncproviders.AuthData

// 1. Get OAuth login URL
fun getMalLoginUrl(): String {
    return AccountManager.malApi.loginUrl
}

// 2. Exchange OAuth code for token
suspend fun authenticateMal(authCode: String) {
    val success = AccountManager.malApi.handleRedirect(authCode)
    if (success) {
        println("Successfully authenticated with MyAnimeList!")
    }
}

// 3. Inspect registered accounts
fun checkAccounts() {
    val malAccounts: Array<AuthData> = AccountManager.accounts(AccountManager.malApi.idPrefix)
    println("Active MAL accounts: ${malAccounts.size}")
}
```

---

## 5. ProGuard / R8 Rules

Add the following rules to your consumer application's `proguard-rules.pro` to preserve data models, serialization schemas, and reflection-based dynamic providers:

```proguard
# CloudStream Core Models & Serialization
-keep class com.lagradost.cloudstream3.** { *; }
-keep interface com.lagradost.cloudstream3.** { *; }

# Preserve Kotlinx Serialization & Jackson annotations
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod
-keepclassmembers class * {
    @com.fasterxml.jackson.annotation.JsonProperty *;
    @kotlinx.serialization.SerialName *;
    @kotlinx.serialization.Serializable *;
}

# Preserve Rhino JavaScript Engine
-keep class org.mozilla.javascript.** { *; }

# Preserve OkHttp & Conscrypt
-keep class okhttp3.** { *; }
-keep class org.conscrypt.** { *; }

# Preserve Jsoup
-keep class org.jsoup.** { *; }

# Preserve Dynamic Plugin Classloading
-keep class * extends com.lagradost.cloudstream3.MainAPI { *; }
-keep class * extends com.lagradost.cloudstream3.utils.ExtractorApi { *; }
```
