package korlibs.korge.gradle.targets

import org.apache.tools.ant.taskdefs.condition.*
import org.gradle.api.*
import org.jetbrains.kotlin.gradle.plugin.*
import java.io.*

// Only mac has ios/tvos targets but since CI exports multiplatform on linux
val supportKotlinNative: Boolean get() {
    // Linux and Windows ARM hosts doesn't have K/N toolchains
    if (isArm && (isLinux || isWindows)) return false
    // We can also try to disable it manually
    if (System.getenv("DISABLE_KOTLIN_NATIVE") == "true") return false
    // On Mac, CI or when FORCE_ENABLE_KOTLIN_NATIVE=true, let's enable it
    //return isMacos || (System.getenv("CI") == "true") || (System.getenv("FORCE_ENABLE_KOTLIN_NATIVE") == "true")
    return true
}

val isWindows get() = Os.isFamily(Os.FAMILY_WINDOWS)
val isMacos get() = Os.isFamily(Os.FAMILY_MAC)
val isLinux get() = Os.isFamily(Os.FAMILY_UNIX) && !isMacos
val isArm get() = listOf("arm", "arm64", "aarch64").any { Os.isArch(it) }
val inCI: Boolean get() = !System.getenv("CI").isNullOrBlank() || !System.getProperty("CI").isNullOrBlank()

val KotlinTarget.isIos get() = name.startsWith("ios")
val KotlinTarget.isTvos get() = name.startsWith("tvos")
val KotlinTarget.isLinux get() = name.startsWith("linux")
val KotlinTarget.isMingw get() = name.startsWith("mingw")
val KotlinTarget.isMacos get() = name.startsWith("macos")

fun NamedDomainObjectContainer<KotlinSourceSet>.createPairSourceSet(
    name: String,
    vararg dependencies: PairSourceSet, doTest: Boolean = true,
    project: Project? = null,
    block: KotlinSourceSet.(test: Boolean) -> Unit = { }
): PairSourceSet {
    val main = maybeCreate("${name}Main").apply { block(false) }
    val test = if (doTest) maybeCreate("${name}Test").apply { block(true) } else null

    val newVariant = if (project?.projectDir != null) !File(project.projectDir, "src/commonMain").isDirectory else false

    //println("!!!!!!!!!!! newVariant=$newVariant, project?.projectDir=${project?.projectDir} isDirectory=${project?.projectDir?.get("src/commonMain")?.isDirectory}")

    if (newVariant) {
        if (name == "common") {
            main.kotlin.srcDirs(listOf("src"))
            main.resources.srcDirs(listOf("resources"))
        } else {
            main.kotlin.srcDirs(listOf("src@$name"))
        }
        if (test != null) {
            //test.kotlin.srcDirs(listOf("test/$name"))
            if (name == "common") {
                test.kotlin.srcDirs(listOf("test"))
                test.resources.srcDirs(listOf("testresources"))
            } else {
                test.kotlin.srcDirs(listOf("test@$name"))
            }
        }
    }

    return PairSourceSet(main, test).also {
        for (dependency in dependencies) {
            it.dependsOn(dependency)
        }
    }
}

data class PairSourceSet(val main: KotlinSourceSet, val test: KotlinSourceSet?) {
    fun get(test: Boolean) = if (test) this.test else this.main
    fun dependsOn(vararg others: PairSourceSet) {
        for (other in others) {
            main.dependsOn(other.main)
            other.test?.let { test?.dependsOn(it) }
        }
    }
}
