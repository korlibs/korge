package korlibs.platforminternal

internal enum class BuildVariant {
    DEBUG, RELEASE;

    val isDebug: Boolean get() = this == DEBUG
    val isRelease: Boolean get() = this == RELEASE

    companion object {
        val CURRENT: BuildVariant get() = currentBuildVariant
    }
}
