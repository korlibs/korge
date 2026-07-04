import org.jetbrains.kotlin.gradle.plugin.mpp.apple.XCFramework

plugins {
    alias(libs.plugins.kotlin.multiplatform)
}

description = "Korge Application – iOS entry point (framework)"
group = "org.korge.application"
version = rootProject.libs.versions.korge.get()

kotlin {
    val frameworkName = "iosApp"

    val iosArm64 = iosArm64()
    val iosSimulatorArm64 = iosSimulatorArm64()
    val iosX64 = iosX64()
    val xcf = XCFramework()

    configure(listOf(iosArm64, iosSimulatorArm64, iosX64)) {
        binaries.framework {
            baseName = frameworkName
            isStatic = true
            export(projects.korgeSandbox.shared)
            xcf.add(this)
        }
    }

    sourceSets {
        commonMain.dependencies {
            implementation(projects.korgeSandbox.shared)
        }
    }
}
