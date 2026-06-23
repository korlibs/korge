plugins {
    alias(libs.plugins.kotlin.multiplatform)
}

description = "Korge Application – iOS entry point (framework)"
group = "org.korge.application"
version = rootProject.libs.versions.korge.get()

kotlin {
    val frameworkName = "iosApp"
    val xcfConfigure: org.jetbrains.kotlin.gradle.plugin.mpp.Framework.() -> Unit = {
        baseName = frameworkName
        isStatic = true
        export(projects.korgeApplication.shared)
    }

    iosArm64 { binaries.framework(xcfConfigure) }
    iosSimulatorArm64 { binaries.framework(xcfConfigure) }
    iosX64 { binaries.framework(xcfConfigure) }

    sourceSets {
        commonMain.dependencies {
            implementation(project(":korge-sandbox:shared"))
        }
    }
}
