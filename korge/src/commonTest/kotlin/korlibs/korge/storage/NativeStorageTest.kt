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

    @Test
    fun testProperty() {
        val views = ViewsForTesting().views
        views.storage.removeAll()
        assertEquals("{}", "${views.storage.toMap()}")
        var demo by views.storage.itemInt("hello", -1)
        assertEquals(-1, demo)
        //assertFailsWith<Throwable> { demo.value }
        demo = 10
        assertEquals(10, demo)
        assertEquals("{hello=10}", "${views.storage.toMap()}")
        views.storage.removeAll()
        assertEquals(-1, demo)
        assertEquals("{}", "${views.storage.toMap()}")
    }
}
