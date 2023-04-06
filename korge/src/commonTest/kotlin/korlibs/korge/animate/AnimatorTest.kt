package korlibs.korge.animate

import korlibs.time.milliseconds
import korlibs.time.seconds
import korlibs.memory.toIntRound
import korlibs.korge.tests.ViewsForTesting
import korlibs.korge.tween._interpolateAngle
import korlibs.korge.tween.denormalized
import korlibs.korge.tween.get
import korlibs.korge.tween.tween
import korlibs.korge.view.*
import korlibs.image.color.Colors
import korlibs.math.geom.degrees
import korlibs.math.interpolation.*
import kotlin.test.Test
import kotlin.test.assertEquals

class AnimatorTest : ViewsForTesting() {
    @Test
    fun test() = viewsTest {
        val view = solidRect(100, 100, Colors.RED)
        val log = arrayListOf<String>()
        animate(completeOnCancel = false) {
            moveTo(view, 100, 0)
            moveBy(view, y = +100.0)
            block { log += "${view.pos}" }
            moveBy(view, x = +10.0)
            moveTo(view, x = { view.xD + 10 })
        }
        assertEquals("(120, 100)", view.pos.toString())
        assertEquals("[(100, 100)]", log.toString())
    }

    @Test
    fun testInterpolateAngle() = viewsTest {
        //        0 360 -360
        //  -90 /+--+\
        // 270 |     | 90 -270
        //      \+--+/
        //        180
        //        -180

        assertEquals(202.5.degrees, _interpolateAngle(0.25.toRatio(), 180.degrees, (-90).degrees))
        assertEquals(0.degrees, _interpolateAngle(0.5.toRatio(), 350.degrees, (10).degrees))
        assertEquals(0.degrees, _interpolateAngle(0.5.toRatio(), 10.degrees, (350).degrees))
    }

    @Test
    fun testTweenAngle() = viewsTest(frameTime = 100.milliseconds) {
        val view = solidRect(10, 10, Colors.RED)
        val log = arrayListOf<Int>()
        tween(view::rotation[350.0.degrees, 10.0.degrees], time = 1.seconds, easing = Easing.LINEAR) {
            log += view.rotation.degrees.toIntRound()
        }
        assertEquals("350,352,354,356,358,0,2,4,6,8,10", log.joinToString(","))
    }

    @Test
    fun testTweenAngleDenormalized() = viewsTest(frameTime = 100.milliseconds) {
        val view = solidRect(10, 10, Colors.RED)
        val log = arrayListOf<Int>()
        tween(view::rotation[350.0.degrees, 10.0.degrees].denormalized(), time = 1.seconds, easing = Easing.LINEAR) {
            log += view.rotation.degrees.toIntRound()
        }
        assertEquals("350,316,282,248,214,180,146,112,78,44,10", log.joinToString(","))
    }

    @Test
    fun testBlockLooped() {
        val rlog = arrayListOf<String>()
        val log = arrayListOf<String>()
        fun log() { rlog += log.joinToString(",") }
        val view = DummyView()
        view.animator(parallel = true) {
            sequence(looped = true) {
                block { log += "a" }
                wait(0.5.seconds)
                block { log += "b" }
                wait(0.5.seconds)
            }
        }
        view.updateSingleView(0.seconds)
        log()
        repeat(6) {
            view.updateSingleView(0.5.seconds)
            log()
        }
        assertEquals(
            """
                a
                a
                a,b
                a,b,a
                a,b,a,b
                a,b,a,b,a
                a,b,a,b,a,b
            """.trimIndent(),
            rlog.joinToString("\n")
        )
    }

    @Test
    fun testBlockNotLooped() {
        val rlog = arrayListOf<String>()
        val log = arrayListOf<String>()
        fun log() { rlog += log.joinToString(",") }
        val view = DummyView()
        view.animator(parallel = true) {
            sequence(looped = false) {
                block { log += "a" }
                wait(0.5.seconds)
                block { log += "b" }
                wait(0.5.seconds)
            }
        }
        view.updateSingleView(0.seconds)
        log()
        repeat(6) {
            view.updateSingleView(0.5.seconds)
            log()
        }
        assertEquals(
            """
                a
                a
                a,b
                a,b
                a,b
                a,b
                a,b
            """.trimIndent(),
            rlog.joinToString("\n")
        )
    }
}
