package com.soywiz.korge.component

import com.soywiz.korge.tests.ViewsForTesting
import com.soywiz.korge.view.SolidRect
import kotlin.test.Test
import kotlin.test.assertEquals

class StageComponentTest : ViewsForTesting() {
    @Test
    fun testOnAttachDetach() = viewsTest {
        val log = arrayListOf<String>()

        val rect = SolidRect(100, 100)
            .onAttachDetach(views,
                onAttach = { log += "attach" },
                onDetach = { log += "detach" },
            )

        delayFrame()
        assertEquals("", log.joinToString(","))
        stage.addChild(rect)
        delayFrame()
        assertEquals("attach", log.joinToString(","))
        stage.removeChild(rect)
        delayFrame()
        assertEquals("attach,detach", log.joinToString(","))
    }
}
