package korlibs.platform

interface Platform {
    // Endianness
    val endian: Endian
    val arch: Arch
    val os: Os
    val runtime: Runtime
    val rawPlatformName: String
    val rawOsName: String
    val buildVariant: BuildVariant

    /**
     * JVM, Android & K/N: true
     * Android: true
     * JS: false <-- workers have different heaps
     **/
    val hasMultithreadedSharedHeap: Boolean

    val isLittleEndian: Boolean get() = endian == Endian.LITTLE_ENDIAN
    val isBigEndian: Boolean get() = endian == Endian.BIG_ENDIAN
    val isDebug: Boolean get() = buildVariant == BuildVariant.DEBUG
    val isRelease: Boolean get() = buildVariant == BuildVariant.RELEASE

    val isWindows: Boolean get() = os.isWindows
    val isUnix: Boolean get() = os.isPosix
    val isPosix: Boolean get() = os.isPosix
    val isLinux: Boolean get() = os.isLinux
    val isMac: Boolean get() = os.isMac
    val isApple: Boolean get() = os.isApple
    val isAppleMobile: Boolean get() = os.isAppleMobile

    val isIos: Boolean get() = os.isIos
    val isAndroid: Boolean get() = os.isAndroid
    val isTvos: Boolean get() = os.isTvos

    val isJs: Boolean get() = runtime.isJs
    val isNative: Boolean get() = runtime.isNative
    val isNativeDesktop: Boolean get() = isNative && os.isDesktop
    val isJvm: Boolean get() = runtime.isJvm
    val isWasm: Boolean get() = runtime.isWasm
    val isJsOrWasm: Boolean get() = isJs || isWasm

    val isJsShell: Boolean get() = rawPlatformName == "js-shell" || rawPlatformName == "wasm-shell"
    val isJsNodeJs: Boolean get() = rawPlatformName == "js-node" || rawPlatformName == "wasm-node"
    val isJsDenoJs: Boolean get() = rawPlatformName == "js-deno" || rawPlatformName == "wasm-deno"
    val isJsBrowser: Boolean get() = rawPlatformName == "js-web" || rawPlatformName == "wasm-web"
    val isJsWorker: Boolean get() = rawPlatformName == "js-worker" || rawPlatformName == "wasm-worker"
    val isJsBrowserOrWorker: Boolean get() = isJsBrowser || isJsWorker


    companion object : Platform {
        override val endian: Endian get() = Endian.NATIVE
        override val isLittleEndian: Boolean get() = currentIsLittleEndian
        override val isBigEndian: Boolean get() = !currentIsLittleEndian
        override val arch: Arch get() = Arch.CURRENT
        override val os: Os get() = Os.CURRENT
        override val runtime: Runtime get() = Runtime.CURRENT
        override val rawPlatformName: String get() = currentRawPlatformName
        override val rawOsName: String get() = currentRawOsName
        override val buildVariant: BuildVariant get() = BuildVariant.CURRENT
        override val isDebug: Boolean get() = currentIsDebug
        override val isRelease: Boolean get() = !currentIsDebug
        override val hasMultithreadedSharedHeap: Boolean get() = multithreadedSharedHeap

        operator fun invoke(
            endian: Endian = Endian.LITTLE_ENDIAN,
            arch: Arch = Arch.UNKNOWN,
            os: Os = Os.UNKNOWN,
            runtime: Runtime = Runtime.JVM,
            buildVariant: BuildVariant = BuildVariant.DEBUG,
            rawPlatformName: String = "unknown",
            rawOsName: String = "unknown",
            hasMultithreadedSharedHeap: Boolean = false,
        ): Platform = Impl(endian, arch, os, runtime, buildVariant, rawPlatformName, rawOsName, hasMultithreadedSharedHeap)
    }

    data class Impl(
        override val endian: Endian,
        override val arch: Arch,
        override val os: Os,
        override val runtime: Runtime,
        override val buildVariant: BuildVariant,
        override val rawPlatformName: String,
        override val rawOsName: String,
        override val hasMultithreadedSharedHeap: Boolean,
    ) : Platform
}
