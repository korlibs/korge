import korlibs.*

description = "Korlibs Memory"

project.extensions.extraProperties.properties.apply {
    applyProjectProperties(
        "https://raw.githubusercontent.com/korlibs/korge/main/korlibs-memory",
        "Public Domain",
        "https://raw.githubusercontent.com/korlibs/korge/main/korlibs-memory/LICENSE"
    )
}

dependencies {
    commonMainApi(project(":korlibs-math-core"))
    commonMainApi(project(":korlibs-platform"))
    commonTestImplementation(project(":korlibs-util"))
    commonTestImplementation(project(":korlibs-crypto"))
}
