package korlibs.datastructure

import korlibs.time.*
import kotlin.test.*
import kotlin.random.*

class RadixSortTest {
    @Test
    fun testStringArray() {
        val array1: Array<String> = arrayOf("abc", "aaa", "bca", "acc", "bbb", "cad", "caa", "dddd", "aaaa", "AAA", "BBB")
        val original: Array<String> = array1.copyOf()
        val items: Array<String> = array1.sortedRadix(transform = { it.lowercaseChar() })
        val items2: Array<String> = array1.sortedArrayWith { a, b -> a.compareTo(b, ignoreCase = true) } as Array<String>
        assertContentEquals(original, array1)
        assertContentEquals(items, items2)
    }

    @Test
    fun testInts() {
        val random = Random(-1L)
        val ints = IntArray(100_000) { random.nextInt() and 0x7FFFFFFF }
        //repeat(10) { println(measureTime { ints.sortedArrayRadix() }) }
        //repeat(10) { println(measureTime { ints.sortedArray() }) }
        assertContentEquals(
            ints.sortedArrayRadix(),
            ints.sortedArray()
        )
    }
}
