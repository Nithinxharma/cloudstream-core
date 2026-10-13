plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.serialization)
    base
}

base {
    archivesName.set("cs3core-test")
}

android {
    namespace = "com.lagradost.cloudstream3.testapp"
    compileSdk = libs.versions.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "com.lagradost.cloudstream3.testapp"
        minSdk = libs.versions.minSdk.get().toInt()
        targetSdk = libs.versions.targetSdk.get().toInt()
        versionCode = 1
        versionName = "1.0"
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
            isMinifyEnabled = false
        }
    }

    compileOptions {
        isCoreLibraryDesugaringEnabled = true
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlin {
        jvmToolchain(libs.versions.jdkToolchain.get().toInt())
    }

    buildFeatures {
        viewBinding = false
    }
}

dependencies {
    // Single Unified AAR (builtBy ensures :app:bundleSingleAar runs before testApp tasks)
    implementation(files("../app/build/outputs/aar/cs3core.aar") {
        builtBy(":app:bundleSingleAar")
    })

    // Standard Runtime & Transitive Dependencies
    implementation(libs.nicehttp)
    implementation(libs.jsoup)
    implementation(libs.ksoup)
    implementation(libs.rhino)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.core.ktx)
    implementation(libs.appcompat)
    implementation(libs.bundles.lifecycle)
    implementation(libs.material)

    // Desugaring for Java 8+ APIs
    coreLibraryDesugaring(libs.desugar.jdk.libs.nio)
}
