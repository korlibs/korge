kotlin {
    jvm {
        jvm {
            //val main by compilations.getting {
            compilations.all {
                kotlinOptions {
                    // Setup the Kotlin compiler options for the 'main' compilation:
                    //jvmTarget = "1.8"
                    freeCompilerArgs = listOf("-Xjvm-default=enable")
                }
            }
        }
        withJava()
    }
}

dependencies {
    add("commonMainApi", project(":korgw"))
}
