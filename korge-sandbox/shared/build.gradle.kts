plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.multiplatform.library)
    id("org.korge.engine")
}

description = "Multiplatform Game Engine written in Kotlin"
group = "org.korge.sandbox"
version = rootProject.libs.versions.korge.get()

// TODO Configure korge
//korge {
//    entrypoint("Sandbox", "JvmMain")
//}

kotlin {
    jvm()
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

// TODO Configure sandbox project correctly

//tasks.register("runJvmAwtSandbox", KorgeJavaExec) {
//    it.group = "run"
//    it.dependsOn("jvmMainClasses")
//    it.mainClass.set("AwtSandboxSample")
//}
