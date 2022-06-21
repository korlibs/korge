package samples

import com.soywiz.korge.scene.Scene
import com.soywiz.korge.view.Container
import com.soywiz.korge.view.text
import com.soywiz.korge.view.xy
import com.soywiz.korim.font.DefaultTtfFont
import com.soywiz.korim.font.SystemFont
import com.soywiz.korim.font.TtfFont
import com.soywiz.korim.font.readTtfFont
import com.soywiz.korim.font.withFallback
import com.soywiz.korio.file.std.resourcesVfs

class MainEmojiColrv1 : Scene() {
    override suspend fun Container.sceneMain() {
        val font = resourcesVfs["twemoji-glyf_colr_1.ttf"].readTtfFont(preload = false)
        val font2 = resourcesVfs["noto-glyf_colr_1.ttf"].readTtfFont(preload = false)
        val font3 = SystemFont.getEmojiFont()

        println("font=$font, font2=$font2, font3=$font3")

        //val font = DefaultTtfFont.withFallback()
        val str = "HELLO! 😀😁🤤👨‍🦳👨🏻‍🦳👨🏻‍🦳👩🏽‍🦳⛴🔮🤹‍♀️😇🥹🍦💩🥜🥝🌄🏞"
        text(str, font = font, textSize = 50.0).xy(64, 100)
        text(str, font = font2, textSize = 50.0).xy(64, 200)
        text(str, font = font3, textSize = 50.0).xy(64, 300)
        //text("👨‍🦳", font = font, textSize = 64.0).xy(64, 100)
        //text("\uD83D\uDC68\u200D", font = font, textSize = 64.0).xy(64, 100)
        //text("HELLO! 😀😁🤤", font = font, textSize = 64.0).xy(64, 100)
        //text("HELLO! 😀\uD83D\uDE01\uD83E\uDD24a", font = font, textSize = 64.0).xy(64, 100)
        //text("😀a", font = font, textSize = 64.0).xy(64, 100)
        //text("😀", font = font, textSize = 64.0).xy(64, 100)
        //text("HELLO! \uD83D\uDE00", font = font, textSize = 64.0).xy(50, 100)
    }
}
