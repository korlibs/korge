import korlibs.applyProjectProperties

description = "Multiplatform Game Engine written in Kotlin"

project.extensions.extraProperties.properties.apply {
    applyProjectProperties(
        "https://github.com/korlibs/korge",
        "MIT License",
        "https://raw.githubusercontent.com/korlibs/korge/master/LICENSE"
    )
}

dependencies {
    commonMainApi(project(":korge-core"))
    jvmMainApi(project(":korge-ipc"))
    add("jvmMainApi", "org.jetbrains.kotlinx:kotlinx-coroutines-debug:1.9.0-RC")
}
