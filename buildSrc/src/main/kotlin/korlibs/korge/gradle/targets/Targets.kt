package korlibs.korge.gradle.targets

import org.apache.tools.ant.taskdefs.condition.*
import org.gradle.api.*
import org.jetbrains.kotlin.gradle.plugin.*

val supportKotlinNative: Boolean get() {
    // Linux and Windows ARM hosts doesn't have K/N toolchains
    if ((isLinux || isWindows) && isArm) return false
    return true
}

val isWindows get() = Os.isFamily(Os.FAMILY_WINDOWS)
val isMacos get() = Os.isFamily(Os.FAMILY_MAC)
val isLinux get() = Os.isFamily(Os.FAMILY_UNIX) && !isMacos
val isArm get() = listOf("arm", "arm64", "aarch64").any { Os.isArch(it) }
val inCI: Boolean get() = !System.getenv("CI").isNullOrBlank() || !System.getProperty("CI").isNullOrBlank()

val KotlinTarget.isIos get() = name.startsWith("ios")
val KotlinTarget.isTvos get() = name.startsWith("tvos")

fun NamedDomainObjectContainer<KotlinSourceSet>.createPairSourceSet(name: String, vararg dependencies: PairSourceSet, doTest: Boolean = true, block: KotlinSourceSet.(test: Boolean) -> Unit = { }): PairSourceSet {
    val main = maybeCreate("${name}Main").apply { block(false) }
    val test = if (doTest) maybeCreate("${name}Test").apply { block(true) } else null
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
