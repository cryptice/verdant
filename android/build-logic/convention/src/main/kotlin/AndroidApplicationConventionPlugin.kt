import com.android.build.api.dsl.ApplicationExtension
import org.gradle.api.JavaVersion
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinAndroidProjectExtension

/**
 * Centralizes Android application defaults — compileSdk, minSdk, targetSdk,
 * Java / Kotlin compile options — that were previously duplicated inline in
 * `app/build.gradle.kts`.
 *
 * Apply with `plugins { id("verdant.android.application") }` in the app
 * module. Lives here so that when the project grows beyond the single
 * `:app` module, every Android module picks up the same baseline.
 */
class AndroidApplicationConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            // AGP 9 compiles Kotlin itself, so org.jetbrains.kotlin.android is
            // no longer applied.
            pluginManager.apply("com.android.application")

            extensions.configure<ApplicationExtension> {
                compileSdk = 35
                defaultConfig {
                    minSdk = 28
                    targetSdk = 35
                }
                compileOptions {
                    sourceCompatibility = JavaVersion.VERSION_17
                    targetCompatibility = JavaVersion.VERSION_17
                }
            }

            extensions.configure<KotlinAndroidProjectExtension> {
                compilerOptions {
                    jvmTarget.set(JvmTarget.JVM_17)
                }
            }
        }
    }
}
