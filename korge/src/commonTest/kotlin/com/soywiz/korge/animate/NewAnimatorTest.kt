package com.soywiz.korge.animate

import com.soywiz.klock.*
import com.soywiz.korge.view.*
import com.soywiz.korio.util.*
import com.soywiz.korma.geom.*
import com.soywiz.korma.interpolation.*
import kotlin.test.*

class NewAnimatorTest {
    @Test
    fun testBasic() {
        val view = DummyView()
        val animator = view.animator(defaultTime = 1.seconds, defaultEasing = Easing.LINEAR) {
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

    @Test
    fun testSequences() {
        val view = DummyView()
        var log = ""
        val animator = view.animator(defaultTime = 1.seconds, defaultEasing = Easing.LINEAR) {
            block(name = "0") { log += "0" }
            block { log += "1" }
            sequence {
                moveTo(view, 10, 0)
                //moveBy(view, y = +10)
                moveTo(view, 0, 10)
            }
            block { log += "2" }
            sequence {
                moveBy(view, x = -20)
            }
            block { log += "3" }
        }
        val lines = arrayListOf<String>()
        fun logLine() {
            lines += "${view.pos.niceStr}, ${view.alpha.niceStr(1)} : $log"
        }
        logLine()
        for (n in 0 until 34) {
            view.updateSingleView(0.1.seconds)
            logLine()
        }
        assertEquals(
            """
                (0, 0), 1 : 
                (1, 0), 1 : 01
                (2, 0), 1 : 01
                (3, 0), 1 : 01
                (4, 0), 1 : 01
                (5, 0), 1 : 01
                (6, 0), 1 : 01
                (7, 0), 1 : 01
                (8, 0), 1 : 01
                (9, 0), 1 : 01
                (10, 0), 1 : 01
                (9, 1), 1 : 01
                (8, 2), 1 : 01
                (7, 3), 1 : 01
                (6, 4), 1 : 01
                (5, 5), 1 : 01
                (4, 6), 1 : 01
                (3, 7), 1 : 01
                (2, 8), 1 : 01
                (1, 9), 1 : 01
                (0, 10), 1 : 01
                (-2, 10), 1 : 012
                (-4, 10), 1 : 012
                (-6, 10), 1 : 012
                (-8, 10), 1 : 012
                (-10, 10), 1 : 012
                (-12, 10), 1 : 012
                (-14, 10), 1 : 012
                (-16, 10), 1 : 012
                (-18, 10), 1 : 012
                (-20, 10), 1 : 012
                (-20, 10), 1 : 0123
                (-20, 10), 1 : 0123
                (-20, 10), 1 : 0123
                (-20, 10), 1 : 0123
            """.trimIndent(),
            lines.joinToString("\n")
        )
    }

    @Test
    fun testParallel() {
        val view = DummyView()
        val animator = view.animator(defaultTime = 1.seconds, defaultEasing = Easing.LINEAR, parallel = true) {
            moveTo(view, 10, 0)
            alpha(view, 0.0, time = 1.2.seconds)
        }
        val lines = arrayListOf<String>()
        fun logLine() {
            lines += "${view.pos.niceStr}, ${view.alpha.niceStr(1)}"
        }
        for (n in 0 until 12) {
            view.updateSingleView(0.1.seconds)
            logLine()
        }
        assertEquals(
            """
                (1, 0), 0.9
                (2, 0), 0.8
                (3, 0), 0.8
                (4, 0), 0.7
                (5, 0), 0.6
                (6, 0), 0.5
                (7, 0), 0.4
                (8, 0), 0.3
                (9, 0), 0.2
                (10, 0), 0.2
                (10, 0), 0.1
                (10, 0), 0
            """.trimIndent(),
            lines.joinToString("\n")
        )
    }

    @Test
    fun testComplex() {
        val executorView = DummyView()
        val view1 = DummyView()
        val view2 = DummyView()
        val log = arrayListOf<String>()
        val animator = executorView.animator(defaultEasing = Easing.LINEAR) {
            sequence(defaultTime = 1.seconds, defaultSpeed = 100.0) {
                //wait(0.25.seconds)
                block {
                    log += "0"
                }
                parallel {
                    //rect1.moveTo(0, 150)
                    moveToWithSpeed(view1, 200.0, 0.0)
                    moveToWithSpeed(view2, 0.0, 200.0)
                    //rect1.moveTo(0, height - 100)
                }
                block {
                    log += "1"
                }
                parallel {
                    //rect1.moveTo(0, 150)
                    moveTo(view1, 200.0, 200.0)
                    moveTo(view2, 200.0, 200.0)
                    //rect1.moveTo(0, height - 100)
                }
                block {
                    log += "2"
                }
                parallel(time = 1.seconds) {
                    hide(view1)
                    hide(view2)
                }
                block {
                    log += "3"
                }
                block {
                    //printStackTrace()
                    //println("ZERO")
                    view1.position(0, 0)
                    view2.position(0, 0)
                }
                block {
                    log += "4"
                }
                parallel(time = 0.5.seconds) {
                    show(view1)
                    show(view2)
                }
                block {
                    log += "5"
                }
                wait(0.5.seconds)
                block {
                    log += "6"
                }
            }
        }
        animator.onComplete.add { log += "complete" }
        val lines = arrayListOf<String>()
        fun logLine() {
            lines += "view1[${view1.pos.niceStr}, ${view1.alpha}], view2[${view2.pos.niceStr}, ${view2.alpha}], log=${log.joinToString("")}"
        }
        for (n in 0 until 24) {
            executorView.updateSingleView(0.25.seconds)
            logLine()
        }
        assertEquals(
            """
                view1[(25, 0), 1.0], view2[(0, 25), 1.0], log=0
                view1[(50, 0), 1.0], view2[(0, 50), 1.0], log=0
                view1[(75, 0), 1.0], view2[(0, 75), 1.0], log=0
                view1[(100, 0), 1.0], view2[(0, 100), 1.0], log=0
                view1[(125, 0), 1.0], view2[(0, 125), 1.0], log=0
                view1[(150, 0), 1.0], view2[(0, 150), 1.0], log=0
                view1[(175, 0), 1.0], view2[(0, 175), 1.0], log=0
                view1[(200, 0), 1.0], view2[(0, 200), 1.0], log=0
                view1[(200, 50), 1.0], view2[(50, 200), 1.0], log=01
                view1[(200, 100), 1.0], view2[(100, 200), 1.0], log=01
                view1[(200, 150), 1.0], view2[(150, 200), 1.0], log=01
                view1[(200, 200), 1.0], view2[(200, 200), 1.0], log=01
                view1[(200, 200), 0.75], view2[(200, 200), 0.75], log=012
                view1[(200, 200), 0.5], view2[(200, 200), 0.5], log=012
                view1[(200, 200), 0.25], view2[(200, 200), 0.25], log=012
                view1[(200, 200), 0.0], view2[(200, 200), 0.0], log=012
                view1[(0, 0), 0.5], view2[(0, 0), 0.5], log=01234
                view1[(0, 0), 1.0], view2[(0, 0), 1.0], log=01234
                view1[(0, 0), 1.0], view2[(0, 0), 1.0], log=012345
                view1[(0, 0), 1.0], view2[(0, 0), 1.0], log=012345
                view1[(0, 0), 1.0], view2[(0, 0), 1.0], log=0123456complete
                view1[(0, 0), 1.0], view2[(0, 0), 1.0], log=0123456complete
                view1[(0, 0), 1.0], view2[(0, 0), 1.0], log=0123456complete
                view1[(0, 0), 1.0], view2[(0, 0), 1.0], log=0123456complete
            """.trimIndent(),
            lines.joinToString("\n")
        )
    }
}

