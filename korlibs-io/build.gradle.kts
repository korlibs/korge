import korlibs.*

description = "Korlibs I/O"

project.extensions.extraProperties.properties.apply {
    includeKotlinNativeDesktop()

    applyProjectProperties(
        "https://raw.githubusercontent.com/korlibs/korge/main/korlibs-io",
        "MIT",
        "https://raw.githubusercontent.com/korlibs/korge/main/korlibs-io/LICENSE"
    )
}

dependencies {
    commonMainApi(libs.kotlinx.coroutines.core)
    commonMainApi(project(":korge-foundation")) // @TODO: We should get rid of it eventually and depend on specific
    commonMainApi(project(":korlibs-math-core"))
    commonMainApi(project(":korlibs-memory"))
    commonMainApi(project(":korlibs-util"))
    commonMainApi(project(":korlibs-crypto"))
    //commonMainApi(project(":korlibs-encoding"))
    commonMainApi(project(":korlibs-platform"))
    commonMainApi(project(":korlibs-datastructure"))
    commonMainApi(project(":korlibs-time"))
    commonMainApi(project(":korlibs-logger"))
    //commonMainApi(project(":korlibs-serialization-json"))
    //commonTestImplementation(project(":korlibs-util"))
    //commonTestImplementation(project(":korlibs-crypto"))
}
