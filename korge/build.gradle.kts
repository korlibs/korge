description = "Multiplatform Game Engine written in Kotlin"

//project.extensions.extraProperties.properties.apply {
//    applyProjectProperties(
//        "https://github.com/korlibs/klogger",
//        "MIT License",
//        "https://raw.githubusercontent.com/korlibs/korge/master/LICENSE"
//    )
//}

dependencies {
    add("commonMainApi", project(":korge-core"))
    add("commonMainApi", project(":korge-foundation"))
    //commonTestApi(project(":korge-test"))
    add("jvmMainApi", "org.jetbrains.kotlin:kotlin-reflect")
    add("jvmMainApi", libs.jackson.databind)
    add("jvmMainApi", libs.jackson.module.kotlin)

    //commonTestApi(testFixtures(project(":korma")))

    //add("jvmMainApi", project(":korte"))

    //add("commonTestApi", "it.krzeminski.vis-assert:vis-assert:0.4.0-beta")
}
