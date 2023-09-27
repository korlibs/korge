package korlibs.korge.input

import korlibs.event.*
import korlibs.io.lang.*
import korlibs.korge.tests.*
import korlibs.korge.time.*
import korlibs.korge.view.*
import korlibs.math.geom.*
import korlibs.time.*
import kotlin.test.*

class KeysEventsTest : ViewsForTesting() {
    @Test
    fun testDownUp() = viewsTest {
        val log = arrayListOf<String>()
        solidRect(100, 100).keys {
            justDown(Key.SPACE) { log.add("justDown") }
            down(Key.SPACE) { log.add("down") }
            up(Key.SPACE) { log.add("up") }
        }
        keyDown(Key.SPACE)
        keyDown(Key.SPACE)
        keyDown(Key.SPACE)
        keyUp(Key.SPACE)
        assertEquals(listOf("down", "justDown", "down", "down", "up"), log)
    }

    @Test
    fun testDownRepeating() = viewsTest {
        var calledTimes = 0
        solidRect(100, 100).keys {
            downRepeating(Key.SPACE, maxDelay = 500.milliseconds, minDelay = 100.milliseconds, delaySteps = 4) { calledTimes++ }
        }
        assertEquals(0, calledTimes)
        keyDown(Key.SPACE)
        assertEquals(1, calledTimes)

        for ((index, delay) in listOf(500, 400, 300, 200, 100, 100).withIndex()) {
            delay(delay.milliseconds)
            assertEquals(index + 2, calledTimes)
        }
        assertEquals(7, calledTimes)

        keyUp(Key.SPACE)
        delay(1000.milliseconds)
        assertEquals(7, calledTimes)
        calledTimes = 0

        keyDown(Key.SPACE)
        delay(3000.milliseconds)
        assertEquals(21, calledTimes)
    }

    @Test
    fun testDownRepeatingMultiple() {
        val views = ViewsForTesting().views
        var calledTimes = LinkedHashMap<Key, Int>()
        var pos = Point.ZERO
        val view = views.stage.solidRect(100, 100).apply {
            keys {
                downFrame(Key.LEFT, Key.RIGHT, Key.UP, Key.DOWN, dt = 100.milliseconds) {
                    calledTimes.getOrPut(it.key) { 0 }
                    calledTimes[it.key] = calledTimes[it.key]!! + 1
                    when (it.key) {
                        Key.LEFT -> pos += Vector2D(-1, 0)
                        Key.RIGHT -> pos += Vector2D(+1, 0)
                        Key.UP -> pos += Vector2D(-1, 0)
                        Key.DOWN -> pos += Vector2D(+1, 0)
                        else -> unreachable
                    }
                }
            }
        }
        fun keyDown(key: Key) {
            views.dispatch(KeyEvent(KeyEvent.Type.DOWN, key = key))
        }
        fun keyUp(key: Key) {
            views.dispatch(KeyEvent(KeyEvent.Type.UP, key = key))
        }
        fun delay(time: TimeSpan) {
            views.dispatch(UpdateEvent(time))
        }

        assertEquals(0, calledTimes[Key.RIGHT] ?: 0)
        keyDown(Key.RIGHT)
        delay(100.milliseconds)
        assertEquals(1, calledTimes[Key.RIGHT])

        for ((index, delay) in listOf(100, 100, 100).withIndex()) {
            delay(delay.milliseconds)
            assertEquals(index + 2, calledTimes[Key.RIGHT])
        }
        assertEquals(4, calledTimes[Key.RIGHT])

        keyUp(Key.RIGHT)
        keyDown(Key.LEFT)
        delay(1000.milliseconds)
        assertEquals(4, calledTimes[Key.RIGHT])
        assertEquals(10, calledTimes[Key.LEFT])
        calledTimes.clear()

        keyDown(Key.UP)
        delay(1000.milliseconds)
        assertEquals(10, calledTimes[Key.UP])
    }
}
