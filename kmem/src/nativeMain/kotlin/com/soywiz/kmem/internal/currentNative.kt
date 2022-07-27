package com.soywiz.kmem.internal

import com.soywiz.kmem.Arch
import com.soywiz.kmem.Os
import com.soywiz.kmem.Runtime

@SharedImmutable
internal actual val currentOs: Os = when (Platform.osFamily) {
    OsFamily.UNKNOWN -> Os.UNKNOWN
    OsFamily.MACOSX -> Os.MACOSX
    OsFamily.IOS -> Os.IOS
    OsFamily.LINUX -> Os.LINUX
    OsFamily.WINDOWS -> Os.WINDOWS
    OsFamily.ANDROID -> Os.ANDROID
    OsFamily.WASM -> Os.UNKNOWN
    OsFamily.TVOS -> Os.TVOS
    OsFamily.WATCHOS -> Os.WATCHOS
}

@SharedImmutable
internal actual val currentRuntime: Runtime = Runtime.NATIVE

@SharedImmutable
internal actual val currentArch: Arch = when (Platform.cpuArchitecture) {
    CpuArchitecture.UNKNOWN -> Arch.UNKNOWN
    CpuArchitecture.ARM32 -> Arch.ARM32
    CpuArchitecture.ARM64 -> Arch.ARM64
    CpuArchitecture.X86 -> Arch.X86
    CpuArchitecture.X64 -> Arch.X64
    CpuArchitecture.MIPS32 -> Arch.MIPS32
    CpuArchitecture.MIPSEL32 -> Arch.MIPSEL32
    CpuArchitecture.WASM32 -> Arch.WASM32
}

internal actual val currentIsDebug: Boolean get() = Platform.isDebugBinary
internal actual val currentIsLittleEndian: Boolean get() = Platform.isLittleEndian
internal actual val multithreadedSharedHeap: Boolean = Platform.memoryModel == MemoryModel.EXPERIMENTAL

@SharedImmutable
internal actual val currentRawPlatformName: String = "native-$currentOs-$currentArch-$currentBuildVariant"
@SharedImmutable
internal actual val currentRawOsName: String = "$currentOs"
