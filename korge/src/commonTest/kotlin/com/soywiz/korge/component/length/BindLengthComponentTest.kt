package com.soywiz.korge.component.length

import com.soywiz.korag.log.LogBaseAG
import com.soywiz.korge.render.testRenderContext
import com.soywiz.korge.tests.ViewsForTesting
import com.soywiz.korge.view.FixedSizeContainer
import com.soywiz.korge.view.fixedSizeContainer
import com.soywiz.korge.view.solidRect
import com.soywiz.korio.async.delay
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class BindLengthComponentTest : ViewsForTesting(log = true) {
    @Test
    fun testPercent() = viewsTest {
        val container = fixedSizeContainer(300.0, 500.0)
        val rect = container.solidRect(100, 100)
        container.bindLength(rect::x) { 50.percent }
        container.bindLength(rect::y) { 50.percent }
        assertEquals(0.0, rect.x)
        assertEquals(0.0, rect.y)
        delayFrame()
        assertTrue { logAg!!.getLogAsString().contains("a_Pos[vec2(150.0,350.0)]") }
        assertEquals(150.0, rect.x)
        assertEquals(250.0, rect.y)
    }
}
