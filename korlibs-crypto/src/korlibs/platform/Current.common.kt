package korlibs.platform

internal expect val currentOs: Os
internal expect val currentRuntime: Runtime
internal expect val currentArch: Arch
internal expect val currentIsDebug: Boolean
internal expect val currentIsLittleEndian: Boolean
internal expect val currentRawPlatformName: String
internal expect val currentRawOsName: String
val currentBuildVariant: BuildVariant get() = if (currentIsDebug) BuildVariant.DEBUG else BuildVariant.RELEASE
internal expect val multithreadedSharedHeap: Boolean
