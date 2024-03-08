import korlibs.*

description = "Korlibs Number"

project.extensions.extraProperties.properties.apply {
    applyProjectProperties(
        "https://raw.githubusercontent.com/korlibs/korge/main/korlibs-number",
        "Public Domain",
        "https://raw.githubusercontent.com/korlibs/korge/main/korlibs-number/LICENSE"
    )
}

dependencies {
}
