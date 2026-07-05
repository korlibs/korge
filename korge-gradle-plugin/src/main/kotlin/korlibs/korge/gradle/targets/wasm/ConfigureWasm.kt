@file:OptIn(
    org.jetbrains.kotlin.gradle.ExperimentalWasmDsl::class,
    org.jetbrains.kotlin.gradle.targets.js.dsl.ExperimentalDistributionDsl::class
)

package korlibs.korge.gradle.targets.wasm

import korlibs.kotlin
import org.gradle.api.Project

fun isWasmEnabled(project: Project?): Boolean = true

fun Project.configureWasmTarget(executable: Boolean, binaryen: Boolean = false) {
    kotlin {
        wasmJs {
            if (executable) {
                binaries.executable()
            }
            browser {
                if (executable) {
                    this.distribution {
                    }
                }
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
