import org.jetbrains.kotlin.gradle.dsl.JvmTarget

fun String.asBuildConfigString(): String = "\"" + replace("\\", "\\\\").replace("\"", "\\\"") + "\""

val postHogApiKey = providers.gradleProperty("POSTHOG_API_KEY")
    .orElse(providers.environmentVariable("POSTHOG_API_KEY"))
    .orElse("")
val postHogHost = providers.gradleProperty("POSTHOG_HOST")
    .orElse(providers.environmentVariable("POSTHOG_HOST"))
    .orElse("https://us.i.posthog.com")

plugins {
    id("com.android.application")
    alias(libs.plugins.compose.compiler)
}

android {
    namespace = "com.nikgapps"
    compileSdk = 37

    signingConfigs {
        create("release") {
            storeFile = file("../my-release-key.keystore")
            storePassword = System.getenv("KEYSTORE_PASSWORD")
            keyAlias = "myKeyAlias"
            keyPassword = System.getenv("KEYSTORE_PASSWORD")
        }

        val devKeystorePath = System.getenv("DEV_KEYSTORE_PATH")
        if (!devKeystorePath.isNullOrBlank()) {
            create("ciDev") {
                storeFile = file(devKeystorePath)
                storePassword = System.getenv("DEV_KEYSTORE_PASSWORD")
                keyAlias = System.getenv("DEV_KEY_ALIAS")
                keyPassword = System.getenv("DEV_KEY_PASSWORD")
            }
        }
    }

    defaultConfig {
        applicationId = "com.nikgapps"
        minSdk = 24
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"
        buildConfigField("String", "POSTHOG_API_KEY", postHogApiKey.get().asBuildConfigString())
        buildConfigField("String", "POSTHOG_HOST", postHogHost.get().asBuildConfigString())
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    buildTypes {
        debug {
            isDebuggable = true
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
            isMinifyEnabled = false
            signingConfig = signingConfigs.findByName("ciDev")
                ?: signingConfigs.getByName("debug")
        }
        release {
            signingConfig = signingConfigs.getByName("release")
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    kotlin {
        compilerOptions {
            jvmTarget = JvmTarget.JVM_21
        }
        jvmToolchain(21)
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    implementation(libs.androidx.datastore)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.compose.bom))
    implementation(libs.androidx.navigation.runtime.ktx)
    implementation(libs.androidx.ui.tooling.preview.android)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.compose.bom))
    implementation(libs.androidx.navigation.compose)
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons.extended)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.tooling.preview.android)
    implementation(libs.androidx.appcompat)
    implementation(libs.coil.compose)
    implementation(libs.libsu)
    implementation(libs.okhttp)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.work.runtime.ktx)
    implementation(libs.android.device.names)
    implementation(libs.posthog.android)
}
