
import korlibs.*
import korlibs.korge.gradle.generate.*

description = "Korlibs Cryptography Library"

project.extensions.extraProperties.properties.apply {
    applyProjectProperties("https://github.com/korlibs/korge/korge-crypto",
        "Apache 2.0",
        "https://raw.githubusercontent.com/korlibs/korge/master/korge-crypto/LICENSE"
    )
}
