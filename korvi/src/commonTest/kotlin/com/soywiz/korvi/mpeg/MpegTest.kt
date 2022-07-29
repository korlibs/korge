package com.soywiz.korvi.mpeg

import com.soywiz.kmem.subarray
import com.soywiz.kmem.toUint8Buffer
import com.soywiz.korio.async.suspendTest
import com.soywiz.korio.file.std.resourcesVfs
import kotlin.test.Test

class MpegTest {
    @Test
    fun test() = suspendTest {
        val data = resourcesVfs["blade-runner-2049-360p-5sec.mpeg1"].readAll()
        val data2 = data.sliceArray(0 until 4 * 1024)
        val player = JSMpegPlayer()
        player.write(data2.toUint8Buffer().subarray(0, 4 * 1024))
        player.frame()
    }
}
