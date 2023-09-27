package korlibs.io.lang

import kotlin.test.*

class CancellableTest {
    @Test
    fun testCancellableGroup() {
        val log = arrayListOf<String>()
        val cancellable1 = Cancellable { log += "1" }
        val cancellable2 = Cancellable { log += "2" }
        CancellableGroup(cancellable1, cancellable2).let { group ->
            assertEquals("", log.joinToString(","))
            group.cancel()
            assertEquals("1,2", log.joinToString(","))
            log.clear()
        }
        CancellableGroup(listOf(cancellable1, cancellable2)).let { group ->
            assertEquals("", log.joinToString(","))
            group.cancel()
            assertEquals("1,2", log.joinToString(","))
            log.clear()
        }
        CancellableGroup().let { group ->
            group.addCancellable(cancellable1)
            group.addCancellable(cancellable2)
            assertEquals("", log.joinToString(","))
            group.cancel()
            assertEquals("1,2", log.joinToString(","))
            log.clear()
        }
    }
}
