import com.android.build.api.dsl.ApplicationExtension
import com.android.build.api.dsl.Lint
import com.android.build.api.variant.ApplicationAndroidComponentsExtension
import com.costafotiadis.deckard.configureBadgingTasks
import com.costafotiadis.deckard.configureDeckardLint
import com.costafotiadis.deckard.configureKotlinAndroid
import com.costafotiadis.deckard.configureSpotlessForAndroid
import com.costafotiadis.deckard.libs
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.kotlin

class ApplicationCommonPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            apply(plugin = "com.android.application")
            apply(plugin = "com.dropbox.dependency-guard")

            extensions.configure<ApplicationExtension> {
                configureKotlinAndroid(this)
                defaultConfig.targetSdk = 35
                testOptions.animationsDisabled = true
                lint(Lint::configureDeckardLint)
            }
            extensions.configure<ApplicationAndroidComponentsExtension> {
                configureBadgingTasks(this)
            }
            configureSpotlessForAndroid()

            dependencies {
                add("androidTestImplementation", kotlin("test"))
                add("testImplementation", kotlin("test"))
                add("testImplementation", libs.findLibrary("junit4").get())
                add("testImplementation", libs.findLibrary("kotlinx-coroutines-test").get())
            }
        }
    }
}
