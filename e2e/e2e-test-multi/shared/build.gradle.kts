plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.korge.library)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(libs.korge.engine)
            implementation(libs.korlibs.image)
        }
    }
}

korge {
    targetJvm()
    //targetJs()
}
