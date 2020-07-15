package com.soywiz.korim.format

import com.soywiz.korim.bitmap.*
import com.soywiz.korim.color.*
import com.soywiz.korio.async.*
import com.soywiz.korio.file.std.*
import kotlin.test.*

class ICOTest {
    @Test
    fun test() = suspendTest {
        //rootLocalVfs["/tmp/demo.ico"].writeBytes(Bitmap32(32, 32, Colors.RED, premultiplied = false).encode(ICO))
    }
}
