import com.android.build.gradle.internal.cxx.configure.gradleLocalProperties
import org.jetbrains.dokka.gradle.engine.parameters.KotlinPlatform
import org.jetbrains.dokka.gradle.engine.parameters.VisibilityModifier
import org.jetbrains.kotlin.gradle.dsl.JvmDefaultMode
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.dokka)
    alias(libs.plugins.kotlin.serialization)
    // Compose compiler plugin disabled for headless library
}

val javaTarget = JvmTarget.fromTarget(libs.versions.jvmTarget.get())

abstract class GenerateGitHashTask : DefaultTask() {
    @get:OutputDirectory
    abstract val outputDir: DirectoryProperty

    @TaskAction
    fun generate() {
        val outFile = outputDir.file("git-hash.txt").get().asFile
        outFile.parentFile.mkdirs()
        outFile.writeText("dev")
    }
}

val generateGitHash = tasks.register<GenerateGitHashTask>("generateGitHash") {
    outputDir.set(layout.buildDirectory.dir("generated/git"))
}

android {
    @Suppress("UnstableApiUsage")
    testOptions {
        unitTests.isReturnDefaultValues = true
    }

    androidComponents {
        onVariants { variant ->
            variant.sources.assets?.addGeneratedSourceDirectory(
                generateGitHash,
                GenerateGitHashTask::outputDir
            )
        }
    }

    compileSdk = libs.versions.compileSdk.get().toInt()

    defaultConfig {
        minSdk = libs.versions.minSdk.get().toInt()

        manifestPlaceholders["target_sdk_version"] = libs.versions.targetSdk.get()

        buildConfigField(
            "String",
            "APPLICATION_ID",
            "\"com.lagradost.cloudstream3\""
        )

        // Reads local.properties
        val localProperties = gradleLocalProperties(rootDir, project.providers)

        val simklId = System.getenv("SIMKL_CLIENT_ID") ?: (localProperties["simkl.id"] as? String) ?: ""
        val simklSecret = System.getenv("SIMKL_CLIENT_SECRET") ?: (localProperties["simkl.secret"] as? String) ?: ""
        val malKey = System.getenv("MAL_KEY") ?: (localProperties["mal.key"] as? String) ?: ""
        val anilistKey = System.getenv("ANILIST_KEY") ?: (localProperties["anilist.key"] as? String) ?: ""

        buildConfigField(
            "long",
            "BUILD_DATE",
            "${System.currentTimeMillis()}L"
        )
        buildConfigField(
            "String",
            "SIMKL_CLIENT_ID",
            "\"$simklId\""
        )
        buildConfigField(
            "String",
            "SIMKL_CLIENT_SECRET",
            "\"$simklSecret\""
        )
        buildConfigField(
            "String",
            "MAL_KEY",
            "\"$malKey\""
        )
        buildConfigField(
            "String",
            "ANILIST_KEY",
            "\"$anilistKey\""
        )
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        isCoreLibraryDesugaringEnabled = true
        sourceCompatibility = JavaVersion.toVersion(javaTarget.target)
        targetCompatibility = JavaVersion.toVersion(javaTarget.target)
    }

    java {
        toolchain {
            languageVersion.set(JavaLanguageVersion.of(libs.versions.jdkToolchain.get()))
        }
    }

    lint {
        checkReleaseBuilds = false
    }

    buildFeatures {
        buildConfig = true
        viewBinding = false
    }

    packaging {
        jniLibs {
            useLegacyPackaging = true
        }
    }

    namespace = "com.lagradost.cloudstream3"
}

dependencies {
    // Testing
    testImplementation(libs.junit)
    testImplementation(libs.json)
    androidTestImplementation(libs.core)
    androidTestImplementation(libs.espresso.core)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.instancio.core)
    androidTestImplementation(libs.junit.ktx)
    androidTestImplementation(libs.kotlin.test)

    // Android Core & Lifecycle
    implementation(libs.core.ktx)
    implementation(libs.activity.ktx)
    implementation(libs.annotation)
    implementation(libs.appcompat)
    implementation(libs.fragment.ktx)
    implementation(libs.bundles.lifecycle)
    implementation(libs.bundles.navigation)
    implementation(libs.kotlinx.collections.immutable)
    implementation(libs.kotlinx.serialization.json) // JSON Parser

    // Design & UI (Disabled for headless AAR)
    implementation(libs.preference.ktx)
    // implementation(libs.material)
    // implementation(libs.constraintlayout)

    // Coil Image Loading
    implementation(libs.bundles.coil)

    // Media 3 (ExoPlayer)
    implementation(libs.bundles.media3)
    implementation(libs.video)

    // FFmpeg Decoding
    implementation(libs.bundles.nextlib)

    // Anime-db for filler
    implementation(libs.anime.db)

    // PlayBack
    // implementation(libs.colorpicker) // Subtitle Color Picker
    implementation(libs.newpipeextractor) // For Trailers
    implementation(libs.juniversalchardet) // Subtitle Decoding

    // UI Stuff (Disabled for headless AAR)
    // implementation(libs.shimmer)
    implementation(libs.palette.ktx) // Palette for Images -> Colors
    implementation(libs.tvprovider)
    // implementation(libs.overlappingpanels)
    implementation(libs.biometric) // Fingerprint Authentication
    // implementation(libs.previewseekbar.media3)
    // implementation(libs.qrcode.kotlin)

    // Extensions & Other Libs
    implementation(libs.jsoup) // HTML Parser
    implementation(libs.ksoup) // HTML Parser
    implementation(libs.rhino) // Run JavaScript
    implementation(libs.safefile) // File management
    coreLibraryDesugaring(libs.desugar.jdk.libs.nio) // NIO Flavor Needed for NewPipeExtractor
    implementation(libs.conscrypt.android) // SSL helper
    implementation(libs.jackson.module.kotlin) // JSON Parser
    implementation(libs.zipline)

    // Temp/deprecated dependencies
    implementation("com.google.code.gson:gson:2.11.0")
    implementation("me.xdrop:fuzzywuzzy:1.4.0")

    // Torrent Support
    implementation(libs.torrentserver)

    // Downloading & Networking
    implementation(libs.work.runtime.ktx)
    implementation(libs.nicehttp) // HTTP Lib

    // Compose (Disabled for headless AAR)
    // implementation(libs.bundles.compose)
    // implementation(libs.activity.compose)
    implementation(libs.kotlinx.io.core) // Logcat parser

    implementation(project(":library"))
    implementation(project(":shared"))
}

tasks.register<Jar>("androidSourcesJar") {
    archiveClassifier.set("sources")
    from(android.sourceSets.getByName("main").java.directories) // Full Sources
}

tasks.withType<KotlinJvmCompile> {
    compilerOptions {
        jvmTarget.set(javaTarget)
        jvmDefault.set(JvmDefaultMode.ENABLE)
        optIn.addAll(
            "com.lagradost.cloudstream3.InternalAPI",
            "com.lagradost.cloudstream3.Prerelease",
        )
    }
}

dokka {
    moduleName = "App"
    dokkaSourceSets {
        configureEach {
            suppress = name != "debug"
            analysisPlatform = KotlinPlatform.JVM
            displayName = "JVM"
            documentedVisibilities(
                VisibilityModifier.Public,
                VisibilityModifier.Protected
            )

            sourceLink {
                localDirectory = file("..")
                remoteUrl("https://github.com/recloudstream/cloudstream/tree/master")
                remoteLineSuffix = "#L"
            }
        }
    }
}
