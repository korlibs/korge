package korlibs.platform

import android.os.*
import korlibs.korlibs_platform.*
import java.nio.*

internal actual val currentOs: Os = Os.ANDROID
internal actual val currentRuntime: Runtime = Runtime.ANDROID
internal actual val multithreadedSharedHeap: Boolean = true

// @TODO:
//System.getProperty("os.arch")
internal actual val currentArch: Arch by lazy {
    val androidArchs: Array<String?>? = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP -> Build.SUPPORTED_ABIS
        else -> arrayOf(Build.CPU_ABI)
    } ?: return@lazy Arch.UNKNOWN
    for (androidArch in androidArchs!!) {
        if (androidArch == null) continue
        when {
            androidArch.contains("arm64") -> return@lazy Arch.ARM64 // "arm64-v8a"
            androidArch.contains("arm") -> return@lazy Arch.ARM32 // "armeabi-v7a"
            androidArch.contains("x86_64") -> return@lazy Arch.X64 // "x86_64"
            androidArch.contains("x86") -> return@lazy Arch.X86 // "x86"
            androidArch.contains("mips") -> return@lazy Arch.MIPS32
        }
    }
    println("Undetected android architecture: ${androidArchs.toList()}")
    return@lazy Arch.UNKNOWN
}

internal actual val currentIsDebug: Boolean get() = BuildConfig.DEBUG
internal actual val currentIsLittleEndian: Boolean get() = ByteOrder.nativeOrder() == ByteOrder.LITTLE_ENDIAN

internal actual val currentRawPlatformName: String = "android-$currentOs-$currentArch-$currentBuildVariant"
internal actual val currentRawOsName: String = System.getProperty("os.name") ?: "android"
