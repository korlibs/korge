package com.soywiz.korge.view

import com.soywiz.korge.scene.*
import com.soywiz.korge.testing.*
import com.soywiz.korge.view.filter.*
import com.soywiz.korim.color.*
import kotlin.test.*

class TransitionViewTest {
    @Test
    fun test() = korgeScreenshotTest(50, 50) {
        val tv = TransitionView()
        tv.startNewTransition(DummyView())
        tv.startNewTransition(Container().apply {
            clipContainer(50, 50) {
                solidRect(50, 50, Colors.BLUE)
            }
        }, MaskTransition(TransitionFilter.Transition.CIRCULAR))
        tv.ratio = 0.5
        addChild(tv)

        assertScreenshot(posterize = 5)
        tv.ratio = 0.9
        assertScreenshot(posterize = 5)
        tv.ratio = 1.0
        assertScreenshot(posterize = 5)
    }

}
