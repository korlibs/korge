
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
    //add("commonMainImplementation", "org.jetbrains.kotlinx:atomicfu:${libs.versions.kotlinx.atomicfu.get()}")
    //add("commonMainApi", "org.jetbrains.kotlinx:atomicfu:${libs.versions.kotlinx.atomicfu.get()}")
    //add("jvmTestApi", "org.powermock:powermock-mockito-release-full:1.6.4")
    //add("jvmTestApi", "org.fuin:units4j:0.8.4")
    //add("jvmTestApi", "org.ow2.asm:asm:8.0.1")
    commonMainApi(libs.kotlinx.coroutines.core)
    commonMainApi(libs.kotlinx.atomicfu)
    //add("commonMainApi", libs.kotlinx.atomicfu)
    //add("commonTestApi", project(":korge-test"))
    commonTestApi(libs.kotlinx.coroutines.test)
    commonMainApi(project(":korlibs-time"))
    commonMainApi(project(":korlibs-crypto"))
    commonMainApi(project(":korlibs-platform"))
    commonMainApi(project(":korlibs-math-core"))
    commonMainApi(project(":korlibs-memory"))
    commonMainApi(project(":korlibs-datastructure"))
    commonMainApi(project(":korlibs-logger"))
    commonMainApi(project(":korlibs-inject"))
    commonMainApi(project(":korlibs-util"))
}
