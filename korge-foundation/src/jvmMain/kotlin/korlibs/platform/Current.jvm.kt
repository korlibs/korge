package korlibs.platform

import java.lang.management.ManagementFactory
import java.nio.ByteOrder

internal actual val currentOs: Os by lazy {
    val os = System.getProperty("os.name").lowercase()
    when {
        os.contains("linux") -> Os.LINUX
        os.contains("android") -> Os.ANDROID
        os.contains("mac") || os.contains("darwin") -> Os.MACOSX
        os.contains("window") -> Os.WINDOWS
        else -> Os.UNKNOWN
    }
}
internal actual val currentRuntime: Runtime = Runtime.JVM
internal actual val currentArch: Arch by lazy {
    val arch = System.getProperty("os.arch").lowercase()
    when {
        arch.contains("powerpc") || arch.contains("ppc") -> Arch.POWERPC64
        arch.contains("amd64") || arch.contains("x86_64") || arch.contains("x64") -> Arch.X64
        arch.contains("i386") || arch.contains("i486") || arch.contains("i586") || arch.contains("i686") || arch.contains(
            "x86"
        ) -> Arch.X86
        arch.contains("mips32") || arch.contains("mips32el") -> Arch.MIPS32
        arch.contains("mips64") || arch.contains("mips64el") -> Arch.MIPS64
        arch.contains("aarch64") -> Arch.ARM64
        arch.contains("arm") -> Arch.ARM32
        else -> Arch.UNKNOWN
    }
}
internal actual val multithreadedSharedHeap: Boolean = true

internal actual val currentIsDebug: Boolean by lazy {
    val inputArguments = ManagementFactory.getRuntimeMXBean().inputArguments
    val inputArgumentsString = inputArguments.toString()
    inputArguments.contains("-Xdebug") || inputArgumentsString.contains("-agentlib:jdwp")
}
internal actual val currentIsLittleEndian: Boolean get() = ByteOrder.nativeOrder() == ByteOrder.LITTLE_ENDIAN

internal actual val currentRawPlatformName: String = "jvm-$currentOs-$currentArch-$currentBuildVariant"
internal actual val currentRawOsName: String = System.getProperty("os.name")
