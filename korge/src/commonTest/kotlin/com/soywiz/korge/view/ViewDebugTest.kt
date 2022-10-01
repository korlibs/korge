package com.soywiz.korge.view

import com.soywiz.korge.debug.uiEditableValue
import kotlin.test.Test

class ViewDebugTest {
    var myprop = 10.0

    @Test
    fun test() {
        val view = DummyView()
        view.addDebugExtraComponent("Debug") {
            uiEditableValue(::myprop)
        }
    }
}
