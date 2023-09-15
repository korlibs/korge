@file:OptIn(ExperimentalNativeApi::class)

package korlibs.io

import kotlin.experimental.*

expect fun nativeCwd(): String

val nativeOsfamilyName: String get() = when (Platform.osFamily) {
    OsFamily.MACOSX -> "macos"
    OsFamily.IOS -> "ios"
    OsFamily.LINUX -> "linux"
    OsFamily.WINDOWS -> "windows"
    OsFamily.ANDROID -> "android"
    OsFamily.WASM -> "wasm"
    OsFamily.TVOS -> "tvos"
    else -> "unknown"
}

val nativeArchName: String get() = when (Platform.cpuArchitecture) {
    CpuArchitecture.ARM32 -> "Arm32"
    CpuArchitecture.ARM64 -> "Arm64"
    CpuArchitecture.X86 -> "X86"
    CpuArchitecture.X64 -> "X64"
    CpuArchitecture.MIPS32 -> "Mips32"
    CpuArchitecture.MIPSEL32 -> "Mipsel32"
    CpuArchitecture.WASM32 -> "Wasm32"
    else -> "Unknown"
}
