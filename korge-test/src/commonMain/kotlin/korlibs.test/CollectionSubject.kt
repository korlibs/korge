package korlibs.test

import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CollectionSubject<T : Any>(subject: Collection<T>) : AnySubject<Collection<T>>(subject) {
    fun isEmpty() {
        assertTrue(subject.isEmpty())
    }
    fun isNotEmpty() {
        assertTrue(subject.isNotEmpty())
    }
    fun hasSize(expectedSize: Int) {
        assertEquals(expectedSize, subject.size)
    }
    fun containsExactlyUnordered(vararg elements: T) {
        val actualSet = subject.toSet()
        val expectedSet = elements.toSet()
        assertEquals(expectedSet.size, actualSet.size)
        for (expected in expectedSet) {
            assertTrue(actualSet.contains(expected))
        }
    }
}
