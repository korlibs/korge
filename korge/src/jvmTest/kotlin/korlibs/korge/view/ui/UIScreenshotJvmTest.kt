package korlibs.korge.view.ui

import korlibs.korge.testing.*
import korlibs.korge.ui.*
import korlibs.korge.view.*
import korlibs.math.geom.*
import org.junit.*

class UIScreenshotJvmTest {
    @Test
    fun test() = korgeScreenshotTest(Size(128, 128)) {
        uiCheckBox(checked = false, text = "").xy(0, 0)
        uiCheckBox(checked = true, text = "").xy(32, 0)
        uiCheckBox(checked = true, text = "hi").xy(0, 32)
        assertScreenshot()
    }
}
