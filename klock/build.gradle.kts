import korlibs.applyProjectProperties

description = "Consistent and portable date and time utilities for multiplatform Kotlin"

project.extensions.extraProperties.properties.apply {
    applyProjectProperties("https://github.com/korlibs/korge/main/kbignum",
        "CC0 1.0 Universal",
        "https://raw.githubusercontent.com/korlibs/klock/master/LICENSE"
        )
}
