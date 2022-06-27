package samples

import com.soywiz.korev.SoftKeyboardReturnKeyType
import com.soywiz.korev.SoftKeyboardType
import com.soywiz.korge.scene.Scene
import com.soywiz.korge.ui.DefaultUIBitmapFont
import com.soywiz.korge.ui.uiTextInput
import com.soywiz.korge.view.Container
import com.soywiz.korge.view.aroundPath
import com.soywiz.korge.view.text
import com.soywiz.korge.view.xy
import com.soywiz.korim.color.Colors
import com.soywiz.korim.font.DefaultTtfFont
import com.soywiz.korim.font.readTtfFont
import com.soywiz.korim.font.withFallback
import com.soywiz.korim.text.DefaultStringTextRenderer
import com.soywiz.korim.text.aroundPath
import com.soywiz.korio.file.std.resourcesVfs
import com.soywiz.korma.geom.shape.buildVectorPath
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

        text("HELLO WORLD", 64.0, font = font).xy(600, 150).aroundPath(buildVectorPath { circle(50, 50, 100) }).also {
            it.font = DefaultUIBitmapFont
            it.color = Colors.RED
        }
    }
}
