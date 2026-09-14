import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    `kotlin-dsl`
    alias(libs.plugins.android.lint)
}

group = "com.costafotiadis.deckard.buildlogic"

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_17
    }
}

dependencies {
    compileOnly(libs.android.gradlePlugin)
    compileOnly(libs.kotlin.gradlePlugin)
    compileOnly(libs.compose.gradlePlugin)
    compileOnly(libs.ksp.gradlePlugin)
    compileOnly(libs.spotless.gradlePlugin)
    implementation(libs.truth)
    lintChecks(libs.androidx.lint.gradle)
}

tasks {
    validatePlugins {
        enableStricterValidation = true
        failOnWarning = true
    }
}

gradlePlugin {
    plugins {
        register("applicationComposeCommonPlugin") {
            id = "application.compose.common"
            implementationClass = "ApplicationComposeCommonPlugin"
        }
        register("applicationCommon") {
            id = "application.common"
            implementationClass = "ApplicationCommonPlugin"
        }
        register("libraryComposeCommon") {
            id = "library.compose.common"
            implementationClass = "LibraryComposeCommonPlugin"
        }
        register("libraryCommon") {
            id = "library.common"
            implementationClass = "LibraryCommonPlugin"
        }
        register("hiltCommon") {
            id = "hilt.common"
            implementationClass = "HiltCommonPlugin"
        }
    }
}
