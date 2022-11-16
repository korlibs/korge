package com.soywiz.korge.render

import com.soywiz.kds.iterators.*
import com.soywiz.korag.*
import com.soywiz.korag.shader.*
import com.soywiz.korge.annotations.*
import com.soywiz.korge.view.*
import com.soywiz.korim.color.*
import com.soywiz.korim.font.*
import com.soywiz.korim.paint.*
import com.soywiz.korim.text.*
import com.soywiz.korma.geom.*

// https://www.shadertoy.com/view/WtdSDs
// https://iquilezles.org/articles/distfunctions
// https://iquilezles.org/articles/distfunctions2d/#:~:text=length(p)%20%2D%20r%3B%0A%7D-,Rounded%20Box%20%2D%20exact,-(https%3A//www
object MaterialRender {
    val u_ShadowColor by Uniform(VarType.Float4)
    val u_ShadowRadius by Uniform(VarType.Float1)
    val u_ShadowOffset by Uniform(VarType.Float2)

    val u_HighlightPos by Uniform(VarType.Float2)
    val u_HighlightRadius by Uniform(VarType.Float1)
    val u_HighlightColor by Uniform(VarType.Float4)

    val u_Size by Uniform(VarType.Float2)
    val u_Radius by Uniform(VarType.Float4)

    val u_BorderSizeHalf by Uniform(VarType.Float1)
    val u_BorderColor by Uniform(VarType.Float4)
    val u_BackgroundColor by Uniform(VarType.Float4)

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
            SET(borderDist, SDFShaders.opBorder(roundedDist, u_BorderSizeHalf))
            SET(borderAlpha, SDFShaders.opAA(borderDist))
            IF(borderAlpha gt 0f) {
                SET(out, SDFShaders.opCombinePremultipliedColors(out, u_BorderColor * borderAlpha))
            }
        }

        // Apply a drop shadow effect.
        //IF((roundedDist ge -.1f) and (u_ShadowRadius gt 0f)) {
        IF((out.a lt 1f) and (u_ShadowRadius ge 0f)) {
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
    shadowOffset: IPoint = IPoint.ZERO,
    shadowColor: RGBA = Colors.BLACK,
    shadowRadius: Double = -1.0,
    highlightPos: IPoint = IPoint.ZERO,
    highlightRadius: Double = 0.0,
    highlightColor: RGBA = Colors.WHITE,
    borderSize: Double = 0.0,
    borderColor: RGBA = Colors.WHITE,
    //colorMul: RGBA = Colors.WHITE,
) {
    _tempProgramUniforms.clear()
    _tempProgramUniforms[MaterialRender.u_Radius] = floatArrayOf(
        radius.bottomRight.toFloat(), radius.topRight.toFloat(),
        radius.bottomLeft.toFloat(), radius.topLeft.toFloat(),
    )
    _tempProgramUniforms[MaterialRender.u_Size] = Point(width, height)

    _tempProgramUniforms[MaterialRender.u_BackgroundColor] = color.premultipliedFast

    _tempProgramUniforms[MaterialRender.u_HighlightPos] = Point(highlightPos.x * width, highlightPos.y * height)
    _tempProgramUniforms[MaterialRender.u_HighlightRadius] = highlightRadius * kotlin.math.max(width, height) * 1.25
    _tempProgramUniforms[MaterialRender.u_HighlightColor] = highlightColor.premultipliedFast

    _tempProgramUniforms[MaterialRender.u_BorderSizeHalf] = borderSize * 0.5
    _tempProgramUniforms[MaterialRender.u_BorderColor] = borderColor.premultipliedFast

    _tempProgramUniforms[MaterialRender.u_ShadowColor] = shadowColor.premultipliedFast
    _tempProgramUniforms[MaterialRender.u_ShadowOffset] = Point(shadowOffset.x, shadowOffset.y)
    _tempProgramUniforms[MaterialRender.u_ShadowRadius] = shadowRadius

    quadPaddedCustomProgram(x, y, width, height, MaterialRender.PROGRAM, _tempProgramUniforms, Margin(shadowRadius + shadowOffset.length))
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
    val placements = text.place(Rectangle(x, y, width, height), wordWrap, includePartialLines, ellipsis, fill, stroke, align, includeFirstLineAlways = includeFirstLineAlways)
    placements.fastForEach { it ->
        val bmpFont = it.font as? BitmapFont?
        if (bmpFont != null) {
            drawText(it.text, bmpFont, it.size, it.x, it.y, (it.fillStyle as? RGBA?) ?: Colors.WHITE, baseline = true)
        }
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
    baseline: Boolean = false
    //stroke: Stroke?,
) {
    val scale = font.getTextScale(textSize)
    var sx = x
    val sy = y + if (baseline) -font.base * scale else 0.0
    //println("multiplyColor=$multiplyColor")
    for (char in text) {
        val glyph = font.getGlyph(char)
        rect(
            sx + glyph.xoffset * scale,
            sy + glyph.yoffset * scale,
            glyph.texWidth.toDouble() * scale,
            glyph.texHeight.toDouble() * scale,
            (color * multiplyColor),
            //multiplyColor,
            true, glyph.texture, font.agProgram,
        )
        sx += glyph.xadvance * scale
    }
}
