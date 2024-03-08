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
    commonMainApi(project(":korlibs-annotations"))
    commonMainApi(project(":korlibs-io-core"))
    commonMainApi(project(":korlibs-memory"))
    commonMainApi(libs.kotlinx.atomicfu)
}
