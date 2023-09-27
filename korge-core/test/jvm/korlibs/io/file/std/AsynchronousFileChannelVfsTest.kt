package korlibs.io.file.std

import korlibs.io.async.suspendTest
import korlibs.io.lang.Environment
import korlibs.io.lang.tempPath
import kotlin.test.Test
import kotlin.test.assertEquals

class AsynchronousFileChannelVfsTest {
    @Test
    fun test() = suspendTest {
        val vfs = AsynchronousFileChannelVfs()
        val file = vfs["${Environment.tempPath}/AsynchronousFileChannelVfsTest.test.txt"]
        file.writeString("HELLO")
        assertEquals("HELLO", file.readString())
    }
}
