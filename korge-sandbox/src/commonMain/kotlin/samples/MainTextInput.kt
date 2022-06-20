package samples

import com.soywiz.korge.scene.Scene
import com.soywiz.korge.ui.uiTextInput
import com.soywiz.korge.view.Container
import com.soywiz.korge.view.image
import com.soywiz.korge.view.xy
import com.soywiz.korim.atlas.MutableAtlasUnit
import com.soywiz.korim.bitmap.NativeImage
import com.soywiz.korim.bitmap.context2d
import com.soywiz.korim.color.Colors
import com.soywiz.korim.font.DefaultTtfFont
import com.soywiz.korim.font.readTtfFont
import com.soywiz.korim.font.withFallback
import com.soywiz.korio.file.std.resourcesVfs
import com.soywiz.korma.geom.vector.rect

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
        }.xy(200, 100)
        uiTextInput("WORLD", width = 256.0, height = 64.0) {
            this.textSize = 40.0
            this.font = font
        }.xy(200, 200)
    }
}
