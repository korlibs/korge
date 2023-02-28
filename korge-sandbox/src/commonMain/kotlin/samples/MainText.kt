package samples

import com.soywiz.klock.*
import com.soywiz.korge.scene.*
import com.soywiz.korge.view.*
import com.soywiz.korim.bitmap.effect.*
import com.soywiz.korim.color.*
import com.soywiz.korim.font.*
import com.soywiz.korim.paint.*
import com.soywiz.korim.text.*
import com.soywiz.korio.lang.*
import com.soywiz.korma.geom.*


class MainText : ScaledScene(512, 160) {
    override suspend fun SContainer.sceneMain() {
        val font = BitmapFont(
            DefaultTtfFont, 64.0,
            paint = LinearGradientPaint(0, 0, 0, 50).add(0.0, Colors.CADETBLUE).add(1.0, Colors.PURPLE),
            effect = BitmapEffect(
                dropShadowX = 2,
                dropShadowY = 2,
                dropShadowRadius = 2,
                dropShadowColor = Colors["#5f005f"]
            )
        )

        var offset = 0.degrees
        var version = 0
        //text("Hello World!", font = font, textSize = 64.0, alignment = TextAlignment.BASELINE_LEFT, renderer = CreateStringTextRenderer({ version++ }) { reader: WStringReader, c: Int, g: GlyphMetrics, advance: Double ->
        val text = text("Hello World!", font = font, textSize = 64.0, alignment = TextAlignment.BASELINE_LEFT, renderer = CreateStringTextRenderer({ version++ }) { reader: WStringReader, c: Int, g: GlyphMetrics, advance: Double ->
            transform.identity()

            val sin = sind(offset + (reader.position * 360 / reader.length).degrees)
            transform.rotate(15.degrees)
            transform.translate(0.0, sin * 16)
            transform.scale(1.0, 1.0 + sin * 0.1)
            put(reader, c)
            advance(advance)
        }).position(100, 100)
        addFixedUpdater(60.timesPerSecond) {
            offset += 10.degrees
            text.invalidate()
        }
    }
}
