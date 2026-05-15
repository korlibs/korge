plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlinx.benchmark)
    alias(libs.plugins.kotlin.allopen)
}

kotlin {
    applyDefaultHierarchyTemplate()
    jvm()

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
