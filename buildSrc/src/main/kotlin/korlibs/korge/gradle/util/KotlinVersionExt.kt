package korlibs.korge.gradle.util

fun KotlinVersion.Companion.fromString(str: String): KotlinVersion {
    val (major, minor, patch) = str.split(".") + listOf("0", "0", "0")
    return KotlinVersion(major.toInt(), minor.toIntOrNull() ?: 0, patch.toIntOrNull() ?: 0)
}
