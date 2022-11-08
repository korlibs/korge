package com.soywiz.korge.animate

import com.soywiz.klock.*
import com.soywiz.korge.view.*
import com.soywiz.korma.geom.*
import com.soywiz.korma.interpolation.*
import kotlin.test.*

class NewAnimatorTest {
    @Test
    fun testBasic() {
        val view = DummyView()
        val animator = view.newAnimator(time = 1.seconds, easing = Easing.LINEAR) {
            moveTo(view, 10, 0)
        }
        val log = arrayListOf<String>()
        animator.onComplete.add { log += "complete" }
        view.updateSingleView(0.milliseconds)
        assertEquals("(0, 0)", view.pos.niceStr)
        view.updateSingleView(100.milliseconds)
        assertEquals("(1, 0)", view.pos.niceStr)
        assertEquals("", log.joinToString(","))
        view.updateSingleView(900.milliseconds)
        assertEquals("(10, 0)", view.pos.niceStr)
        assertEquals("complete", log.joinToString(","))

        // Add a new node to the animator (even if completed)
        animator.moveBy(view, 0, 10)
        view.updateSingleView(100.milliseconds)
        assertEquals("(10, 1)", view.pos.niceStr)
        assertEquals("complete", log.joinToString(","))
        view.updateSingleView(900.milliseconds)
        assertEquals("(10, 10)", view.pos.niceStr)
        assertEquals("complete,complete", log.joinToString(","))
    }
}

