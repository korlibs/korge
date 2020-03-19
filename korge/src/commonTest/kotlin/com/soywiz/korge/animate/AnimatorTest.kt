package com.soywiz.korge.animate

import com.soywiz.korge.tests.ViewsForTesting
import com.soywiz.korge.view.solidRect
import com.soywiz.korim.color.Colors
import kotlin.test.Test
import kotlin.test.assertEquals

class AnimatorTest : ViewsForTesting()  {
    @Test
    fun test() = viewsTest {
        val view = solidRect(100, 100, Colors.RED)
        animate(completeOnCancel = false) {
            view.moveTo(100, 0)
            view.moveBy(y = +100.0)
            view.moveBy(x = +10.0)
            view.moveTo(x = { view.x + 10 })
        }
        assertEquals("(120, 100)", view.pos.toString())
    }
}
