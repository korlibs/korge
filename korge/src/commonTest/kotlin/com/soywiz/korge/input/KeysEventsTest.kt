package com.soywiz.korge.input

import com.soywiz.klock.*
import com.soywiz.korev.*
import com.soywiz.korge.tests.*
import com.soywiz.korge.time.*
import com.soywiz.korge.view.*
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
}
