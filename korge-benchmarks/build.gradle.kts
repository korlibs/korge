plugins {
    kotlin("multiplatform")
    id("org.jetbrains.kotlinx.benchmark") version "0.4.4"
    id("org.jetbrains.kotlin.plugin.allopen") version "1.6.0"
}

allOpen {
    annotation("org.openjdk.jmh.annotations.State")
}

benchmark {
    targets {
        register("jvm")
    }
}

dependencies {
//    add("commonMainApi", project(":kds"))
    add("commonMainApi", project(":korim"))
    add("commonMainApi", project(":korge"))
    add("commonMainApi", "org.jetbrains.kotlinx:kotlinx-benchmark-runtime:0.4.4")

//    implementation("org.jetbrains.kotlinx:kotlinx-benchmark-runtime:0.4.4")
}
