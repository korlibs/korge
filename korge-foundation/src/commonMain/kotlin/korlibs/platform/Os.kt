package korlibs.platform

enum class Os {
    UNKNOWN, MACOSX, IOS, LINUX, WINDOWS, ANDROID, WASM, TVOS;

    val isWindows: Boolean get() = this == WINDOWS
    val isAndroid: Boolean get() = this == ANDROID
    val isLinux: Boolean get() = this == LINUX
    val isMac: Boolean get() = this == MACOSX
    val isIos: Boolean get() = this == IOS
    val isTvos: Boolean get() = this == TVOS
    val isAppleMobile: Boolean get() = isIos || isTvos
    val isDesktop: Boolean get() = isLinux || isWindows || isMac
    val isMobile: Boolean get() = isAndroid || isAppleMobile
    val isApple: Boolean get() = isMac || isAppleMobile
    val isPosix: Boolean get() = !isWindows

    companion object {
        val VALUES = values()
        val CURRENT: Os get() = currentOs
    }
}
