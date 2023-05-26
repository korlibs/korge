package korlibs.datastructure

import kotlin.test.Test
import kotlin.test.assertEquals

class FastStringMapTest {
    @Test
    fun testFastKeyForEach() {
        val map = FastStringMap<Int>()
        map["hello"] = 10
        map["world"] = 20
        val out = arrayListOf<String>()
        map.fastKeyForEach { out.add(it) }
        assertEquals(listOf("hello", "world"), out.sorted())
    }
}
