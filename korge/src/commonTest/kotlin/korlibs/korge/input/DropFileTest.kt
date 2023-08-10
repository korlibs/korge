@file:OptIn(ExperimentalStdlibApi::class)

package korlibs.korge.input

import korlibs.event.*
import korlibs.korge.tests.*
import korlibs.korge.view.*
import kotlin.test.*

class DropFileTest : ViewsForTesting() {
    @Test
    fun test() = viewsTest {
        val log = arrayListOf<String>()
        val view = container()
        val closeable = view.onDropFile {
            log += ":${it.type}"
        }
        views.dispatch(DropFileEvent(DropFileEvent.Type.START, listOf()))
        view.dispatch(DropFileEvent(DropFileEvent.Type.START, listOf()))
        gameWindow.dispatch(DropFileEvent(DropFileEvent.Type.START, listOf()))
        assertEquals(":START,:START,:START", log.joinToString(","))
        closeable.close()
        view.dispatch(DropFileEvent(DropFileEvent.Type.START, listOf()))
        assertEquals(":START,:START,:START", log.joinToString(","))
    }
}
