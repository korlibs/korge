import korlibs.*

description = "Korlibs Numbers - Former Klock"

dependencies {
    commonMainApi(project(":korlibs-time"))
}


project.extensions.extraProperties.properties.apply {
    applyProjectProperties(
        "https://raw.githubusercontent.com/korlibs/korge/main/korlibs-number",
        "Public Domain",
        "https://raw.githubusercontent.com/korlibs/korge/main/korlibs-number/LICENSE"
    )
}
