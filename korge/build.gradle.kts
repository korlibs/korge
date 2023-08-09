import korlibs.applyProjectProperties

description = "Multiplatform Game Engine written in Kotlin"

project.extensions.extraProperties.properties.apply {
    applyProjectProperties(
        "https://github.com/korlibs/klogger",
        "MIT License",
        "https://raw.githubusercontent.com/korlibs/korge/master/LICENSE"
    )
}

dependencies {
    add("commonMainApi", project(":korim"))
    add("commonMainApi", project(":korgw"))
    add("commonMainApi", project(":korau"))
    add("commonMainApi", project(":korinject"))
    commonMainApi(project(":korte"))
    jvmMainApi("org.jetbrains.kotlin:kotlin-reflect")
    jvmMainApi(libs.jackson.databind)
    jvmMainApi(libs.jackson.module.kotlin)

    //commonTestApi(testFixtures(project(":korma")))

    //add("jvmMainApi", project(":korte"))

    //add("commonTestApi", "it.krzeminski.vis-assert:vis-assert:0.4.0-beta")
}
