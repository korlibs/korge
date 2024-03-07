import korlibs.*

description = "Korlibs TOML Serialization Library"

project.extensions.extraProperties.properties.apply {
    applyProjectProperties(
        "https://raw.githubusercontent.com/korlibs/korge/main/korlibs-serialization-toml",
        "Public Domain",
        "https://raw.githubusercontent.com/korlibs/korge/main/korlibs-serialization-toml/LICENSE"
    )
}

dependencies {
}
