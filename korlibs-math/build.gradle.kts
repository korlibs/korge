import korlibs.*

description = "Korlibs Math"

project.extensions.extraProperties.properties.apply {
    applyProjectProperties(
        "https://raw.githubusercontent.com/korlibs/korge/main/korlibs-math",
        "Public Domain",
        "https://raw.githubusercontent.com/korlibs/korge/main/korlibs-math/LICENSE"
    )
}

dependencies {
    commonMainApi(project(":korlibs-util"))
    commonMainApi(project(":korlibs-number"))
    commonMainApi(project(":korlibs-math-core"))
    commonMainApi(project(":korlibs-datastructure"))
}
