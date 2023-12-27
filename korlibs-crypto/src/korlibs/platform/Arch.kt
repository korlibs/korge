package korlibs.platform

enum class Arch(val bits: Int, val isArm: Boolean = false, val isX86OrX64: Boolean = false, val isMips: Boolean = false, val isWasm: Boolean = false, val isPowerPC: Boolean = false) {
    UNKNOWN(-1),
    X86(32, isX86OrX64 = true),
    X64(64, isX86OrX64 = true),
    ARM32(32, isArm = true),
    ARM64(64, isArm = true),
    MIPS32(32, isMips = true),
    MIPSEL32(32, isMips = true),
    MIPS64(64, isMips = true),
    MIPSEL64(64, isMips = true),
    WASM32(32, isWasm = true),
    POWERPC64(64, isPowerPC = true);

    val is32Bits: Boolean get() = bits == 32
    val is64Bits: Boolean get() = bits == 64

    val isX86: Boolean get() = this == X86
    val isX64: Boolean get() = this == X64

    val isArm32: Boolean get() = this == ARM32
    val isArm64: Boolean get() = this == ARM64

    val isMIPS32: Boolean get() = this == MIPS32
    val isMIPSEL32: Boolean get() = this == MIPSEL32
    val isMIPS64: Boolean get() = this == MIPS64
    val isMIPSEL64: Boolean get() = this == MIPSEL64

    val isWASM32: Boolean get() = this == WASM32
    val isPOWERPC64: Boolean get() = this == POWERPC64

    companion object {
        val CURRENT: Arch get() = currentArch
    }
}
