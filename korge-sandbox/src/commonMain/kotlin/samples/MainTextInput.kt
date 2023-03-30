package samples

import korlibs.event.*
import korlibs.korge.scene.*
import korlibs.korge.text.*
import korlibs.korge.ui.*
import korlibs.korge.view.*
import korlibs.korge.view.filter.*
import korlibs.image.color.*
import korlibs.image.font.*
import korlibs.io.file.std.*
import korlibs.math.geom.*
import korlibs.math.geom.shape.*

class MainTextInput : Scene() {
    override suspend fun SContainer.sceneMain() {
        //val bitmap = NativeImage(512, 512, premultiplied = true).context2d {
        //    fill(Colors.RED) {
        //        rect(0, 0, 512, 512)
        //    }
        //}
        //val atlas = MutableAtlasUnit()
        //atlas.add(bitmap.toBMP32IfRequired(), Unit)
        //sceneContainer.changeTo({ MainSWF().apply { init(it) } })
        //image(atlas.bitmap)

        val emojiFont = resourcesVfs["noto-glyf_colr_1.ttf"].readTtfFont()
        val font = DefaultTtfFont.withFallback(emojiFont)

        uiTextInput("HELLO", width = 256.0, height = 64.0) {
            this.textSize = 40.0
            this.font = font
            this.softKeyboardReturnKeyType = SoftKeyboardReturnKeyType.NEXT
        }.xy(200, 100)

        uiTextInput("1234", width = 256.0, height = 64.0) {
            this.textSize = 40.0
            this.font = font
            this.softKeyboardType = SoftKeyboardType.NUMBER_PAD
            this.softKeyboardReturnKeyType = SoftKeyboardReturnKeyType.EMERGENCY_CALL
        }.xy(200, 200)

        uiTextInput("test@gmail.com", width = 256.0, height = 64.0) {
            this.textSize = 40.0
            this.font = font
            this.softKeyboardType = SoftKeyboardType.EMAIL_ADDRESS
        }.xy(200, 300)

        val textPath = buildVectorPath { circle(Point(0, 0), 100f) }

        text(
            "HELLO WORLD",
            textSize = 64.0,
            font = font,
            color = Colors.RED,
        ).xy(600, 200).textSpacing(9.0).aroundPath(textPath).filters(DropshadowFilter()).also { it.editText() }

        text(
            "HELLO WORLD",
            textSize = 64.0,
            font = font,
            color = Colors.RED,
        ).xy(600, 500)
            .textSpacing(9.0)
            .aroundPath(buildVectorPath { moveTo(Point(0.0, 0.0)); quadTo(Point(250.0, -100.0), Point(500.0, 0.0)) })
            .filters(DropshadowFilter())
            .also { it.editText() }

        /*
        gpuGraphics {
        //graphics { it.useNativeRendering = false
            //gpuGraphics({
            //this.fill(Colors.RED, winding = Winding.NON_ZERO) {
            //this.fill(Colors.RED, winding = Winding.EVEN_ODD) {
            this.fill(Colors.RED) {
                this.text(
                    "HELLO WORLD",
                    textSize = 64.0,
                    x = 600.0, y = 200.0,
                    renderer = DefaultStringTextRenderer
                        .withSpacing(9.0)
                        .aroundPath(buildVectorPath { circle(0, 0, 100) }),
                    font = DefaultTtfFont
                )
            }
        }

         */

        //text("HELLO WORLD", textSize = 64.0, renderer = korlibs.image.text.DefaultStringTextRenderer.aroundPath(buildVectorPath { circle(0, 0, 100) }), font = DefaultTtfFont).xy(700, 200)
        /*
        text("HELLO WORLD", 64.0, font = font).xy(600, 150).aroundPath(buildVectorPath { circle(50, 50, 100) }).also {
            it.font = DefaultUIBitmapFont
            it.color = Colors.RED
        }

         */
    }
}
