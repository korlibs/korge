package korlibs.korge.render

import korlibs.datastructure.iterators.*
import korlibs.graphics.shader.*
import korlibs.korge.annotations.*
import korlibs.korge.view.*
import korlibs.image.color.*
import korlibs.image.font.*
import korlibs.image.paint.*
import korlibs.image.text.*
import korlibs.math.geom.*

// https://www.shadertoy.com/view/WtdSDs
// https://iquilezles.org/articles/distfunctions
// https://iquilezles.org/articles/distfunctions2d/#:~:text=length(p)%20%2D%20r%3B%0A%7D-,Rounded%20Box%20%2D%20exact,-(https%3A//www
object MaterialRender {
    object MaterialBlockUB : UniformBlock(fixedLocation = 3) {
        val u_ShadowColor by vec4()
        val u_ShadowRadius by float()
        val u_ShadowOffset by vec2()

        val u_HighlightPos by vec2()
        val u_HighlightRadius by float()
        val u_HighlightColor by vec4()

        val u_Size by vec2()
        val u_Radius by vec4()

        val u_BorderSizeHalf by float()
        val u_BorderColor by vec4()
        val u_BackgroundColor by vec4()
    }
    val u_ShadowColor = MaterialBlockUB.u_ShadowColor
    val u_ShadowRadius = MaterialBlockUB.u_ShadowRadius
    val u_ShadowOffset = MaterialBlockUB.u_ShadowOffset

    val u_HighlightPos = MaterialBlockUB.u_HighlightPos
    val u_HighlightRadius = MaterialBlockUB.u_HighlightRadius
    val u_HighlightColor = MaterialBlockUB.u_HighlightColor

    val u_Size = MaterialBlockUB.u_Size
    val u_Radius = MaterialBlockUB.u_Radius

    val u_BorderSizeHalf = MaterialBlockUB.u_BorderSizeHalf
    val u_BorderColor = MaterialBlockUB.u_BorderColor
    val u_BackgroundColor = MaterialBlockUB.u_BackgroundColor

    //val ub_MaterialBlock = MaterialBlockUB.uniformBlock

    init {
        //println("ub_MaterialBlock.uniforms.voffset=${MaterialBlockUB.uniforms.map { it.name }}")
        //println("ub_MaterialBlock.uniforms.voffset=${MaterialBlockUB.uniforms.map { it.voffset }}")
        //println("ub_MaterialBlock.uniforms.voffset=${ub_MaterialBlock.uniforms.map { it.name }}")
        //println("ub_MaterialBlock.totalSize=${MaterialBlockUB.totalSize}")
        //println("ub_MaterialBlock.totalSize=${ub_MaterialBlock.totalSize}")
    }

    val PROGRAM = ShadedView.buildShader {
        val roundedDist = TEMP(Float1)
        val borderDist = TEMP(Float1)
        val highlightDist = TEMP(Float1)
        val borderAlpha = TEMP(Float1)
        val highlightAlpha = TEMP(Float1)

        // The pixel space scale of the rectangle.
        val size = u_Size

        SET(roundedDist, SDFShaders.roundedBox(v_Tex - (size / 2f), size / 2f, u_Radius))
        SET(out, u_BackgroundColor * SDFShaders.opAA(roundedDist))

        // Render circle highlight
        IF(u_HighlightRadius gt 0f) {
            SET(highlightDist, SDFShaders.opIntersect(roundedDist, SDFShaders.circle(v_Tex - u_HighlightPos, u_HighlightRadius)))
            SET(highlightAlpha, SDFShaders.opAA(highlightDist))
            IF(highlightAlpha gt 0f) {
                SET(out, SDFShaders.opCombinePremultipliedColors(out, u_HighlightColor * highlightAlpha))
            }
        }

        // Render border
        IF(u_BorderSizeHalf gt 0f) {
            SET(borderDist, SDFShaders.opBorderInner(roundedDist, u_BorderSizeHalf * 2f))
            //SET(borderDist, SDFShaders.opBorder(roundedDist, u_BorderSizeHalf))
            SET(borderAlpha, SDFShaders.opAA(borderDist))
            IF(borderAlpha gt 0f) {
                SET(out, SDFShaders.opCombinePremultipliedColors(out, u_BorderColor * borderAlpha))
            }
        }

        // Apply a drop shadow effect.
        //IF((roundedDist ge -.1f) and (u_ShadowRadius gt 0f)) {
        IF((out.a lt 1f) and (u_ShadowRadius gt 0f)) {
        //IF((smoothedAlpha lt .1f.lit) and (u_ShadowRadius gt 0f.lit)) {
            val shadowSoftness = u_ShadowRadius
            val shadowOffset = u_ShadowOffset
            val shadowDistance = SDFShaders.roundedBox(v_Tex["xy"] + shadowOffset - (size / 2f), size / 2f, u_Radius)
            val shadowAlpha = 1f - smoothstep(-shadowSoftness, shadowSoftness, shadowDistance)

            SET(out, SDFShaders.opCombinePremultipliedColors(u_ShadowColor * shadowAlpha, out))
            //SET(out, mix(out, shadowColor, (shadowAlpha + smoothedAlpha)))
        }

        SET(out, out * v_Col)
    }
}

