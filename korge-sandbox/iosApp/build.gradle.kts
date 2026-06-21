plugins {
    alias(libs.plugins.kotlin.multiplatform)
}

description = "Korge Sandbox – iOS entry point (framework)"
group = "org.korge.sandbox"
version = rootProject.libs.versions.korge.get()

kotlin {
    val frameworkName = "SandboxApp"
    val xcfConfigure: org.jetbrains.kotlin.gradle.plugin.mpp.Framework.() -> Unit = {
        baseName = frameworkName
        isStatic = true
        export(projects.korgeSandbox.shared)
    }

    iosArm64 { binaries.framework(xcfConfigure) }
    iosSimulatorArm64 { binaries.framework(xcfConfigure) }
    iosX64 { binaries.framework(xcfConfigure) }

    sourceSets {
        commonMain.dependencies {
            api(projects.korgeSandbox.shared)
        }
    }
}
