package korlibs.korge.storage

import korlibs.korge.service.storage.*
import korlibs.korge.tests.*
import kotlin.test.*

class NativeStorageTest : ViewsForTesting() {
    @Test
    fun test() = viewsTest {
        views.storage.removeAll()
        val demo = views.storage.itemInt("hello")
        assertEquals(false, demo.isDefined)
        assertEquals(0, demo.value)
        //assertFailsWith<Throwable> { demo.value }
        demo.value = 10
        assertEquals(true, demo.isDefined)
        assertEquals(10, demo.value)
    }
}