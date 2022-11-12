package com.soywiz.korge.render

import com.soywiz.korag.shader.*
import com.soywiz.korge.annotations.*
import com.soywiz.korge.view.*
import com.soywiz.korim.color.*
import com.soywiz.korma.geom.*

// https://www.shadertoy.com/view/WtdSDs
// https://iquilezles.org/articles/distfunctions
// https://iquilezles.org/articles/distfunctions2d/#:~:text=length(p)%20%2D%20r%3B%0A%7D-,Rounded%20Box%20%2D%20exact,-(https%3A//www
object MaterialRender {
    val u_ShadowColor = Uniform("u_ShadowColor", VarType.Float4)
    val u_ShadowRadius = Uniform("u_ShadowRadius", VarType.Float1)
    val u_ShadowOffset = Uniform("u_ShadowOffset", VarType.Float2)
    val u_HighlightPos = Uniform("u_HighlightPos", VarType.Float2)
    val u_HighlightRadius = Uniform("u_HighlightRadius", VarType.Float1)
    val u_Size = Uniform("u_Size", VarType.Float2)
    val u_Radius = Uniform("u_Radius", VarType.Float4)
    val u_HighlightAlpha = Uniform("u_HighlightAlpha", VarType.Float1)
    val PROGRAM = ShadedView.buildShader {
        val roundedBoxSDF = FUNC("roundedBoxSDF", VarType.Float1) {
            val p = ARG("p", VarType.Float2)
            val b = ARG("b", VarType.Float2)
            val r = ARG("r", VarType.Float4)
            SET(r["xy"], TERNARY(p.x gt 0f.lit, r["xy"], r["zw"]))
            SET(r["x"], TERNARY(p.y gt 0f.lit, r["x"], r["y"]))
            val q = abs(p) - b + r.x
            RETURN(min(max(q.x, q.y), 0f.lit) + length(max(q, 0f.lit)) - r.x)
        }

        // The pixel space scale of the rectangle.
        val size = u_Size
        // How soft the edges should be (in pixels). Higher values could be used to simulate a drop shadow.
        val edgeSoftness = 1.0f.lit
        // Calculate distance to edge.
        val distance = roundedBoxSDF(v_Tex["xy"] - (size / 2.0f.lit), size / 2.0f.lit, u_Radius)
        // Smooth the result (free antialiasing).
        val smoothedAlpha = t_Temp0.x
        SET(smoothedAlpha, 1.0f.lit - smoothstep(0.0f.lit, edgeSoftness * 2.0f.lit, distance))

        val highlightAlpha = t_Temp0.y
        SET(highlightAlpha, smoothstep(u_HighlightRadius, u_HighlightRadius - 1f.lit, length(v_Tex - u_HighlightPos)) * u_HighlightAlpha)

        SET(out, v_Col * smoothedAlpha)

        // Return the resultant shape.
        SET(out, out + (vec4(.7f.lit) * highlightAlpha * smoothedAlpha))

        // Apply a drop shadow effect.
        IF(smoothedAlpha lt 0.05f.lit) {
            val shadowSoftness = u_ShadowRadius
            val shadowOffset = u_ShadowOffset
            val shadowDistance = roundedBoxSDF(v_Tex["xy"] + shadowOffset - (size / 2.0f.lit), size / 2.0f.lit, u_Radius)
            val shadowAlpha = 1.0f.lit - smoothstep(-shadowSoftness, shadowSoftness, shadowDistance)
            val shadowColor = u_ShadowColor

            SET(out, mix(out, shadowColor, (shadowAlpha - smoothedAlpha)))
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
