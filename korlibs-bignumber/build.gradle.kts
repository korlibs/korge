
import korlibs.*
import korlibs.korge.gradle.generate.*

description = "Korlibs Bignumber Library"

project.extensions.extraProperties.properties.apply {
    applyProjectProperties("https://github.com/korlibs/korge/korlibs-bignumber",
        "Public Domain",
        "https://raw.githubusercontent.com/korlibs/korge/master/korlibs-bignumber/LICENSE"
    )
}
