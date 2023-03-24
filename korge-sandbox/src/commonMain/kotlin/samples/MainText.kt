package samples

import korlibs.time.*
import korlibs.korge.scene.*
import korlibs.korge.view.*
import korlibs.image.bitmap.effect.*
import korlibs.image.color.*
import korlibs.image.font.*
import korlibs.image.paint.*
import korlibs.image.text.*
import korlibs.io.lang.*
import korlibs.math.geom.*


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
