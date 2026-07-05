package korlibs.korge.gradle.util

import korlibs.*

// TODO Either remove this check or move the min java version to global variables to not lose track
// TODO Check if min java version is actually 11 or higher (e.g. 17 or 21)
fun checkMinimumJavaVersion() {
    val javaVersionProp = System.getProperty("java.version") ?: "unknown"
    val javaVersion = currentJavaVersion()

    if (javaVersion < 11) {
        error("Java 11 or greater is is required, but found $javaVersion - $javaVersionProp")
    }
}
