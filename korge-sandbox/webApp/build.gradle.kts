import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl

plugins {
    alias(libs.plugins.kotlin.multiplatform)
}

description = "Korge Application – Web (JS/WASM) entry point"
group = "org.korge.application"
version = rootProject.libs.versions.korge.get()

kotlin {
    js {
        browser {
            compilerOptions {
                target.set("es2015")
            }
        }
        binaries.executable()
    }
    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        browser {
            compilerOptions {
                target.set("es2015")
            }
        }
        binaries.executable()
    }

    sourceSets {
        commonMain.dependencies {
            implementation(project(":korge-sandbox:shared"))
        }
    }
}
