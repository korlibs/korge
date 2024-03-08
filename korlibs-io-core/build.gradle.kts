import korlibs.*

description = "Korlibs I/O Core"

project.extensions.extraProperties.properties.apply {
    applyProjectProperties(
        "https://raw.githubusercontent.com/korlibs/korge/main/korlibs-io-core",
        "Public Domain",
        "https://raw.githubusercontent.com/korlibs/korge/main/korlibs-io-core/LICENSE"
    )
}

dependencies {
    commonMainApi(project(":korlibs-util"))
}
