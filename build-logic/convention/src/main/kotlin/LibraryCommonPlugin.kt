import com.android.build.api.dsl.LibraryExtension
import com.android.build.api.dsl.Lint
import com.android.build.api.variant.LibraryAndroidComponentsExtension
import com.costafotiadis.deckard.configureDeckardLint
import com.costafotiadis.deckard.configureKotlinAndroid
import com.costafotiadis.deckard.configureSpotlessForAndroid
import com.costafotiadis.deckard.disableUnnecessaryAndroidTests
import com.costafotiadis.deckard.libs
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.kotlin

class LibraryCommonPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            apply(plugin = "com.android.library")

            extensions.configure<LibraryExtension> {
                configureKotlinAndroid(this)
                testOptions.animationsDisabled = true
                // The resource prefix is derived from the module name,
                // so resources inside ":core:module1" must be prefixed with "core_module1_"
                resourcePrefix =
                    path.split("""\W""".toRegex()).drop(1).distinct().joinToString(separator = "_")
                        .lowercase() + "_"
                lint(Lint::configureDeckardLint)
            }
            extensions.configure<LibraryAndroidComponentsExtension> {
                disableUnnecessaryAndroidTests(target)
            }
            configureSpotlessForAndroid()

            dependencies {
                add("androidTestImplementation", kotlin("test"))
                add("testImplementation", kotlin("test"))
                add("testImplementation", libs.findLibrary("junit4").get())
            }
        }
    }
}
