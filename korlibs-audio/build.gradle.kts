
import korlibs.*
import korlibs.korge.gradle.generate.*

description = "Korlibs Audio Library"

project.extensions.extraProperties.properties.apply {
    applyProjectProperties("https://github.com/korlibs/korge/korlibs-audio",
        "MIT",
        "https://raw.githubusercontent.com/korlibs/korge/master/korlibs-audio/LICENSE"
    )
}

dependencies {
    commonMainApi(project(":korlibs-io"))
    commonMainApi(project(":korlibs-ffi"))
    commonTestApi(libs.kotlinx.coroutines.test)
}