@KorgeExperimental
fun RenderContext2D.materialRoundRect(
    x: Double,
    y: Double,
    width: Double,
    height: Double,
    color: RGBA = Colors.RED,
    radius: RectCorners = RectCorners.EMPTY,
    shadowOffset: Point = Point.ZERO,
    shadowColor: RGBA = Colors.BLACK,
    shadowRadius: Double = 0.0,
    highlightPos: Point = Point.ZERO,
    highlightRadius: Double = 0.0,
    highlightColor: RGBA = Colors.WHITE,
    borderSize: Double = 0.0,
    borderColor: RGBA = Colors.WHITE,
    //colorMul: RGBA = Colors.WHITE,
) {
    ctx[MaterialRender.MaterialBlockUB].push(deduplicate = true) {
        it[u_Radius] = radius
        it[u_Size] = Size(width, height)
        it[u_BackgroundColor] = (color.premultipliedFast)
        it[u_HighlightPos] = (highlightPos * Size(width, height))
        it[u_HighlightRadius] = (highlightRadius * kotlin.math.max(width, height) * 1.25).toFloat()
        it[u_HighlightColor] = (highlightColor.premultipliedFast)
        it[u_BorderSizeHalf] = (borderSize * 0.5f).toFloat()
        it[u_BorderColor] = (borderColor.premultipliedFast)
        it[u_ShadowColor] = (shadowColor.premultipliedFast)
        it[u_ShadowOffset] = (shadowOffset)
        it[u_ShadowRadius] = (shadowRadius).toFloat()
    }
    //ctx[MaterialRender.ub_MaterialBlock].push(deduplicate = true) {
    //    it[MaterialRender.u_Radius].set(radius)
    //    it[MaterialRender.u_Size].set(Size(width, height))
    //    it[MaterialRender.u_BackgroundColor].set(color.premultipliedFast)
    //    it[MaterialRender.u_HighlightPos].set(highlightPos * Size(width, height))
    //    it[MaterialRender.u_HighlightRadius].set(highlightRadius * kotlin.math.max(width, height) * 1.25)
    //    it[MaterialRender.u_HighlightColor].set(highlightColor.premultipliedFast)
    //    it[MaterialRender.u_BorderSizeHalf].set(borderSize * 0.5)
    //    it[MaterialRender.u_BorderColor].set(borderColor.premultipliedFast)
    //    it[MaterialRender.u_ShadowColor].set(shadowColor.premultipliedFast)
    //    it[MaterialRender.u_ShadowOffset].set(shadowOffset)
    //    it[MaterialRender.u_ShadowRadius].set(shadowRadius)
    //}
    quadPaddedCustomProgram(x, y, width, height, MaterialRender.PROGRAM, Margin((shadowRadius + shadowOffset.length).toFloat()))
}

@KorgeExperimental
fun RenderContext2D.drawText(
    text: RichTextData,
    x: Double = 0.0,
    y: Double = 0.0,
    width: Double = 10000.0,
    height: Double = 10000.0,
    wordWrap: Boolean = true,
    includePartialLines: Boolean = false,
    ellipsis: String? = null,
    fill: Paint? = null,
    stroke: Stroke? = null,
    align: TextAlignment = TextAlignment.TOP_LEFT,
    includeFirstLineAlways: Boolean = true
) {
    drawText(text.place(MRectangle(x, y, width, height), wordWrap, includePartialLines, ellipsis, fill, stroke, align, includeFirstLineAlways = includeFirstLineAlways))
}

@KorgeExperimental
fun RenderContext2D.drawText(
    placements: RichTextDataPlacements,
    textRangeStart: Int = 0,
    textRangeEnd: Int = Int.MAX_VALUE,
) {
    var n = 0
    placements.fastForEach {
        drawText(
            it.text, it.font.lazyBitmap, it.size, it.x, it.y, (it.fillStyle as? RGBA?) ?: Colors.WHITE, baseline = true,
            textRangeStart = textRangeStart - n,
            textRangeEnd = textRangeEnd - n
        )
        n += it.text.length
    }
}

@KorgeExperimental
fun RenderContext2D.drawText(
    text: String,
    font: BitmapFont,
    textSize: Double = 16.0,
    x: Double = 0.0,
    y: Double = 0.0,
    color: RGBA = Colors.WHITE,
    baseline: Boolean = false,
    textRangeStart: Int = 0,
    textRangeEnd: Int = Int.MAX_VALUE,
    //stroke: Stroke?,
) {
    val scale = font.getTextScale(textSize)
    var sx = x
    val sy = y + if (baseline) -font.base * scale else 0.0
    //println("multiplyColor=$multiplyColor")
    var n = 0
    for (char in text) {
        val glyph = font.getGlyph(char)
        if (n in textRangeStart until textRangeEnd) {
            rect(
                sx + glyph.xoffset * scale,
                sy + glyph.yoffset * scale,
                glyph.texWidth.toDouble() * scale,
                glyph.texHeight.toDouble() * scale,
                (color * multiplyColor),
                //multiplyColor,
                true, glyph.texture, font.agProgram,
            )
        }
        sx += glyph.xadvance * scale
        n++
    }
}