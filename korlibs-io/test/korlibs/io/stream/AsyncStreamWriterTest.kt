package korlibs.io.stream

import korlibs.io.async.suspendTest
import kotlin.test.Test
import kotlin.test.assertEquals

class AsyncStreamWriterTest {
    val log = arrayListOf<String>()
    val logStr: String get() = log.joinToString(",")

    @Test
    fun test() = suspendTest {
        val stream = asyncStreamWriter(lazy = true) {
            log += "start"
            it.write(byteArrayOf(10, 11, 12))
            log += "1"
            it.write(byteArrayOf(13, 14, 15))
            log += "2"
        }

        suspend fun step() = "${stream.read()}:$logStr"

        assertEquals(
            """
                10:start,1
                11:start,1
                12:start,1
                13:start,1,2
                14:start,1,2
                15:start,1,2
                -1:start,1,2
                -1:start,1,2
            """.trimIndent(),
            """
                ${step()}
                ${step()}
                ${step()}
                ${step()}
                ${step()}
                ${step()}
                ${step()}
                ${step()}
            """.trimIndent()
        )
    }
}
