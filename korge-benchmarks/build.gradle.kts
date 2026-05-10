plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.multiplatform.library)
    alias(libs.plugins.kotlinx.benchmark)
    alias(libs.plugins.kotlin.allopen)
}

kotlin {
    applyDefaultHierarchyTemplate()
    jvm()
    android {
        namespace = "org.korge.benchmarks"
        compileSdk = libs.versions.compileSdk.get().toInt()
        minSdk = libs.versions.minSdk.get().toInt()

        androidResources.enable = true
        withHostTest {}
        withDeviceTest {}
    }

    sourceSets {
        jvmMain.dependencies {
            api(projects.korge)
            api(libs.kotlinx.benchmark.runtime)
        }
    }
}

allOpen {
    // Configure all-open for benchmark testing
    annotation("org.openjdk.jmh.annotations.State")
}

benchmark {
    targets {
        register("jvm")
    }
}
