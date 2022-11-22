plugins {
    kotlin("multiplatform")
}

dependencies {
    add("commonMainApi", project(":backend:foundation"))
}
