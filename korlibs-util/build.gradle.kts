import korlibs.*

description = "Korlibs Util"

project.extensions.extraProperties.properties.apply {
    includeKotlinNativeDesktop()

    applyProjectProperties(
        "https://raw.githubusercontent.com/korlibs/korge/main/korlibs-util",
        "Public Domain",
        "https://raw.githubusercontent.com/korlibs/korge/main/korlibs-util/LICENSE"
    )
}
