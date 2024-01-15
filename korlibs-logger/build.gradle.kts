import korlibs.*

description = "Korlibs Logger"

project.extensions.extraProperties.properties.apply {
    applyProjectProperties(
        "https://raw.githubusercontent.com/korlibs/korge/main/korlibs-logger",
        "Public Domain",
        "https://raw.githubusercontent.com/korlibs/korge/main/korlibs-logger/LICENSE"
    )
}

dependencies {
    commonMainApi(libs.kotlinx.coroutines.core)
    commonMainApi(libs.kotlinx.atomicfu)
    commonTestApi(libs.kotlinx.coroutines.test)
    commonMainApi(libs.kotlinx.atomicfu)
    commonTestApi(libs.kotlinx.coroutines.test)
    commonMainApi(project(":korlibs-time"))
    commonMainApi(project(":korlibs-crypto"))
    commonMainApi(project(":korlibs-platform"))
    commonMainApi(project(":korlibs-datastructure"))
}
