package com.soywiz.korvi.mpeg

import com.soywiz.kmem.Int32Buffer
import com.soywiz.kmem.arraycopy
import com.soywiz.kmem.mem
import com.soywiz.kmem.toUint8Buffer
import com.soywiz.korio.async.suspendTest
import com.soywiz.korio.file.std.resourcesVfs
import com.soywiz.krypto.MD5
import kotlin.test.Test
import kotlin.test.assertEquals

class MpegTest {
    @Test
    fun test() = suspendTest {
        val data = resourcesVfs["blade-runner-2049-360p-5sec.mpeg1"].readAll()
        val data2 = data.sliceArray(0 until 32 * 1024)
        val player = JSMpegPlayer(coroutineContext)
        player.write(data2.toUint8Buffer())
        //player.write(data2.toUint8Buffer().subarray(0, 32 * 1024))
        for (n in 0 until 7) player.frameSimple()
        val bytes = ByteArray(player.bitmap.ints.size * 4)
        arraycopy(Int32Buffer(player.bitmap.ints).mem, 0, bytes, 0, bytes.size)
        assertEquals("1065ea34f461308d055b08c498f37953", MD5.digest(bytes).hex)
        //player.bitmap.showImageAndWait()
    }
}
