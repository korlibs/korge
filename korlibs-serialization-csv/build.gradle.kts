import korlibs.*

description = "Korlibs CSV Serialization Library"

project.extensions.extraProperties.properties.apply {
    applyProjectProperties(
        "https://raw.githubusercontent.com/korlibs/korge/main/korlibs-serialization-csv",
        "Public Domain",
        "https://raw.githubusercontent.com/korlibs/korge/main/korlibs-serialization-csv/LICENSE"
    )
}


dependencies {
    commonMainApi(project(":korlibs-util"))
}
