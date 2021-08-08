package com.soywiz.korge.view

import com.soywiz.korge.scene.*
import com.soywiz.korge.test.*
import com.soywiz.korge.tests.*
import com.soywiz.korge.view.filter.*
import com.soywiz.korim.color.*
import com.soywiz.korma.geom.*
import kotlin.test.*

class TransitionViewTest : ViewsForTesting(log = true, windowSize = SizeInt(800, 600), virtualSize = SizeInt(512, 512)) {
    @Test
    fun test() = viewsTest {
        val tv = TransitionView()
        tv.startNewTransition(DummyView())
        tv.startNewTransition(Container().apply {
            clipContainer(512, 512) {
                solidRect(512, 512, Colors.BLUE)
            }
        }, MaskTransition(TransitionFilter.Transition.CIRCULAR))
        tv.ratio = 0.5
        addChild(tv)

        delayFrame()

        assertEqualsFileReference("korge/view/ref/TransitionTest.log", logAg.getLogAsString())
    }

}
