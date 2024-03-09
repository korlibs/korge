import korlibs.*

description = "Korlibs FFI"

project.extensions.extraProperties.properties.apply {
    applyProjectProperties(
        "https://raw.githubusercontent.com/korlibs/korge/main/korlibs-ffi",
        "Public Domain",
        "https://raw.githubusercontent.com/korlibs/korge/main/korlibs-ffi/LICENSE"
    )
}

dependencies {
    commonMainApi(project(":korlibs-util"))
    commonMainApi(project(":korlibs-memory"))
    commonMainApi(project(":korlibs-annotations"))
    commonMainApi(project(":korlibs-datastructure")) // @TODO: We should remove this at some point
    jvmMainImplementation(libs.bundles.jna)
    commonMainApi(libs.kotlinx.coroutines.core)
    commonMainApi(libs.kotlinx.atomicfu)
    commonTestApi(libs.kotlinx.coroutines.test)
}
