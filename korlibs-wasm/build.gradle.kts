import korlibs.*

description = "Korlibs WASM"

project.extensions.extraProperties.properties.apply {
    includeKotlinNativeDesktop()

    applyProjectProperties(
        "https://raw.githubusercontent.com/korlibs/korge/main/korlibs-wasm",
        "",
        "https://raw.githubusercontent.com/korlibs/korge/main/korlibs-wasm/LICENSE"
    )
}

dependencies {
    commonMainApi(project(":korlibs-io"))
    //commonMainApi(project(":korlibs-ffi")) // @TODO: We should remove this dependency at some point
    jvmMainImplementation(libs.asm.core)
    jvmMainImplementation(libs.asm.util)
}
