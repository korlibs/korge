package com.soywiz.kmem

import com.soywiz.kmem.internal.currentIsDebug
import com.soywiz.kmem.internal.currentIsLittleEndian
import com.soywiz.kmem.internal.currentRawOsName
import com.soywiz.kmem.internal.currentRawPlatformName
import com.soywiz.kmem.internal.multithreadedSharedHeap

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
     * JVM: true
     * Android: true
     * JS: false <-- workers have different heaps
     * K/N:
     *   - new memory model: true
     *   - old memory model: false <-- frozen
     * */
    val hasMultithreadedSharedHeap: Boolean

    val isLittleEndian: Boolean get() = endian == Endian.LITTLE_ENDIAN
    val isBigEndian: Boolean get() = endian == Endian.BIG_ENDIAN
    val isDebug: Boolean get() = buildVariant == BuildVariant.DEBUG
    val isRelease: Boolean get() = buildVariant == BuildVariant.RELEASE

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
        override val hasMultithreadedSharedHeap: Boolean
    ) : Platform
}
