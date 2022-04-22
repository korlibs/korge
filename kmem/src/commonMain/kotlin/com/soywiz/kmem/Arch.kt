package com.soywiz.kmem

import com.soywiz.kmem.internal.currentArch

enum class Arch(val bits: Int) {
    UNKNOWN(-1),
    X86(32),
    X64(64),
    ARM32(32),
    ARM64(64),
    MIPS32(32),
    MIPSEL32(32),
    MIPS64(64),
    MIPSEL64(64),
    WASM32(32),
    POWERPC64(64);

    companion object {
        val CURRENT: Arch get() = currentArch
    }
}
