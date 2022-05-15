package com.soywiz.korim.format

import com.soywiz.korio.async.suspendTest
import kotlin.test.Test

class ICOTest {
    @Test
    fun test() = suspendTest {
        //rootLocalVfs["/tmp/demo.ico"].writeBytes(Bitmap32(32, 32, Colors.RED, premultiplied = false).encode(ICO))
    }
}
