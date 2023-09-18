import korlibs.korge.gradle.targets.all.AddFreeCompilerArgs

plugins {
    id("org.jetbrains.kotlin.multiplatform")
    id("org.jetbrains.kotlinx.benchmark") version libs.versions.kotlinx.benchmark
    id("org.jetbrains.kotlin.plugin.allopen") version libs.versions.kotlin
}

allOpen {
    annotation("org.openjdk.jmh.annotations.State")
}

kotlin {
    jvm {
        AddFreeCompilerArgs.addFreeCompilerArgs(project, this)
    }
}
benchmark.targets.register("jvm")
//kotlin { js(IR) { nodejs() } }; benchmark.targets.register("js")

dependencies {
    add("commonMainApi", project(":korge"))
    add("commonMainApi", "org.jetbrains.kotlinx:kotlinx-benchmark-runtime:0.4.7")
}
