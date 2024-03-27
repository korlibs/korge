package korlibs.korge.ui

import korlibs.korge.annotations.*
import korlibs.korge.testing.*
import korlibs.korge.text.*
import korlibs.math.geom.*
import kotlin.test.*

class UITextInputTestJvm {
    @OptIn(KorgeExperimental::class)
    @Test
    fun emptyTextInputRendersCaret() = korgeScreenshotTestV2 {
        val input = uiTextInput(
            initialText = "",
            size = Size(20.0, 24.0),
            settings = TextInputSettings(caretBlinkingDuration = null)
        ) { }
        input.focus()
        it.recordGolden(input, "emptyTextInputRendersCaret")
        it.endTest()
    }
}
