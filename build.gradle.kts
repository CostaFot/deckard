plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.hilt) apply false
    alias(libs.plugins.compose) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.dependencyGuard) apply false
    alias(libs.plugins.spotless)
}

// The modules format themselves through the convention plugins; this covers what no module owns:
// build-logic's own Kotlin and the build scripts at the root.
spotless {
    kotlin {
        target("build-logic/convention/src/**/*.kt")
        ktlint(libs.versions.ktlint.get()).editorConfigOverride(mapOf("android" to "true"))
        endWithNewline()
    }
    format("kts") {
        target("*.kts", "build-logic/*.kts", "build-logic/convention/*.kts")
        endWithNewline()
    }
}
