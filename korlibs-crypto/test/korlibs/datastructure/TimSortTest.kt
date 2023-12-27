package korlibs.datastructure

import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals

class TimSortTest {
    @Test
    fun test() {
        val pairs = listOf(1, 2, 3, 4, 5, 11, 15, 76, 192, 1024, 2048, 3000).map { N ->
        //val pairs = listOf(1, 2, 3, 4).map { N ->
            val items = (0 until N).toList()
            val shuffledItems = items.shuffled(Random(0))
            items to shuffledItems
        }

        for ((items, shuffledItems) in pairs) {
            assertEquals(items, shuffledItems.timSorted())
            //assertEquals(items, shuffledItems.genericSorted())
        }
    }
}
