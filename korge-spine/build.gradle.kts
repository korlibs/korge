kotlin {
    jvm {
        withJava()
    }
}

dependencies {
    add("commonMainApi", project(":korgw"))
}
