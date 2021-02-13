package com.soywiz.korau.sound.impl.jna

import com.soywiz.korio.util.OS
import com.sun.jna.Native
import java.io.File

private val arch by lazy { System.getProperty("os.arch").toLowerCase() }
private val alClassLoader by lazy { JnaOpenALNativeSoundProvider::class.java.classLoader }
private fun getNativeFile(path: String): ByteArray = alClassLoader.getResource(path)?.readBytes() ?: error("Can't find '$path'")
private fun getNativeFileLocalPath(path: String): String {
    val tempDir = File(System.getProperty("java.io.tmpdir"))
    //val tempFile = File.createTempFile("libopenal_", ".${File(path).extension}")
    val tempFile = File(tempDir, "korau_openal.${File(path).extension}")
    if (!tempFile.exists()) {
        try {
            tempFile.writeBytes(getNativeFile(path))
        } catch (e: Throwable) {
            e.printStackTrace()
        }
    }
    return tempFile.absolutePath
}

val nativeOpenALLibraryPath: String? by lazy {
    when {
        OS.isMac -> {
            //getNativeFileLocalPath("natives/macosx64/libopenal.dylib")
            "OpenAL" // Mac already includes the OpenAL library
        }
        OS.isLinux -> {
            when {
                arch.contains("arm") -> getNativeFileLocalPath("natives/linuxarm/libopenal.so")
                arch.contains("64") -> getNativeFileLocalPath("natives/linuxx64/libopenal.so")
                else -> getNativeFileLocalPath("natives/linuxx86/libopenal.so")
            }
        }
        OS.isWindows -> {
            when {
                arch.contains("64") -> getNativeFileLocalPath("natives/winx64/soft_oal.dll")
                else -> getNativeFileLocalPath("natives/winx86/soft_oal.dll")
            }
        }
        else -> {
            println("  - Unknown/Unsupported OS")
            null
        }
    }
}

val alq: AL? by lazy {
    runCatchingAl {
        try {
            Native.load(nativeOpenALLibraryPath, AL::class.java)
        } catch (e: Throwable) {
            println("Failed to initialize OpenAL: arch=$arch, OS.rawName=${OS.rawName}, nativeOpenALLibraryPath=$nativeOpenALLibraryPath")
            e.printStackTrace()
            throw e
        }
    }
}

val al: AL by lazy { alq ?: ALDummy }
