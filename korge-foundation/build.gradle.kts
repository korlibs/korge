
import korlibs.*
import korlibs.korge.gradle.generate.*

description = "Korge Foundation Libraries"

project.extensions.extraProperties.properties.apply {
    applyProjectProperties("https://github.com/korlibs/korge/korge-foundation",
        "Apache 2.0",
        "https://raw.githubusercontent.com/korlibs/korge/master/korge-foundation/LICENSE"
    )
}

dependencies {
    commonMainApi(libs.kotlinx.coroutines.core)
    commonMainApi(libs.kotlinx.atomicfu)
    commonMainApi(project(":korlibs-annotations"))
    commonMainApi(project(":korlibs-time"))
    commonMainApi(project(":korlibs-bignumber"))
    commonMainApi(project(":korlibs-datastructure"))
    commonMainApi(project(":korlibs-crypto"))
    commonMainApi(project(":korlibs-platform"))
    commonMainApi(project(":korlibs-math-core"))
    commonMainApi(project(":korlibs-math"))
    commonMainApi(project(":korlibs-memory"))
    commonMainApi(project(":korlibs-number"))
    commonMainApi(project(":korlibs-logger"))
    commonMainApi(project(":korlibs-inject"))
    commonMainApi(project(":korlibs-util"))
    commonTestApi(libs.kotlinx.coroutines.test)
}
