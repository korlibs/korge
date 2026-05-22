import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.abi.ExperimentalAbiValidation

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.multiplatform.library)
    alias(libs.plugins.kotlinx.kover)
    alias(libs.plugins.dokka)
    alias(libs.plugins.vanniktech.mavenPublish)
}

description = "Multiplatform Game Engine written in Kotlin"
group = "org.korge.engine"
version = rootProject.libs.versions.korge.get()

kotlin {
    applyDefaultHierarchyTemplate()
    // TODO Consider enabling ABI validation
//    @OptIn(ExperimentalAbiValidation::class)
//    abiValidation {
//        enabled.set(true)
//    }

    jvm()

    android {
        namespace = "org.korge.engine"
        compileSdk = libs.versions.compileSdk.get().toInt()
        minSdk = libs.versions.minSdk.get().toInt()

        androidResources.enable = true
        withHostTest {
            isIncludeAndroidResources = true
        }
        withDeviceTest {}
    }
    js {
        browser {
            compilerOptions {
                target.set("es2015")
            }
        }
    }
    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        browser {
            compilerOptions {
                target.set("es2015")
            }
        }
    }

    iosArm64()
    iosSimulatorArm64()
    iosX64()
    tvosArm64()
    tvosSimulatorArm64()
    watchosArm64()
    watchosArm32()
    watchosDeviceArm64()
    watchosSimulatorArm64()
    macosArm64()
    // TODO Add support for these targets as well
//    linuxX64()
//    linuxArm64()
//    mingwX64()
    // TODO Add android native targets as well

    sourceSets {
        commonMain.dependencies {
            api(projects.korgeCore)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.coroutines.test)
        }
        jvmMain.dependencies {
            api(projects.korgeIpc)
            api(libs.kotlinx.coroutines.debug)
        }
        val androidHostTest by getting {
            dependencies {
                // workaround in android host tests to replace main dispatcher (Android looper)
                // with JVM swing
                implementation(libs.kotlinx.coroutines.swing)
            }
        }

        val iosTvosMain by creating {
            dependsOn(appleMain.get())
        }

        iosMain {
            dependsOn(iosTvosMain)
        }

        tvosMain {
            dependsOn(iosTvosMain)
        }
    }
}

// Workaround in android host tests to exclude android coroutine dispatcher to use swing implementation
configurations.matching {
    it.name.contains("android") && it.name.contains("HostTestRuntimeClasspath")
}.all {
    exclude(group = "org.jetbrains.kotlinx", module = "kotlinx-coroutines-android")
}

val appleTestTargets = listOf(
    "iosSimulatorArm64",
    "iosArm64",
    "iosX64",
    "tvosSimulatorArm64",
    "tvosArm64",
    "tvosX64",
    "macosArm64",
    "macosX64",
    "watchosSimulatorArm64",
    "watchosArm64",
)

// For all apple test targets configure the test task to copy the resources from
// commonTest and appleTest to the according build directory where the executable lives.
// This makes the resources discovrable via the vfs.
appleTestTargets.forEach { target ->
    val testTaskName = "${target}Test"
    val copyTaskName = "copyTestResourcesFor${target.replaceFirstChar { it.uppercaseChar() }}"

    // TODO find a better way, as the test resources are copied multiple times and consume quite some space
    tasks.register<Copy>(copyTaskName) {
        from("src/commonTest/resources")
        from("src/appleTest/resources")
        // Consider enabling this target if needed for iOS test targets only
        // from("src/iosTest/resources")
        into(layout.buildDirectory.dir("bin/$target/debugTest"))
    }

    tasks.matching { it.name == testTaskName }.configureEach {
        dependsOn(copyTaskName)
    }
}
