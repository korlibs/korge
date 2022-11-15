package com.soywiz.korge.text

import com.soywiz.korag.shader.*
import com.soywiz.korge.render.*
import com.soywiz.korge.view.*

object MsdfRender {
    val PROGRAM_MSDF = createProgram(msdf = true, inverted = false)
    val PROGRAM_MSDF_I = createProgram(msdf = true, inverted = true)
    val PROGRAM_SDF_R = createProgram(msdf = false, sdf = "r", inverted = false)
    val PROGRAM_SDF_A = createProgram(msdf = false, sdf = "a", inverted = false)

    fun createProgram(msdf: Boolean, sdf: String = "a", inverted: Boolean = false): Program = ShadedView.buildShader(name = "PROGRAM_msdf=${msdf},sdf=$sdf,inverted=$inverted") {
        val median by FUNC(Float1, Float1, Float1, Float1) { a, b, c ->
            RETURN(max(min(a, b), min(max(a, b), c)))
        }

        IF_ELSE_BINARY_LOOKUP(BatchBuilder2D.v_TexIndex, 0, BB_MAX_TEXTURES - 1) { n ->
            SET(out, texture2D(BatchBuilder2D.u_TexN[n], v_Tex["xy"]))
        }
        val d = t_Temp0.x
        val alpha = t_Temp0.y
        SET(out, (out - vec4(.5f.lit)))
        if (msdf) {
            SET(d, median(out["r"], out["g"], out["b"]))
        } else {
            SET(d, out[sdf])
        }
        SET(alpha, clamp(d / fwidth(d) + 0.5f.lit, 0f.lit, 1f.lit))
        if (inverted) {
            SET(alpha, 1f.lit - alpha)
        }
        SET(out, v_Col * alpha)
    }
}
