package korlibs.korge.ui

import korlibs.korge.tests.ViewsForTesting
import kotlin.test.Test
import kotlin.test.assertEquals

class UIButtonToggleableGroupTest : ViewsForTesting() {
    @Test
    fun test() = viewsTest {
        val buttons = arrayListOf<UIButton>()
        uiHorizontalStack {
            val group = UIButtonToggleableGroup()
            buttons.add(uiButton().group(group, pressed = false))
            buttons.add(uiButton().group(group, pressed = true))
        }
        assertEquals("false,true", buttons.joinToString(",") { "${it.forcePressed}" })
        buttons[0].simulateClick()
        assertEquals("true,false", buttons.joinToString(",") { "${it.forcePressed}" })
        buttons[0].simulateClick()
        assertEquals("true,false", buttons.joinToString(",") { "${it.forcePressed}" })
        buttons[1].simulateClick()
        assertEquals("false,true", buttons.joinToString(",") { "${it.forcePressed}" })
    }
}
