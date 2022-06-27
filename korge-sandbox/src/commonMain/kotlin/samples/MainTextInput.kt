package samples

import com.soywiz.klock.seconds
import com.soywiz.korev.SoftKeyboardReturnKeyType
import com.soywiz.korev.SoftKeyboardType
import com.soywiz.korge.scene.Scene
import com.soywiz.korge.time.timeout
import com.soywiz.korge.ui.DefaultUIBitmapFont
import com.soywiz.korge.ui.uiTextInput
import com.soywiz.korge.view.Container
import com.soywiz.korge.view.aroundPath
import com.soywiz.korge.view.circle
import com.soywiz.korge.view.graphics
import com.soywiz.korge.view.roundRect
import com.soywiz.korge.view.text
import com.soywiz.korge.view.vector.gpuGraphics
import com.soywiz.korge.view.vector.gpuShapeView
import com.soywiz.korge.view.xy
import com.soywiz.korim.color.Colors
import com.soywiz.korim.font.DefaultTtfFont
import com.soywiz.korim.font.readTtfFont
import com.soywiz.korim.font.withFallback
import com.soywiz.korim.text.DefaultStringTextRenderer
import com.soywiz.korim.text.DefaultTextRenderer
import com.soywiz.korim.text.aroundPath
import com.soywiz.korim.text.text
import com.soywiz.korim.text.withSpacing
import com.soywiz.korio.file.std.resourcesVfs
import com.soywiz.korma.geom.shape.buildVectorPath
import com.soywiz.korma.geom.vector.Winding
import com.soywiz.korma.geom.vector.circle

class MainTextInput : Scene() {
    override suspend fun Container.sceneMain() {
        //val bitmap = NativeImage(512, 512, premultiplied = true).context2d {
        //    fill(Colors.RED) {
        //        rect(0, 0, 512, 512)
        //    }
        //}
        //val atlas = MutableAtlasUnit()
        //atlas.add(bitmap.toBMP32IfRequired(), Unit)
        //sceneContainer.changeTo({ MainSWF().apply { init(it) } })
        //image(atlas.bitmap)

        val emojiFont = resourcesVfs["noto-glyf_colr_1.ttf"].readTtfFont(preload = false)
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

        //text("HELLO WORLD", textSize = 64.0, renderer = com.soywiz.korim.text.DefaultStringTextRenderer.aroundPath(buildVectorPath { circle(0, 0, 100) }), font = DefaultTtfFont).xy(700, 200)
        /*
        text("HELLO WORLD", 64.0, font = font).xy(600, 150).aroundPath(buildVectorPath { circle(50, 50, 100) }).also {
            it.font = DefaultUIBitmapFont
            it.color = Colors.RED
        }

         */
    }
}
