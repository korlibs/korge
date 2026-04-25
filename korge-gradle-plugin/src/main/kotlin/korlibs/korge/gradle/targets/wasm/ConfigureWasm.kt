@file:OptIn(
    org.jetbrains.kotlin.gradle.ExperimentalWasmDsl::class,
    org.jetbrains.kotlin.gradle.targets.js.dsl.ExperimentalDistributionDsl::class
)

package korlibs.korge.gradle.targets.wasm

import korlibs.*
import org.gradle.api.*

//fun Project.isWasmEnabled(): Boolean = findProperty("enable.wasm") == "true"
fun isWasmEnabled(project: Project?): Boolean = true
//fun isWasmEnabled(project: Project?): Boolean = false
//fun Project.isWasmEnabled(): Boolean = false

fun Project.configureWasmTarget(executable: Boolean, binaryen: Boolean = false) {
    kotlin {
        wasmJs {
            if (executable) {
                binaries.executable()
            }
            //applyBinaryen()
            browser {
                //commonWebpackConfig { experiments = mutableSetOf("topLevelAwait") }
                if (executable) {
                    this.distribution {
                    }
                }
                //testTask {
                //    it.useKarma {
                //        //useChromeHeadless()
                //        this.webpackConfig.configDirectory = File(rootProject.rootDir, "karma.config.d")
                //    }
                //}
            }

            // Binaryen is enabled by default in recent Kotlin versions.
            // Keeping the flag for API compatibility, but no explicit call is required.
            if (binaryen) {
                // no-op
            }
        }

        sourceSets.maybeCreate("wasmJsTest").apply {
            dependencies {
                implementation("org.jetbrains.kotlin:kotlin-test-wasm-js")
            }
        }
    }
}
