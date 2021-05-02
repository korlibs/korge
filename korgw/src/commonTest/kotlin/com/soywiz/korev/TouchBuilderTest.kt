package com.soywiz.korev

import com.soywiz.korio.util.*
import kotlin.test.*

class TouchBuilderTest {
    val builder = TouchBuilder()

    fun TouchEvent.touchesToStringNice() = touches.joinToString(",") { it.toStringNice() }

    @Test
    fun testIos() {
        builder.frame(TouchBuilder.Mode.IOS, TouchEvent.Type.START) {
            touch(0, 10.0, 10.0)
        }.clone().let { event ->
            assertEquals(TouchEvent.Type.START, event.type)
            assertEquals("Touch[0][ADD](10,10)", event.touchesToStringNice())
        }

        builder.frame(TouchBuilder.Mode.IOS, TouchEvent.Type.START) {
            touch(1, 20.0, 20.0)
        }.clone().let { event ->
            assertEquals(TouchEvent.Type.START, event.type)
            assertEquals("Touch[0][KEEP](10,10),Touch[1][ADD](20,20)", event.touchesToStringNice())
        }

        builder.frame(TouchBuilder.Mode.IOS, TouchEvent.Type.MOVE) {
            touch(0, 15.0, 15.0)
        }.clone().let { event ->
            assertEquals(TouchEvent.Type.MOVE, event.type)
            assertEquals("Touch[0][KEEP](15,15),Touch[1][KEEP](20,20)", event.touchesToStringNice())
        }

        builder.frame(TouchBuilder.Mode.IOS, TouchEvent.Type.END) {
            touch(0, 25.0, 25.0)
        }.clone().let { event ->
            assertEquals(TouchEvent.Type.END, event.type)
            assertEquals("Touch[0][REMOVE](25,25),Touch[1][KEEP](20,20)", event.touchesToStringNice())
        }

        builder.frame(TouchBuilder.Mode.IOS, TouchEvent.Type.MOVE) {
            touch(1, 10.0, 10.0)
        }.clone().let { event ->
            assertEquals(TouchEvent.Type.MOVE, event.type)
            assertEquals("Touch[1][KEEP](10,10)", event.touchesToStringNice())
        }
    }
}
