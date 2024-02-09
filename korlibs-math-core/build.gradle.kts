import korlibs.*

description = "Korlibs Math Core"

project.extensions.extraProperties.properties.apply {
    applyProjectProperties(
        "https://raw.githubusercontent.com/korlibs/korge/main/korlibs-math-core",
        "Public Domain",
        "https://raw.githubusercontent.com/korlibs/korge/main/korlibs-math-core/LICENSE"
    )
}

dependencies {
    commonMainApi(libs.kotlinx.coroutines.core)
    commonMainApi(libs.kotlinx.atomicfu)
    commonTestApi(libs.kotlinx.coroutines.test)
    commonMainApi(libs.kotlinx.atomicfu)
    commonTestApi(libs.kotlinx.coroutines.test)
}
