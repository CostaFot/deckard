package com.costafotiadis.deckard

import com.diffplug.gradle.spotless.SpotlessExtension
import org.gradle.api.Project
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.configure

/**
 * Formats an Android module's Kotlin (ktlint, Android style), build script and XML resources.
 * No licence header: the LICENSE at the root covers the repo.
 */
internal fun Project.configureSpotlessForAndroid() {
    apply(plugin = "com.diffplug.spotless")
    extensions.configure<SpotlessExtension> {
        kotlin {
            target("src/**/*.kt")
            ktlint(libs.findVersion("ktlint").get().requiredVersion).editorConfigOverride(
                mapOf("android" to "true"),
            )
            endWithNewline()
        }
        format("kts") {
            target("*.kts")
            endWithNewline()
        }
        format("xml") {
            target("src/**/*.xml")
            endWithNewline()
        }
    }
}
