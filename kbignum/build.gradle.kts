import korlibs.applyProjectProperties

description = "Big Integers and decimals in Kotlin Common"

project.extensions.extraProperties.properties.apply {
    applyProjectProperties("https://github.com/korlibs/korge/main/kbignum","MIT License","https://raw.githubusercontent.com/korlibs/korge/main/kbignum/LICENSE")
}

dependencies {
}
