package com.soywiz.korag

import kotlin.test.*

class AGBatchTest {
    @Test
    fun test() {
        val writer = NAGDrawCommandArrayWriter()
        writer.add(AGDrawType.TRIANGLES, AGIndexType.USHORT, 12345, 654321, 999999)
        writer.add(AGDrawType.LINES, AGIndexType.UBYTE, 1193046, 3430008, 305419896)
        val array = writer.toArray()
        val log = arrayListOf<String>()
        array.fastForEach { drawType, indexType, offset, count, instances ->
            log += "$drawType, $indexType, $offset, $count, $instances"
        }
        assertEquals(
            """
                TRIANGLES, USHORT, 12345, 654321, 999999
                LINES, UBYTE, 1193046, 3430008, 305419896
            """.trimIndent(),
            log.joinToString("\n")
        )
    }
}
