package com.soywiz.korge.render

import com.soywiz.korag.shader.*

object SDFShaders : Program.Builder() {
    // https://www.ronja-tutorials.com/post/035-2d-sdf-combination/
    // https://iquilezles.org/articles/distfunctions/
    val opInterpolate by FUNC(Float1, Float1, Float1, returns = Float1) { a, b, c ->
        RETURN(mix(a, b, c))
    }

    val opUnion by FUNC(Float1, Float1, returns = Float1) { a, b ->
        RETURN(min(a, b))
    }
    val opIntersect by FUNC(Float1, Float1, returns = Float1) { a, b ->
        RETURN(max(a, b))
    }
    val opSubtract by FUNC(Float1, Float1, returns = Float1) { a, b ->
        RETURN(max(a, -b))
    }
    val opRound by FUNC(Float1, Float1, returns = Float1) { d, r ->
        RETURN(d - r)
    }
    /**
     * From an SDF function, this converts a filled SDF into a stroke (border of the shape)
     */
    val opBorder by FUNC(Float1, Float1, returns = Float1) { d, r ->
        RETURN(abs(d) - r)
    }
    /**
     * From an SDF function, this creates a smooth antialiased version working with pixel distances.
     */
    val opAA by FUNC(Float1, returns = Float1) { d ->
        RETURN(1f - clamp(d / fwidth(d) + 0.5f, 0f.lit, 1f.lit))
    }
    val opSmoothUnion by FUNC(Float1, Float1, Float1, returns = Float1) { d1, d2, k ->
        val h = TEMP(Float1)
        SET(h, clamp(.5f + .5f * (d2 - d1) / k, 0f.lit, 1f.lit))
        RETURN(mix(d2, d1, h) - k * h * (1f - h))
    }
    val opSmoothSubtraction by FUNC(Float1, Float1, Float1, returns = Float1) { d1, d2, k ->
        val h = TEMP(Float1)
        SET(h, clamp(.5f - .5f * (d2 + d1) / k, 0f.lit, 1f.lit))
        RETURN(mix(d2, -d1, h) + k * h * (1f - h))
    }
    val opSmoothIntersection by FUNC(Float1, Float1, Float1, returns = Float1) { d1, d2, k ->
        val h = TEMP(Float1)
        SET(h, clamp(.5f - .5f * (d2 - d1) / k, 0f.lit, 1f.lit))
        RETURN(mix(d2, d1, h) + k * h * (1f - h))
    }

    // https://iquilezles.org/articles/distfunctions2d/
    val circle by FUNC(Float2, Float1, returns = Float1) { p, r ->
        RETURN(length(p) - r)
    }
    val roundedBox by FUNC(Float2, Float2, Float4, returns = Float1) { p, b, r ->
        val q = TEMP(Float2)
        SET(r["xy"], TERNARY(p.x gt 0f, r["xy"], r["zw"]))
        SET(r.x, TERNARY(p.y gt 0f, r.x, r.y))
        SET(q, abs(p) - b + r.x)
        RETURN(min(max(q.x, q.y), 0f.lit) + length(max(q, 0f.lit)) - r.x)
    }
    val box by FUNC(Float2, Float2, returns = Float1) { p, b ->
        val d = TEMP(Float2)
        SET(d, abs(p) - b)
        RETURN(length(max(d, 0f.lit)) + min(max(d.x, d.y), 0f.lit))
    }

    /**
     * Mixes colors, considering they are premultiplied.
     * The first parameter is the existing color, and the second one the color trying to put on top.
     */
    val opCombinePremultipliedColors by FUNC(Float4, Float4, returns = Float4) { dst, src ->
        RETURN(src + dst * (1f - src.a))
    }
}
