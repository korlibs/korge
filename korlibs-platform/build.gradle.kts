import korlibs.*

description = "Korlibs Platform Library"

project.extensions.extraProperties.properties.apply {
    applyProjectProperties(
        "https://raw.githubusercontent.com/korlibs/korge/main/korlibs-platform",
        "Public Domain",
        "https://raw.githubusercontent.com/korlibs/korge/main/korlibs-platform/LICENSE"
    )
}

dependencies {
}
