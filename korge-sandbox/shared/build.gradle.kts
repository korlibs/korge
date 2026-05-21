@file:OptIn(ExperimentalKotlinGradlePluginApi::class)

import korlibs.korge.gradle.targets.jvm.KorgeJavaExec
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.multiplatform.library)
    id("org.korge.engine")
}

description = "Multiplatform Game Engine written in Kotlin"
group = "org.korge.sandbox"
version = rootProject.libs.versions.korge.get()

korge {
    // Registers a new task runJvm[name] (here "runJvmSandbox")
    entrypoint(name = "Sandbox", jvmMainClassName =  "JvmMain")
}

kotlin {
    jvm {
        // Configure jvmRun task to use JvmMain as main class
        mainRun {
            mainClass.set("JvmMain")
        }
    }
    android {
        namespace = "org.korge.sandbox.shared"
        compileSdk = libs.versions.compileSdk.get().toInt()
        minSdk = libs.versions.minSdk.get().toInt()

        androidResources.enable = true
        withHostTest {}
        withDeviceTest {}
    }

    sourceSets {
        commonMain.dependencies {
            implementation(projects.korge)
        }
    }
}

tasks.register<KorgeJavaExec>("runJvmAwtSandbox") {
    group = "run"
    description = "AWT entrypoint for JVM targets"
    mainClass.set("AwtSandboxSample")
    dependsOn("jvmMainClasses")
}

tasks.withType<JavaExec> {
    // Configure required arguments for jvmRun task
    jvmArgs(
        "--add-opens=java.desktop/sun.java2d.opengl=ALL-UNNAMED",
        "--add-exports=java.desktop/com.apple.eawt.event=ALL-UNNAMED",
    )
}
