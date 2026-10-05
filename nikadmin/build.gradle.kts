import org.jetbrains.kotlin.gradle.dsl.JvmTarget

fun String.asBuildConfigString(): String = "\"" + replace("\\", "\\\\").replace("\"", "\\\"") + "\""

val postHogPersonalApiKey = providers.gradleProperty("POSTHOG_PERSONAL_API_KEY")
    .orElse(providers.environmentVariable("POSTHOG_PERSONAL_API_KEY"))
    .map(String::trim).orElse("")
val postHogProjectId = providers.gradleProperty("POSTHOG_PROJECT_ID")
    .orElse(providers.environmentVariable("POSTHOG_PROJECT_ID"))
    .map(String::trim).orElse("")
val postHogApiHost = providers.gradleProperty("POSTHOG_API_HOST")
    .orElse(providers.environmentVariable("POSTHOG_API_HOST"))
    .map { it.trim().trimEnd('/').ifBlank { "https://us.posthog.com" } }
    .orElse("https://us.posthog.com")

plugins {
    id("com.android.application")
    alias(libs.plugins.compose.compiler)
}

android {
    namespace = "com.nikgapps.admin"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.nikgapps.admin"
        minSdk = 24
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"
        buildConfigField("String", "POSTHOG_PERSONAL_API_KEY", "".asBuildConfigString())
        buildConfigField("String", "POSTHOG_PROJECT_ID", "".asBuildConfigString())
        buildConfigField("String", "POSTHOG_API_HOST", postHogApiHost.get().asBuildConfigString())
    }

    buildTypes {
        debug {
            buildConfigField("String", "POSTHOG_PERSONAL_API_KEY", postHogPersonalApiKey.get().asBuildConfigString())
            buildConfigField("String", "POSTHOG_PROJECT_ID", postHogProjectId.get().asBuildConfigString())
        }
        release {
            // A personal API key must never be embedded in a distributable APK.
            buildConfigField("String", "POSTHOG_PERSONAL_API_KEY", "".asBuildConfigString())
            buildConfigField("String", "POSTHOG_PROJECT_ID", "".asBuildConfigString())
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
    kotlin {
        compilerOptions { jvmTarget = JvmTarget.JVM_21 }
        jvmToolchain(21)
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons.extended)
    implementation(libs.okhttp)
    implementation(libs.kotlinx.serialization.json)
    testImplementation(libs.junit)
}
