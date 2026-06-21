plugins {
    alias(libs.plugins.android.application)
}

description = "Multiplatform Game Engine written in Kotlin"
group = "org.korge.sandbox"
version = rootProject.libs.versions.korge.get()

// TODO See how korge is integrated here
android {
    namespace = "org.korge.sandbox.android"
    compileSdk = libs.versions.compileSdk.get().toInt()
}
