package korlibs.korge.storage

import korlibs.korge.service.storage.*
import korlibs.korge.tests.*
import kotlin.test.*

class NativeStorageTest {
    @Test
    fun test() {
        val views = ViewsForTesting().views
        println("views.storage.toMap()=${views.storage.toMap()}")
        views.storage.removeAll()
        val demo = views.storage.itemInt("hello")
        assertEquals(false, demo.isDefined)
        assertEquals(0, demo.value)
        //assertFailsWith<Throwable> { demo.value }
        demo.value = 10
        assertEquals(true, demo.isDefined)
        assertEquals(10, demo.value)
        println("views.storage.toMap()=${views.storage.toMap()}")
        views.storage.removeAll()
        assertEquals(false, demo.isDefined)
        assertEquals(0, demo.value)
        println("views.storage.toMap()=${views.storage.toMap()}")
    }
}
