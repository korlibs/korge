package korlibs.korge.gradle.targets

import korlibs.korge.gradle.*
import org.gradle.api.*

val Project.exKotlinSourceSetContainer: ExKotlinSourceSetContainer get() = extensionGetOrCreate("exKotlinSourceSetContainer")

open class ExKotlinSourceSetContainer(val project: Project) {
    val kotlin = project.kotlin
    val sourceSets = kotlin.sourceSets

    val common by lazy { sourceSets.createPairSourceSet("common", project = project) }
    val nonJs by lazy { sourceSets.createPairSourceSet("nonJs", common, project = project) }
    val concurrent by lazy { sourceSets.createPairSourceSet("concurrent", common, project = project) }

    // JS
    val js by lazy { sourceSets.createPairSourceSet("js", common, project = project) }

    // JVM
    val jvm by lazy { sourceSets.createPairSourceSet("jvm", concurrent, nonJs, project = project) }

    // Native
    val native by lazy { sourceSets.createPairSourceSet("native", concurrent, nonJs, project = project) }
    val posix by lazy { sourceSets.createPairSourceSet("posix", native, project = project) }
    val darwin by lazy { sourceSets.createPairSourceSet("darwin", posix, project = project) }
    val darwinMobile by lazy { sourceSets.createPairSourceSet("darwinMobile", darwin, project = project) }
    val iosTvos by lazy { sourceSets.createPairSourceSet("iosTvos", darwinMobile/*, iosTvosMacos*/, project = project) }
    val tvos by lazy { sourceSets.createPairSourceSet("tvos", iosTvos, project = project) }
    val ios by lazy { sourceSets.createPairSourceSet("ios", iosTvos/*, iosMacos*/, project = project) }
}

