import korlibs.*

description = "Korlibs Template - Former Korte"

project.extensions.extraProperties.properties.apply {
    applyProjectProperties(
        "https://raw.githubusercontent.com/korlibs/korge/main/korlibs-template",
        "Public Domain",
        "https://raw.githubusercontent.com/korlibs/korge/main/korlibs-template/LICENSE"
    )
}

dependencies {
    commonMainApi(project(":korlibs-serialization-yaml"))
    commonMainApi(project(":korlibs-util"))
    commonMainApi(libs.kotlinx.atomicfu)
    commonTestApi(libs.kotlinx.coroutines.test)
}
