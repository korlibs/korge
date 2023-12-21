package korlibs.korge.view

import korlibs.image.color.*
import korlibs.image.font.*
import korlibs.image.text.*
import korlibs.io.file.std.*
import korlibs.korge.testing.*
import korlibs.math.geom.*
import org.junit.*

class TextBlockScreenshotTest {
    @Test
    fun testTextBlock() = korgeScreenshotTest(
        Size(64, 32),
        bgcolor = Colors.BLUE
    ) {
        textBlock(RichTextData(
            "Hello",
            font = resourcesVfs["font/m5x7_16_outline.fnt"].readBitmapFont(),
            color = Colors.RED
        )).xy(2, 0).also { it.smoothing = false }
        textBlock(RichTextData(
            "World",
            font = resourcesVfs["font/m5x7_16_outline.fnt"].readBitmapFont(),
            color = Colors.GREEN,
        )).xy(2, 16).also { it.smoothing = false }

        textBlock(RichTextData(
            "Test",
            font = resourcesVfs["font/m5x7_16_outline.fnt"].readBitmapFont(),
            textSize = 18.0,
            color = Colors.YELLOW
        )).xy(34, 0).also { it.smoothing = true }
        textBlock(RichTextData(
            "Demo",
            font = resourcesVfs["font/m5x7_16_outline.fnt"].readBitmapFont(),
            textSize = 18.0,
            color = Colors.PURPLE
        )).xy(34, 16).also { it.smoothing = false }

        assertScreenshot(this, "textBlock", posterize = 6, psnr = 25.0)
    }
}
