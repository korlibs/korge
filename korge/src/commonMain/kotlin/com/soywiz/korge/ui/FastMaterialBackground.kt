package com.soywiz.korge.ui

import com.soywiz.korag.*
import com.soywiz.korag.shader.*
import com.soywiz.korge.render.*
import com.soywiz.korge.view.*
import com.soywiz.korim.color.*
import com.soywiz.korma.geom.*

inline fun Container.fastMaterialBackground(
    width: Double = UI_DEFAULT_WIDTH,
    height: Double = UI_DEFAULT_HEIGHT,
    block: @ViewDslMarker FastMaterialBackground.() -> Unit = {}
): FastMaterialBackground = FastMaterialBackground(width, height).addTo(this).apply(block)

class FastMaterialBackground(
    width: Double = 100.0,
    height: Double = 100.0
) : ShadedView(PROGRAM, width, height, CoordsType.D_W_H) {
    var borderColor: RGBA = Colors.BLACK
    var borderSize: Double = 0.0; set(value) { field = value; invalidateRender() }
    var radius = 0.0 ; set(value) { field = value; invalidateRender() }
    var highlightPos = Point(0.5, 0.5) ; set(value) { field = value; invalidateRender() }
    var highlightRadius = 0.0; set(value) { field = value; invalidateRender() }
    var highlightAlpha = 1.0; set(value) { field = value; invalidateRender() }

    var shadowColor: RGBA = Colors.BLACK.withAd(0.3); set(value) { field = value; invalidateRender() }
    var shadowRadius: Double = 10.0; set(value) { field = value; invalidateRender() }
    var shadowOffsetX: Double = 0.0; set(value) { field = value; invalidateRender() }
    var shadowOffsetY: Double = 0.0; set(value) { field = value; invalidateRender() }

    override fun invalidateRender() {
        super.invalidateRender()
        setPadding(shadowRadius)
    }

    init {
        invalidateRender()
    }

    override fun updateUniforms(uniforms: AG.UniformValues, ctx: RenderContext) {
        //uniforms[u_Radius] = height / 2.0
        uniforms[u_Radius] = radius
        uniforms[u_Size] = Point(width, height)
        uniforms[u_HighlightPos] = Point(highlightPos.x * width, highlightPos.y * height)
        uniforms[u_HighlightRadius] = highlightRadius * kotlin.math.max(width, height) * 1.25
        uniforms[u_highlightAlpha] = highlightAlpha
        uniforms[u_backgroundColor] = shadowColor.premultipliedFast
        uniforms[u_backgroundOffset] = Point(shadowOffsetX, shadowOffsetY)
        uniforms[u_backgroundRadius] = shadowRadius
    }

    // https://www.shadertoy.com/view/WtdSDs
    // https://iquilezles.org/articles/distfunctions2d/#:~:text=length(p)%20%2D%20r%3B%0A%7D-,Rounded%20Box%20%2D%20exact,-(https%3A//www
    companion object {
        val u_backgroundColor = Uniform("u_backgroundColor", VarType.Float4)
        val u_backgroundRadius = Uniform("u_backgroundRadius", VarType.Float1)
        val u_backgroundOffset = Uniform("u_backgroundOffset", VarType.Float2)
        val u_HighlightPos = Uniform("u_HighlightPos", VarType.Float2)
        val u_HighlightRadius = Uniform("u_HighlightRadius", VarType.Float1)
        val u_Size = Uniform("u_Size", VarType.Float2)
        val u_Radius = Uniform("u_Radius", VarType.Float1)
        val u_highlightAlpha = Uniform("u_highlightAlpha", VarType.Float1)
        val PROGRAM = buildShader {
            val roundedBoxSDF = FUNC("roundedBoxSDF", VarType.Float1) {
                val p = ARG("p", VarType.Float2)
                val b = ARG("b", VarType.Float2)
                val r = ARG("r", VarType.Float4)
                SET(r["xy"], TERNARY(p.x gt 0f.lit, r["xy"], r["zw"]))
                SET(r.x, TERNARY(p.y gt 0f.lit, r.x, r.y))
                val q = abs(p) - b + r.x
                RETURN(min(max(q.x, q.y), 0f.lit) + length(max(q, 0f.lit)) - r.x)
            }

            // The pixel space scale of the rectangle.
            val size = u_Size
            // How soft the edges should be (in pixels). Higher values could be used to simulate a drop shadow.
            val edgeSoftness = 1.0f.lit
            // The radius of the corners (in pixels).
            val radius = u_Radius
            // Calculate distance to edge.
            val distance = roundedBoxSDF(v_Tex["xy"] - (size / 2.0f.lit), size / 2.0f.lit, vec4(radius))
            // Smooth the result (free antialiasing).
            val smoothedAlpha = t_Temp0.x
            SET(smoothedAlpha, 1.0f.lit - smoothstep(0.0f.lit, edgeSoftness * 2.0f.lit, distance))

            val highlightAlpha = t_Temp0.y
            SET(highlightAlpha, smoothstep(u_HighlightRadius, u_HighlightRadius - 1f.lit, length(v_Tex - u_HighlightPos)) * u_highlightAlpha)

            // Return the resultant shape.
            val quadColor = (v_Col * smoothedAlpha) + ((vec4(.7f.lit) * highlightAlpha) * smoothedAlpha)
            SET(out, quadColor)

            // Apply a drop shadow effect.
            IF(smoothedAlpha lt 0.05f.lit) {
                val shadowSoftness = u_backgroundRadius
                val shadowOffset = u_backgroundOffset
                val shadowDistance = roundedBoxSDF(v_Tex["xy"] + shadowOffset - (size / 2.0f.lit), size / 2.0f.lit, vec4(radius))
                val shadowAlpha = 1.0f.lit - smoothstep(-shadowSoftness, shadowSoftness, shadowDistance)
                val shadowColor = u_backgroundColor

                SET(out, mix(out, shadowColor, (shadowAlpha - smoothedAlpha)))
            }
        }

        /*
// from https://iquilezles.org/articles/distfunctions
float roundedBoxSDF(vec2 CenterPosition, vec2 Size, float Radius) {
    return length(max(abs(CenterPosition)-Size+Radius,0.0))-Radius;
}
void mainImage( out vec4 fragColor, in vec2 fragCoord ) {
    // The pixel space scale of the rectangle.
    vec2 size = vec2(iResolution.x, iResolution.y);

    // How soft the edges should be (in pixels). Higher values could be used to simulate a drop shadow.
    float edgeSoftness  = 1.0f;

    // The radius of the corners (in pixels).
    float radius = 144.0;

    // Calculate distance to edge.
    float distance 		= roundedBoxSDF(fragCoord.xy - (size/2.0f), size / 2.0f, radius);

    // Smooth the result (free antialiasing).
    float smoothedAlpha =  1.0f-smoothstep(0.0f, edgeSoftness * 2.0f,distance);

    // Return the resultant shape.
    vec4 quadColor		= mix(vec4(1.0f, 1.0f, 1.0f, 1.0f), vec4(0.0f, 0.2f, 1.0f, smoothedAlpha), smoothedAlpha);

    // Apply a drop shadow effect.
    float shadowSoftness = 30.0f;
    vec2 shadowOffset 	 = vec2(0.0f, 10.0f);
    float shadowDistance = roundedBoxSDF(fragCoord.xy + shadowOffset - (size/2.0f), size / 2.0f, radius);
    float shadowAlpha 	 = 1.0f-smoothstep(-shadowSoftness, shadowSoftness, shadowDistance);
    vec4 shadowColor 	 = vec4(0.4f, 0.4f, 0.4f, 1.0f);
    fragColor 			 = mix(quadColor, shadowColor, shadowAlpha - smoothedAlpha);
}
         */
    }
}
