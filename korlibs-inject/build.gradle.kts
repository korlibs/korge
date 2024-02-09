import korlibs.*

description = "Korlibs Inject"

project.extensions.extraProperties.properties.apply {
    applyProjectProperties(
        "https://raw.githubusercontent.com/korlibs/korge/main/korlibs-inject",
        "Public Domain",
        "https://raw.githubusercontent.com/korlibs/korge/main/korlibs-inject/LICENSE"
    )
}

dependencies {
    commonMainApi(libs.kotlinx.coroutines.core)
    commonMainApi(libs.kotlinx.atomicfu)
    commonTestApi(libs.kotlinx.coroutines.test)
    commonMainApi(libs.kotlinx.atomicfu)
    commonTestApi(libs.kotlinx.coroutines.test)
}
