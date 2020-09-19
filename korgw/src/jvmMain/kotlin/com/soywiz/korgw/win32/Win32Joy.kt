package com.soywiz.korgw.win32

import com.sun.jna.Library
import com.sun.jna.Memory
import com.sun.jna.Native
import com.sun.jna.Structure

internal interface Win32Joy : Library {
    companion object {
        val LIBRARY_NAME = "Winmm.dll"

        operator fun invoke(): Win32Joy? = try {
            Native.load(LIBRARY_NAME, Win32Joy::class.java).also {
                it.joyGetDevCapsW(0, null, 0)
            }
        } catch (e: Throwable) {
            e.printStackTrace()
            null
        }
    }

    fun joyGetDevCapsW(
        uJoyID: Int,
        pjc: JoyCapsW?,
        cbjc: Int,
    ): Int
}

internal class JoyCapsW : Structure(Memory(SIZE.toLong())) {
    companion object {
        val SIZE = 728
    }

    @JvmField var wMid: Short = 0
    @JvmField var wPid: Short = 0
    @JvmField var szPname: ByteArray = ByteArray(32 * 2)
    val name get() = szPname.toString(Charsets.UTF_16LE).trimEnd('\u0000')

    override fun getFieldOrder(): List<String> {
        return listOf(
            JoyCapsW::wMid.name,
            JoyCapsW::wPid.name,
            JoyCapsW::szPname.name,
        )
    }
}

/*
object JoySample {
    @JvmStatic
    fun main(args: Array<String>) {
        val caps = JoyCapsW()
        val result = Win32Joy()!!.joyGetDevCapsW(0, caps, JoyCapsW.SIZE)
        println(result)
        if (result == 0) {
            println(caps.name)
        }
    }
}
*/
