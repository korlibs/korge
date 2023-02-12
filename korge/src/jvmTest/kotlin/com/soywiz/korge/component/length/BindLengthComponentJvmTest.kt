package com.soywiz.korge.component.length

import com.soywiz.korge.testing.*
import com.soywiz.korge.time.*
import com.soywiz.korge.view.*
import com.soywiz.korim.color.*
import kotlin.test.*

class BindLengthComponentJvmTest {
    @Test
    fun testPercent() = korgeScreenshotTest(512, 512) {
        val container = fixedSizeContainer(300.0, 500.0)
        container.solidRect(container.width, container.height, Colors.BLUE)
        val rect = container.solidRect(100, 100)
        rect.bindLength(View::x) { 50.percent }
        rect.bindLength(View::y) { 50.percent }
        assertEquals(0.0, rect.x)
        assertEquals(0.0, rect.y)
        assertScreenshot()
        delayFrame()
        assertScreenshot()
        assertEquals(150.0, rect.x)
        assertEquals(250.0, rect.y)
    }
}
