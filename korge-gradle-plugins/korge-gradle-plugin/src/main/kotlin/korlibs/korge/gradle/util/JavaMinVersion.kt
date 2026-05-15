package korlibs.korge.gradle.util

import korlibs.*

fun checkMinimumJavaVersion() {
    val javaVersionProp = System.getProperty("java.version") ?: "unknown"
    val javaVersion = currentJavaVersion()

    if (javaVersion < 11) {
        error("Java 11 or greater is is required, but found $javaVersion - $javaVersionProp")
    }
}
