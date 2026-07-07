import korlibs.korge.gradle.korge

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.korge.library)
}

korge {
    id = "org.korge.e2e.test.shared"

    targetJvm()
    targetJs()
    // TODO Fix gradle plugins to support ios targets easier
    // targetIos()
    targetAndroid()
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(libs.korge)
        }

        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
    }
}
