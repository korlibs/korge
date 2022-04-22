package com.soywiz.kmem

import com.soywiz.kmem.internal.currentIsDebug
import com.soywiz.kmem.internal.currentIsLittleEndian
import com.soywiz.kmem.internal.currentRawOsName
import com.soywiz.kmem.internal.currentRawPlatformName

object Platform {
    // Endianness
    val endian: Endian get() = Endian.NATIVE
    val isLittleEndian: Boolean get() = currentIsLittleEndian
    val isBigEndian: Boolean get() = !currentIsLittleEndian

    // Architecture, operating system & runtime
    val arch: Arch get() = Arch.CURRENT
    val os: Os get() = Os.CURRENT
    val runtime: Runtime get() = Runtime.CURRENT
    val rawPlatformName: String get() = currentRawPlatformName
    val rawOsName: String get() = currentRawOsName

    // Build variant: debug, release
    val buildVariant: BuildVariant get() = BuildVariant.CURRENT
    val isDebug: Boolean get() = currentIsDebug
    val isRelease: Boolean get() = !currentIsDebug

}
