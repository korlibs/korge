package korlibs.korge.gradle.targets

enum class ProjectType {
    EXECUTABLE, LIBRARY;

    val isExecutable: Boolean get() = this == EXECUTABLE
    val isLibrary: Boolean get() = this == LIBRARY

    companion object {
        fun fromExecutable(executable: Boolean) = if (executable) EXECUTABLE else LIBRARY
    }
}
