package korlibs.korge.bitmapfont

import korlibs.korge.render.*
import korlibs.korge.view.*
import korlibs.image.color.*
import korlibs.image.font.*
import korlibs.math.geom.*

fun BitmapFont.drawText(
    ctx: RenderContext,
    textSize: Double,
    str: String,
    x: Int,
    y: Int,
    m: Matrix = Matrix.IDENTITY,
    colMul: RGBA = Colors.WHITE,
    blendMode: BlendMode = BlendMode.INHERIT,
    filtering: Boolean = true
) {
	val scale = textSize / fontSize.toDouble()
    val m2 = m
	    .pretranslated(x.toDouble(), y.toDouble())
	    .prescaled(scale, scale)
	var dx = 0.0
	var dy = 0.0
    ctx.useBatcher { batch ->
        for (n in str.indices) {
            val c1 = str[n].toInt()
            if (c1 == '\n'.toInt()) {
                dx = 0.0
                dy += fontSize
                continue
            }
            val c2 = str.getOrElse(n + 1) { ' ' }.toInt()
            val glyph = this[c1]
            val tex = glyph.texture
            batch.drawQuad(
                ctx.getTex(tex),
                (dx + glyph.xoffset).toFloat(),
                (dy + glyph.yoffset).toFloat(),
                tex.width.toFloat(),
                tex.height.toFloat(),
                m = m2.immutable,
                colorMul = colMul,
                blendMode = blendMode,
                filtering = filtering,
            )
            val kerningOffset = kernings[BitmapFont.Kerning.buildKey(c1, c2)]?.amount ?: 0
            dx += glyph.xadvance + kerningOffset
        }
    }
}

fun RenderContext.drawText(
    font: BitmapFont,
    textSize: Double,
    str: String,
    x: Int,
    y: Int,
    m: Matrix = Matrix.IDENTITY,
    colMul: RGBA = Colors.WHITE,
    blendMode: BlendMode = BlendMode.INHERIT,
    filtering: Boolean = true
) {
	font.drawText(this, textSize, str, x, y, m, colMul, blendMode, filtering)
}
