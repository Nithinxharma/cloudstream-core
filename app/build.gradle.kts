import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import com.android.build.gradle.internal.cxx.configure.gradleLocalProperties
import org.jetbrains.dokka.gradle.engine.parameters.KotlinPlatform
import org.jetbrains.dokka.gradle.engine.parameters.VisibilityModifier
import org.jetbrains.kotlin.gradle.dsl.JvmDefaultMode
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile

plugins {
    alias(libs.plugins.android.library)
    id("maven-publish")
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
        consumerProguardFiles("proguard-rules.pro")

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

    publishing {
        singleVariant("release") {
            withSourcesJar()
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
    api(libs.nicehttp) // HTTP Lib

    // Compose (Disabled for headless AAR)
    // implementation(libs.bundles.compose)
    // implementation(libs.activity.compose)
    implementation(libs.kotlinx.io.core) // Logcat parser

    api(project(":library"))
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



tasks.register("bundleSingleAar") {
    group = "build"
    description = "Bundles :app, :library, and :shared into a single unified cs3core.aar"
    dependsOn(":app:assembleDebug", ":library:assemble", ":shared:assemble")

    val appAarFile = layout.buildDirectory.file("outputs/aar/app-debug.aar").get().asFile
    val libAarDefault = rootProject.project(":library").layout.buildDirectory.file("outputs/aar/library.aar").get().asFile
    val libAarDebug = rootProject.project(":library").layout.buildDirectory.file("outputs/aar/library-debug.aar").get().asFile
    val sharedAarDefault = rootProject.project(":shared").layout.buildDirectory.file("outputs/aar/shared.aar").get().asFile
    val sharedAarDebug = rootProject.project(":shared").layout.buildDirectory.file("outputs/aar/shared-debug.aar").get().asFile
    val outputAarFile = layout.buildDirectory.file("outputs/aar/cs3core.aar").get().asFile
    val buildDirectory = layout.buildDirectory.get().asFile

    inputs.files(appAarFile, libAarDefault, libAarDebug, sharedAarDefault, sharedAarDebug)
    outputs.file(outputAarFile)

    doLast {
        val resolvedLibAar = if (libAarDefault.exists()) libAarDefault else libAarDebug
        val resolvedSharedAar = if (sharedAarDefault.exists()) sharedAarDefault else sharedAarDebug

        if (!appAarFile.exists()) error("app-debug.aar does not exist at ${appAarFile.absolutePath}")
        if (!resolvedLibAar.exists()) error("library.aar does not exist at ${resolvedLibAar.absolutePath}")
        if (!resolvedSharedAar.exists()) error("shared.aar does not exist at ${resolvedSharedAar.absolutePath}")

        outputAarFile.parentFile.mkdirs()
        val tempDir = File(buildDirectory, "tmp/bundleSingleAar").apply {
            deleteRecursively()
            mkdirs()
        }

        fun unzipArchive(zipFile: File, destDir: File) {
            ZipInputStream(FileInputStream(zipFile)).use { zis ->
                var entry = zis.nextEntry
                while (entry != null) {
                    val newFile = File(destDir, entry.name)
                    if (entry.isDirectory) {
                        newFile.mkdirs()
                    } else {
                        newFile.parentFile.mkdirs()
                        FileOutputStream(newFile).use { fos -> zis.copyTo(fos) }
                    }
                    entry = zis.nextEntry
                }
            }
        }

        fun createZipArchive(sourceDirectory: File, targetZipFile: File) {
            ZipOutputStream(FileOutputStream(targetZipFile)).use { zos ->
                sourceDirectory.walkTopDown().forEach { file ->
                    val relativePath = file.relativeTo(sourceDirectory).path.replace('\\', '/')
                    if (file.isDirectory) {
                        if (relativePath.isNotEmpty()) {
                            zos.putNextEntry(ZipEntry("$relativePath/"))
                            zos.closeEntry()
                        }
                    } else {
                        zos.putNextEntry(ZipEntry(relativePath))
                        FileInputStream(file).use { input -> input.copyTo(zos) }
                        zos.closeEntry()
                    }
                }
            }
        }

        try {
            val appExtractDir = File(tempDir, "app_aar").apply { mkdirs() }
            val libExtractDir = File(tempDir, "lib_aar").apply { mkdirs() }
            val sharedExtractDir = File(tempDir, "shared_aar").apply { mkdirs() }
            val mergedClassesDir = File(tempDir, "merged_classes").apply { mkdirs() }
            val finalClassesJar = File(appExtractDir, "classes.jar")

            // Unzip AAR archives
            unzipArchive(appAarFile, appExtractDir)
            unzipArchive(resolvedLibAar, libExtractDir)
            unzipArchive(resolvedSharedAar, sharedExtractDir)

            // Extract classes.jar from all 3 modules into mergedClassesDir
            val jarFiles = listOf(
                File(appExtractDir, "classes.jar"),
                File(libExtractDir, "classes.jar"),
                File(sharedExtractDir, "classes.jar")
            )
            for (jar in jarFiles) {
                if (jar.exists()) {
                    unzipArchive(jar, mergedClassesDir)
                }
            }

            // 1. Pack merged classes directory into final classes.jar
            if (finalClassesJar.exists()) {
                finalClassesJar.delete()
            }
            createZipArchive(mergedClassesDir, finalClassesJar)

            // 2. Pack the updated appExtractDir into cs3core.aar
            if (outputAarFile.exists()) {
                outputAarFile.delete()
            }
            createZipArchive(appExtractDir, outputAarFile)

            println("Successfully created single merged AAR: ${outputAarFile.absolutePath} (${outputAarFile.length()} bytes)")
        } finally {
            tempDir.deleteRecursively()
        }
    }
}

afterEvaluate {
    publishing {
        publications {
            create<MavenPublication>("release") {
                from(components["release"])
                groupId = "com.lagradost.cloudstream3"
                artifactId = "cs3core"
                version = libs.versions.versionName.get()
            }
        }
        repositories {
            maven {
                name = "ProjectLocal"
                url = uri(rootProject.layout.buildDirectory.dir("repo"))
            }
        }
    }
}

