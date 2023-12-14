package korlibs.datastructure

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFails

class ListReaderTest {
    @Test
    fun test() {
        val reader = listOf(1, 2, 3).reader()
        assertEquals(true, reader.hasMore)
        assertEquals(false, reader.eof)
        assertEquals(1, reader.peek())
        assertEquals(1, reader.peek())
        assertEquals(1, reader.read())
        assertEquals(2, reader.read())
        assertEquals(3, reader.expect(3))
        assertEquals(false, reader.hasMore)
        assertEquals(true, reader.eof)
    }

    @Test
    fun expect() {
        val reader = listOf(1, 2, 3).reader()
        reader.expect(1)
        assertEquals("Expecting '-2' but found '2'", assertFails { reader.expect(-2) }.message)
    }
}
