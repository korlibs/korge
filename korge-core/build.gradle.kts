import korlibs.applyProjectProperties

description = "Multiplatform Game Engine written in Kotlin"

project.extensions.extraProperties.properties.apply {
    applyProjectProperties(
        "https://github.com/korlibs/korge",
        "MIT License",
        "https://raw.githubusercontent.com/korlibs/korge/master/LICENSE"
    )
}

dependencies {
    commonMainApi(libs.bundles.korlibs.all)
    //commonMainApi(libs.korlibs.audio)
    //commonMainApi(libs.korlibs.image)
    //commonMainApi(libs.korlibs.inject)
    //commonMainApi(libs.korlibs.template)
    //commonMainApi(libs.korlibs.time)
    commonMainApi(libs.kotlinx.atomicfu)
    commonMainApi(libs.kotlinx.coroutines.core)
    //commonTestApi(project(":korge-test"))
    jvmMainApi("org.jetbrains.kotlin:kotlin-reflect")
    jvmMainImplementation(libs.jackson.databind)
    jvmMainImplementation(libs.jackson.module.kotlin)

    //commonTestApi(testFixtures(project(":korma")))

    //add("jvmMainApi", project(":korte"))

    //add("commonTestApi", "it.krzeminski.vis-assert:vis-assert:0.4.0-beta")
}
