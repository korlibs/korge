plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.korge.library)
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(projects.shared)
            implementation(libs.korge.engine)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.korlibs.image)
        }
    }
}

korge {
    id = "com.sample.clientserver"
    targetJvm()
    //targetJs()
}
