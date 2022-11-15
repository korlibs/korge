package com.soywiz.korge.render

import com.soywiz.kds.iterators.*
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
    val u_Size by Uniform(VarType.Float2)
    val u_Radius by Uniform(VarType.Float4)
    val u_HighlightAlpha by Uniform(VarType.Float1)

    val PROGRAM = ShadedView.buildShader {
        // The pixel space scale of the rectangle.
        val size = u_Size
        // Calculate distance to edge.
        val distance = SDFShaders.roundedBox(v_Tex["xy"] - (size / 2.0f.lit), size / 2.0f.lit, u_Radius)
        // Smooth the result (free antialiasing).
        val smoothedAlpha = t_Temp0.x
        //SET(smoothedAlpha, 1.0f.lit - smoothstep(0.0f.lit, edgeSoftness * 2.0f.lit, distance))
        SET(smoothedAlpha, SDFShaders.computeAAAlphaFromDist(distance))

        val highlightAlpha = t_Temp0.y
        SET(highlightAlpha, smoothstep(u_HighlightRadius, u_HighlightRadius - 1f.lit, length(v_Tex - u_HighlightPos)) * u_HighlightAlpha)

        SET(out, v_Col * smoothedAlpha)

        // Return the resultant shape.
        SET(out, out + (vec4(.7f.lit) * highlightAlpha * smoothedAlpha))

        // Apply a drop shadow effect.
        IF((distance ge (-.1f).lit) and (u_ShadowRadius gt 0f.lit)) {
        //IF((smoothedAlpha lt .1f.lit) and (u_ShadowRadius gt 0f.lit)) {
            val shadowSoftness = u_ShadowRadius
            val shadowOffset = u_ShadowOffset
            val shadowDistance = SDFShaders.roundedBox(v_Tex["xy"] + shadowOffset - (size / 2.0f.lit), size / 2.0f.lit, u_Radius)
            val shadowAlpha = 1.0f.lit - smoothstep(-shadowSoftness, shadowSoftness, shadowDistance)

            SET(out, SDFShaders.mixPremultipliedColor(u_ShadowColor * shadowAlpha, out))
            //SET(out, mix(out, shadowColor, (shadowAlpha + smoothedAlpha)))
        }
    }
}

@KorgeExperimental
fun RenderContext2D.materialRoundRect(
    x: Double,
    y: Double,
    width: Double,
    height: Double,
    color: RGBA = this.multiplyColor,
    //color: RGBA = Colors.RED,
    radius: RectCorners = RectCorners.EMPTY,
    shadowOffset: IPoint = IPoint.ZERO,
    shadowColor: RGBA = Colors.BLACK,
    shadowRadius: Double = 0.0,
    highlightPos: IPoint = IPoint.ZERO,
    highlightRadius: Double = 0.0,
    highlightAlpha: Double = 1.0,
) {
    keepColor {
        this.multiplyColor = color
        _tempProgramUniforms.clear()
        _tempProgramUniforms[MaterialRender.u_Radius] = floatArrayOf(
            radius.bottomRight.toFloat(), radius.topRight.toFloat(),
            radius.bottomLeft.toFloat(), radius.topLeft.toFloat(),
        )
        _tempProgramUniforms[MaterialRender.u_Size] = Point(width, height)
        _tempProgramUniforms[MaterialRender.u_HighlightPos] = Point(highlightPos.x * width, highlightPos.y * height)
        _tempProgramUniforms[MaterialRender.u_HighlightRadius] = highlightRadius * kotlin.math.max(width, height) * 1.25
        _tempProgramUniforms[MaterialRender.u_HighlightAlpha] = highlightAlpha
        _tempProgramUniforms[MaterialRender.u_ShadowColor] = shadowColor.premultipliedFast
        _tempProgramUniforms[MaterialRender.u_ShadowOffset] = Point(shadowOffset.x, shadowOffset.y)
        _tempProgramUniforms[MaterialRender.u_ShadowRadius] = shadowRadius
        quadPaddedCustomProgram(x, y, width, height, MaterialRender.PROGRAM, _tempProgramUniforms, Margin(shadowRadius + shadowOffset.length))
    }
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
) {
    val placements = text.place(Rectangle(x, y, width, height), wordWrap, includePartialLines, ellipsis, fill, stroke, align)
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
    for (char in text) {
        val glyph = font.getGlyph(char)
        rect(
            sx + glyph.xoffset * scale,
            sy + glyph.yoffset * scale,
            glyph.texWidth.toDouble() * scale,
            glyph.texHeight.toDouble() * scale,
            color, true, glyph.texture, font.agProgram,
        )
        sx += glyph.xadvance * scale
    }
}
