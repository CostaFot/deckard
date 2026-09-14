import java.util.Properties

plugins {
    id("application.common")
    id("application.compose.common")
    id("hilt.common")
    alias(libs.plugins.kotlin.serialization)
}

// Resolve the AI-detector API key from the environment first (CI / GitHub Actions secret), then fall
// back to local.properties (gitignored) for local builds.
val aiDetectorApiKey: String = System.getenv("AI_DETECTOR_API_KEY")?.takeIf { it.isNotBlank() }
    ?: Properties().apply {
        val file = rootProject.file("local.properties")
        if (file.exists()) file.inputStream().use { load(it) }
    }.getProperty("AI_DETECTOR_API_KEY", "")

// A canned verdict for seeing the report card without spending a Pangram call, e.g.
// `./gradlew :app:installDebug -PmockVerdict=assisted`. One of ai / assisted / human / mixed; any
// other value (and every release build) leaves detection real.
val mockVerdict: String = (findProperty("mockVerdict") as String?).orEmpty().ifBlank { "off" }

android {
    defaultConfig {
        applicationId = "com.costafotiadis.deckard"
        versionCode = 1
        versionName = "0.0.1"

        testInstrumentationRunner = "com.costafotiadis.testing.CustomTestRunner"

        vectorDrawables {
            useSupportLibrary = true
        }

        buildConfigField("String", "AI_DETECTOR_API_KEY", "\"$aiDetectorApiKey\"")
        buildConfigField("String", "MOCK_VERDICT", "\"off\"")
    }

    buildFeatures {
        buildConfig = true
        resValues = true
    }

    buildTypes {
        val debug by getting {
            applicationIdSuffix = ".debug"
            resValue("string", "app_name", "Deckard Debug")
            buildConfigField("String", "MOCK_VERDICT", "\"$mockVerdict\"")
        }
        val release by getting {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("debug")
            resValue("string", "app_name", "Deckard")
        }
    }

    packaging {
        resources {
            excludes.add("/META-INF/{AL2.0,LGPL2.1}")
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

    testOptions {
        unitTests {
            isIncludeAndroidResources = true
        }
    }

    namespace = "com.costafotiadis.deckard"
}

dependencyGuard {
    configuration("releaseRuntimeClasspath")
}

dependencies {
    implementation(project(":design"))
    implementation(project(":common"))
    implementation(project(":logging"))
    implementation(project(":textresource"))

    implementation(libs.material.design)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.compose.runtime)
    implementation(libs.androidx.lifecycle.runtimeCompose)
    implementation(libs.androidx.lifecycle.viewModelCompose)
    implementation(libs.androidx.lifecycle.livedata.ktx)
    implementation(libs.androidx.lifecycle.service)
    implementation(libs.androidx.compose.runtime.tracing)
    implementation(libs.androidx.compose.material3.windowSizeClass)
    implementation(libs.androidx.compose.material.iconsExtended)
    implementation(libs.androidx.window.manager)
    implementation(libs.androidx.compose.runtime.livedata)
    implementation(libs.coil.kt.okhttp)
    implementation(libs.coil.kt.compose)
    implementation(libs.coil.kt.svg)
    implementation(libs.coil.kt.gif)
    implementation(libs.androidx.navigation3.runtime)
    implementation(libs.androidx.navigation3.ui)
    implementation(libs.androidx.lifecycle.viewmodel.navigation3)


    // on-device LLM inference
    implementation(libs.litertlm.android)

    // networking
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.okhttp.logging)
    implementation(libs.retrofit.core)
    implementation(libs.retrofit.kotlin.serialization)

    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test)

    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.testManifest)

    // unit testing
    testImplementation(libs.mockk)

    // UI testing
    androidTestImplementation(project(":testing"))
    androidTestImplementation(project(":common-test"))
}
