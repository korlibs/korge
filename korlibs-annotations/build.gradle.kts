import korlibs.*

description = "Korlibs Annotations"

project.extensions.extraProperties.properties.apply {
    applyProjectProperties(
        "https://raw.githubusercontent.com/korlibs/korge/main/korlibs-annotations",
        "Public Domain",
        "https://raw.githubusercontent.com/korlibs/korge/main/korlibs-annotations/LICENSE"
    )
}

dependencies {
}
