@file:OptIn(ExperimentalNativeApi::class)

package korlibs.platform

import kotlin.experimental.*

internal actual val currentOs: Os = when (kotlin.native.Platform.osFamily) {
    OsFamily.MACOSX -> Os.MACOSX
    OsFamily.IOS -> Os.IOS
    OsFamily.LINUX -> Os.LINUX
    OsFamily.WINDOWS -> Os.WINDOWS
    OsFamily.ANDROID -> Os.ANDROID
    OsFamily.WASM -> Os.WASM
    OsFamily.TVOS -> Os.TVOS
    else -> Os.UNKNOWN
}

internal actual val currentRuntime: Runtime = Runtime.NATIVE

internal actual val currentArch: Arch = when (kotlin.native.Platform.cpuArchitecture) {
    CpuArchitecture.ARM32 -> Arch.ARM32
    CpuArchitecture.ARM64 -> Arch.ARM64
    CpuArchitecture.X86 -> Arch.X86
    CpuArchitecture.X64 -> Arch.X64
    CpuArchitecture.MIPS32 -> Arch.MIPS32
    CpuArchitecture.MIPSEL32 -> Arch.MIPSEL32
    CpuArchitecture.WASM32 -> Arch.WASM32
    else -> Arch.UNKNOWN
}

internal actual val currentIsDebug: Boolean get() = kotlin.native.Platform.isDebugBinary
internal actual val currentIsLittleEndian: Boolean get() = kotlin.native.Platform.isLittleEndian
internal actual val multithreadedSharedHeap: Boolean = true
internal actual val currentRawPlatformName: String = "native-$currentOs-$currentArch-$currentBuildVariant"
internal actual val currentRawOsName: String = "$currentOs"
