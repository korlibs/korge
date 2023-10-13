package korlibs.korge.gradle.targets

import korlibs.korge.gradle.*
import org.gradle.api.*

val Project.exKotlinSourceSetContainer: ExKotlinSourceSetContainer get() = extensionGetOrCreate("exKotlinSourceSetContainer")

open class ExKotlinSourceSetContainer(val project: Project) {
    val kotlin = project.kotlin
    val sourceSets = kotlin.sourceSets

    val common by lazy { sourceSets.createPairSourceSet("common") }
    val nonJs by lazy { sourceSets.createPairSourceSet("nonJs", common) }
    val concurrent by lazy { sourceSets.createPairSourceSet("concurrent", common) }

    // JS
    val js by lazy { sourceSets.createPairSourceSet("js", common) }

    // JVM
    val jvm by lazy { sourceSets.createPairSourceSet("jvm", concurrent, nonJs) }

    // Native
    val native by lazy { sourceSets.createPairSourceSet("native", concurrent, nonJs) }
    val posix by lazy { sourceSets.createPairSourceSet("posix", native) }
    val darwin by lazy { sourceSets.createPairSourceSet("darwin", posix) }
    val darwinMobile by lazy { sourceSets.createPairSourceSet("darwinMobile", darwin) }
    val iosTvos by lazy { sourceSets.createPairSourceSet("iosTvos", darwinMobile/*, iosTvosMacos*/) }
    val tvos by lazy { sourceSets.createPairSourceSet("tvos", iosTvos) }
    val ios by lazy { sourceSets.createPairSourceSet("ios", iosTvos/*, iosMacos*/) }
}

