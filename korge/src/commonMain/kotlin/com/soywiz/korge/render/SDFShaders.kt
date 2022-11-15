package com.soywiz.korge.render

import com.soywiz.korag.shader.*

object SDFShaders : Program.Builder() {
    // https://www.ronja-tutorials.com/post/035-2d-sdf-combination/
    // https://iquilezles.org/articles/distfunctions/
    val opInterpolate by FUNC(Float1, Float1, Float1, Float1) { a, b, c ->
        RETURN(mix(a, b, c))
    }

    val opUnion by FUNC(Float1, Float1, Float1) { a, b ->
        RETURN(min(a, b))
    }
    val opIntersect by FUNC(Float1, Float1, Float1) { a, b ->
        RETURN(max(a, b))
    }
    val opSubtract by FUNC(Float1, Float1, Float1) { a, b ->
        RETURN(max(a, -b))
    }
    val opRound by FUNC(Float1, Float1, Float1) { d, r ->
        RETURN(d - r)
    }
    val opOnion by FUNC(Float1, Float1, Float1) { d, r ->
        RETURN(abs(d) - r)
    }

    val opSmoothUnion by FUNC(Float1, Float1, Float1, Float1) { d1, d2, k ->
        val h = TEMP(Float1)
        SET(h, clamp(.5f.lit + .5f.lit * (d2 - d1) / k, 0f.lit, 1f.lit))
        RETURN(mix(d2, d1, h) - k * h * (1f.lit - h))
    }
    val opSmoothSubtraction by FUNC(Float1, Float1, Float1, Float1) { d1, d2, k ->
        val h = TEMP(Float1)
        SET(h, clamp(.5f.lit - .5f.lit * (d2 + d1) / k, 0f.lit, 1f.lit))
        RETURN(mix(d2, -d1, h) + k * h * (1f.lit - h))
    }
    val opSmoothIntersection by FUNC(Float1, Float1, Float1, Float1) { d1, d2, k ->
        val h = TEMP(Float1)
        SET(h, clamp(.5f.lit - .5f.lit * (d2 - d1) / k, 0f.lit, 1f.lit))
        RETURN(mix(d2, d1, h) + k * h * (1f.lit - h))
    }

    // https://iquilezles.org/articles/distfunctions2d/
    val circle by FUNC(Float1, Float2, Float1) { p, r ->
        RETURN(length(p) - r)
    }
    val roundedBox by FUNC(Float1, Float2, Float2, Float4) { p, b, r ->
        val q = TEMP(Float2)
        SET(r["xy"], TERNARY(p.x gt 0f.lit, r["xy"], r["zw"]))
        SET(r.x, TERNARY(p.y gt 0f.lit, r.x, r.y))
        SET(q, abs(p) - b + r.x)
        RETURN(min(max(q.x, q.y), 0f.lit) + length(max(q, 0f.lit)) - r.x)
    }
    val box by FUNC(Float1, Float2, Float2) { p, b ->
        val d = TEMP(Float2)
        SET(d, abs(p) - b)
        RETURN(length(max(d, 0f.lit)) + min(max(d.x, d.y), 0f.lit))
    }

    val computeAAAlphaFromDist by FUNC(Float1, Float1) { d ->
        RETURN(1f.lit - clamp(d / fwidth(d) + 0.5f.lit, 0f.lit, 1f.lit))
    }

    val mixPremultipliedColor by FUNC(Float4, Float4, Float4) { src, dst ->
        RETURN(src + dst * (1f.lit - src.a))
    }
}
