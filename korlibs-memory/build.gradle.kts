import korlibs.*

description = "Korlibs Memory"

project.extensions.extraProperties.properties.apply {
    applyProjectProperties(
        "https://raw.githubusercontent.com/korlibs/korge/main/korlibs-memory",
        "Public Domain",
        "https://raw.githubusercontent.com/korlibs/korge/main/korlibs-memory/LICENSE"
    )
}

dependencies {
    commonMainApi(libs.kotlinx.coroutines.core)
    commonMainApi(libs.kotlinx.atomicfu)
    commonTestApi(libs.kotlinx.coroutines.test)
    commonMainApi(libs.kotlinx.atomicfu)
    commonTestApi(libs.kotlinx.coroutines.test)
    commonMainApi(project(":korlibs-math-core"))
    commonMainApi(project(":korlibs-platform"))
    commonMainApi(project(":korlibs-util"))
    commonMainApi(project(":korlibs-crypto"))
}
