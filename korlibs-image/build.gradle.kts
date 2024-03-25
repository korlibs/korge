import korlibs.*

description = "Korlibs Image"

project.extensions.extraProperties.properties.apply {
    applyProjectProperties(
        "https://raw.githubusercontent.com/korlibs/korge/main/korlibs-image",
        "MIT",
        "https://raw.githubusercontent.com/korlibs/korge/main/korlibs-image/LICENSE"
    )
}

dependencies {
    commonMainApi(project(":korlibs-io"))
    commonMainApi(project(":korlibs-ffi"))
    commonMainApi(project(":korlibs-wasm"))
    //jvmMainImplementation(libs.bundles.jna)
    commonMainApi(libs.kotlinx.atomicfu)
}
