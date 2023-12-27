package korlibs.datastructure

import kotlin.test.Test
import kotlin.test.assertEquals

class BinarySearchTest {
    @Test
    fun test() {
        val v = intArrayOf(10, 20, 30, 40, 50)
        assertEquals(0, v.binarySearch(10).index)
        assertEquals(1, v.binarySearch(20).index)
        assertEquals(2, v.binarySearch(30).index)
        assertEquals(3, v.binarySearch(40).index)
        assertEquals(4, v.binarySearch(50).index)

        assertEquals(true, v.binarySearch(10).found)
        assertEquals(false, v.binarySearch(11).found)

        assertEquals(0, v.binarySearch(0).nearIndex)
        assertEquals(0, v.binarySearch(9).nearIndex)
        assertEquals(1, v.binarySearch(11).nearIndex)
        assertEquals(2, v.binarySearch(21).nearIndex)
        assertEquals(3, v.binarySearch(31).nearIndex)
        assertEquals(4, v.binarySearch(41).nearIndex)
        assertEquals(5, v.binarySearch(51).nearIndex)
        assertEquals(5, v.binarySearch(100).nearIndex)

        for (nonItem in listOf(-1, 0, 9, 11, 19, 21, 31, 41, 51)) {
            assertEquals(-1, v.binarySearch(nonItem).index)
        }
    }

    @Test
    fun testLeft() {
        intArrayOf(10, 20, 30, 40, 50).also { v ->
            assertEquals(0, v.binarySearchLeft(5))
            assertEquals(0, v.binarySearchLeft(10))
            assertEquals(0, v.binarySearchLeft(15))
            assertEquals(1, v.binarySearchLeft(20))
            assertEquals(1, v.binarySearchLeft(25))
            assertEquals(2, v.binarySearchLeft(30))
            assertEquals(2, v.binarySearchLeft(35))
            assertEquals(3, v.binarySearchLeft(40))
            assertEquals(3, v.binarySearchLeft(45))
            assertEquals(4, v.binarySearchLeft(50))
            assertEquals(4, v.binarySearchLeft(55))
        }
        intArrayOf(10, 20, 30, 40).also { v ->
            assertEquals(0, v.binarySearchLeft(5))
            assertEquals(0, v.binarySearchLeft(10))
            assertEquals(0, v.binarySearchLeft(15))
            assertEquals(1, v.binarySearchLeft(20))
            assertEquals(1, v.binarySearchLeft(25))
            assertEquals(2, v.binarySearchLeft(30))
            assertEquals(2, v.binarySearchLeft(35))
            assertEquals(3, v.binarySearchLeft(40))
            assertEquals(3, v.binarySearchLeft(45))
        }
    }

    @Test
    fun testRight() {
        intArrayOf(10, 20, 30, 40, 50).also { v ->
            assertEquals(0, v.binarySearchRight(5))
            assertEquals(0, v.binarySearchRight(10))
            assertEquals(1, v.binarySearchRight(15))
            assertEquals(1, v.binarySearchRight(20))
            assertEquals(2, v.binarySearchRight(25))
            assertEquals(2, v.binarySearchRight(30))
            assertEquals(3, v.binarySearchRight(35))
            assertEquals(3, v.binarySearchRight(40))
            assertEquals(4, v.binarySearchRight(45))
            assertEquals(4, v.binarySearchRight(50))
            assertEquals(4, v.binarySearchRight(55))
        }
        intArrayOf(10, 20, 30, 40).also { v ->
            assertEquals(0, v.binarySearchRight(5))
            assertEquals(0, v.binarySearchRight(10))
            assertEquals(1, v.binarySearchRight(15))
            assertEquals(1, v.binarySearchRight(20))
            assertEquals(2, v.binarySearchRight(25))
            assertEquals(2, v.binarySearchRight(30))
            assertEquals(3, v.binarySearchRight(35))
            assertEquals(3, v.binarySearchRight(40))
            assertEquals(3, v.binarySearchRight(45))
        }
    }
}
