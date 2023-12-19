import korlibs.*

description = "Korlibs Memory - Former Klock"

dependencies {
    commonMainApi(project(":korlibs-math"))
}

project.extensions.extraProperties.properties.apply {
    applyProjectProperties(
        "https://raw.githubusercontent.com/korlibs/korge/main/korlibs-crypto",
        "Public Domain",
        "https://raw.githubusercontent.com/korlibs/korge/main/korlibs-crypto/LICENSE"
    )
}
