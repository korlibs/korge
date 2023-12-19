import korlibs.*

description = "Korlibs Crypto - Former Klock"

dependencies {
    commonMainApi(project(":korlibs-memory"))
}

project.extensions.extraProperties.properties.apply {
    applyProjectProperties(
        "https://raw.githubusercontent.com/korlibs/korge/main/korlibs-crypto",
        "Public Domain",
        "https://raw.githubusercontent.com/korlibs/korge/main/korlibs-crypto/LICENSE"
    )
}
