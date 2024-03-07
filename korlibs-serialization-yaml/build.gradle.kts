import korlibs.*

description = "Korlibs Yaml Serialization Library"

project.extensions.extraProperties.properties.apply {
    applyProjectProperties(
        "https://raw.githubusercontent.com/korlibs/korge/main/korlibs-serialization-yaml",
        "Public Domain",
        "https://raw.githubusercontent.com/korlibs/korge/main/korlibs-serialization-yaml/LICENSE"
    )
}

dependencies {
}
