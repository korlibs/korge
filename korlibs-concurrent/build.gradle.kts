import korlibs.*

description = "Korlibs Concurrent"

project.extensions.extraProperties.properties.apply {
    applyProjectProperties(
        "https://raw.githubusercontent.com/korlibs/korge/main/korlibs-concurrent",
        "Public Domain",
        "https://raw.githubusercontent.com/korlibs/korge/main/korlibs-concurrent/LICENSE"
    )
}

dependencies {
    commonMainApi(libs.kotlinx.atomicfu)
}
