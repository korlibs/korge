package korlibs.datastructure

import kotlin.test.*

class SparseChunkedStackedIntArray2Test {
    @Test
    fun test() {
        val sparse = SparseChunkedStackedIntArray2()
        val chunk1 = StackedIntArray2(16, 16, startX = -100, startY = -100)
        val chunk2 = StackedIntArray2(16, 16, startX = -50, startY = 50)
        sparse.putChunk(chunk1)
        sparse.putChunk(chunk2)

        assertTrue {
            (0 until 16).all { y ->
                (0 until 16).all { x ->
                    sparse.getChunkAt(-100 + x, -100 + y) === chunk1
                }
            }
        }

        assertEquals(null, sparse.getChunkAt(-101, -100))
        assertEquals(null, sparse.getChunkAt(-100 + 16, -100))
        assertEquals(null, sparse.getChunkAt(10, -200))
        assertEquals(chunk1, sparse.getChunkAt(-90, -90))
        assertEquals(chunk2, sparse.getChunkAt(-40, 55))

        assertEquals(-1, sparse.getLast(-200, -200))
        assertEquals(-1, sparse.getLast(-90, -90))

        sparse.push(-200, -200, 10)
        sparse.push(-90, -90, 10)
        sparse.push(-200, -200, 20)
        sparse.push(-90, -90, 20)

        assertEquals(-1, sparse.getLast(-200, -200))
        assertEquals(20, sparse.getLast(-90, -90))
        assertEquals(-1, sparse.getFirst(-200, -200))
        assertEquals(10, sparse.getFirst(-90, -90))


        sparse.push(-40, 55, 10)

        assertEquals(10, sparse.getLast(-40, 55))
        assertEquals(10, sparse.getFirst(-40, 55))

        sparse.push(-40, 55, 20)

        assertEquals(20, sparse.getLast(-40, 55))
        assertEquals(10, sparse.getFirst(-40, 55))

        val sparse2 = sparse.clone()

        sparse2.push(-40, 55, 30)

        assertEquals(20, sparse.getLast(-40, 55))
        assertEquals(10, sparse.getFirst(-40, 55))

        assertEquals(2, sparse.findAllChunks().size)
        assertEquals(2, sparse2.findAllChunks().size)

        assertEquals(30, sparse2.getLast(-40, 55))
        assertEquals(10, sparse2.getFirst(-40, 55))

        assertEquals(
            "-100, -100, -34, 66",
            "${sparse2.startX}, ${sparse2.startY}, ${sparse2.endX}, ${sparse2.endY}"
        )
    }
}
