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
    commonMainApi(libs.kotlinx.atomicfu)
}
