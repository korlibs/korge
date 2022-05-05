package com.soywiz.korge.view.vector

import com.soywiz.korag.AG
import com.soywiz.korag.DefaultShaders
import com.soywiz.korag.FragmentShaderDefault
import com.soywiz.korag.VertexShaderDefault
import com.soywiz.korag.shader.*
import com.soywiz.korge.internal.KorgeInternal
import com.soywiz.korim.bitmap.Bitmap32
import com.soywiz.korim.color.toVector3D
import com.soywiz.korim.paint.*
import com.soywiz.korma.geom.Matrix
import com.soywiz.korma.geom.Vector3D
import com.soywiz.korma.geom.toMatrix3D
import kotlin.math.PI

@KorgeInternal
object GpuShapeViewPrograms {
    val u_ProgramType = Uniform("u_ProgramType", VarType.Float1)
    val u_LineWidth = Uniform("u_LineWidth", VarType.Float1)
    val u_Color = Uniform("u_Color", VarType.Float4)
    val u_GlobalAlpha = Uniform("u_GlobalAlpha", VarType.Float1)
    val u_Transform = Uniform("u_Transform", VarType.Mat4)
    val u_Gradientp0 = Uniform("u_Gradientp0", VarType.Float3)
    val u_Gradientp1 = Uniform("u_Gradientp1", VarType.Float3)
    val a_Dist: Attribute = Attribute("a_Dist", VarType.Float1, normalized = false, precision = Precision.MEDIUM)
    val v_Dist: Varying = Varying("v_Dist", VarType.Float1, precision = Precision.MEDIUM)
    val LAYOUT = VertexLayout(DefaultShaders.a_Pos)
    val LAYOUT_TEX = VertexLayout(DefaultShaders.a_Tex)
    val LAYOUT_DIST = VertexLayout(a_Dist)
    val LAYOUT_FILL = VertexLayout(DefaultShaders.a_Pos, DefaultShaders.a_Tex)

    val LAYOUT_POS_DIST = VertexLayout(DefaultShaders.a_Pos, a_Dist)
    val LAYOUT_POS_TEX_FILL_DIST = VertexLayout(DefaultShaders.a_Pos, DefaultShaders.a_Tex, a_Dist)

    val LW = (u_LineWidth)
    val LW1 = Program.ExpressionBuilder { (LW - 1f.lit) }

    val Operand.pow2: Operand get() = Program.ExpressionBuilder { pow(this@pow2, 2f.lit) }

    const val PROGRAM_TYPE_COLOR = 0
    const val PROGRAM_TYPE_BITMAP = 1
    const val PROGRAM_TYPE_GRADIENT_LINEAR = 2
    const val PROGRAM_TYPE_GRADIENT_RADIAL = 3
    const val PROGRAM_TYPE_GRADIENT_SWEEP = 4
    const val PROGRAM_TYPE_STENCIL = 5

     val PROGRAM_COMBINED = Program(
        name = "GpuShapeViewPrograms.Combined",
        vertex = VertexShaderDefault {
            SET(out, (u_ProjMat * u_ViewMat) * vec4(a_Pos, 0f.lit, 1f.lit))
            //SET(out, vec4(a_Pos, 0f.lit, 1f.lit))
            SET(v_Tex, DefaultShaders.a_Tex)
            SET(v_Dist, a_Dist)
        },
        fragment = FragmentShaderDefault {
            IF(u_ProgramType eq PROGRAM_TYPE_STENCIL.toFloat().lit) {
                SET(out, vec4(1f.lit, 0f.lit, 0f.lit, 1f.lit))
                RETURN()
            }
            IF(abs(v_Dist) ge LW) {
                DISCARD()
            }
            IF_ELSE_LIST(u_ProgramType, 0, 4) {
                when (it) {
                    // Color paint
                    PROGRAM_TYPE_COLOR -> {
                        SET(out, u_Color)
                        //SET(out, vec4(1f.lit, 1f.lit, 0f.lit, 1f.lit))
                    }
                    // Bitmap paint
                    PROGRAM_TYPE_BITMAP -> {
                        // @TODO: we should convert 0..1 to texture slice coordinates
                        SET(out, texture2D(u_Tex, fract(vec2((u_Transform * vec4(v_Tex, 0f.lit, 1f.lit))["xy"]))))
                    }
                    // Linear gradient paint
                    PROGRAM_TYPE_GRADIENT_LINEAR -> {
                        SET(out, texture2D(u_Tex, (u_Transform * vec4(v_Tex.x, v_Tex.y, 0f.lit, 1f.lit))["xy"]))
                    }
                    // Radial gradient paint
                    PROGRAM_TYPE_GRADIENT_RADIAL -> {
                        val rpoint = createTemp(VarType.Float2)
                        SET(rpoint["xy"], (u_Transform * vec4(v_Tex.x, v_Tex.y, 0f.lit, 1f.lit))["xy"])
                        val x = rpoint.x
                        val y = rpoint.y
                        val x0 = u_Gradientp0.x
                        val y0 = u_Gradientp0.y
                        val r0 = u_Gradientp0.z
                        val x1 = u_Gradientp1.x
                        val y1 = u_Gradientp1.y
                        val r1 = u_Gradientp1.z
                        val ratio = t_Temp0.x
                        val r0r1_2 = t_Temp0.y
                        val r0pow2 = t_Temp0.z
                        val r1pow2 = t_Temp0.w
                        val y0_y1 = t_Temp1.x
                        val x0_x1 = t_Temp1.y
                        val r0_r1 = t_Temp1.z
                        val radial_scale = t_Temp1.w

                        SET(r0r1_2, 2f.lit * r0 * r1)
                        SET(r0pow2, r0.pow2)
                        SET(r1pow2, r1.pow2)
                        SET(x0_x1, x0 - x1)
                        SET(y0_y1, y0 - y1)
                        SET(r0_r1, r0 - r1)
                        SET(radial_scale, 1f.lit / ((r0 - r1).pow2 - (x0 - x1).pow2 - (y0 - y1).pow2))

                        SET(
                            ratio,
                            1f.lit - (-r1 * r0_r1 + x0_x1 * (x1 - x) + y0_y1 * (y1 - y) - sqrt(r1pow2 * ((x0 - x).pow2 + (y0 - y).pow2) - r0r1_2 * ((x0 - x) * (x1 - x) + (y0 - y) * (y1 - y)) + r0pow2 * ((x1 - x).pow2 + (y1 - y).pow2) - (x1 * y0 - x * y0 - x0 * y1 + x * y1 + x0 * y - x1 * y).pow2)) * radial_scale
                        )
                        SET(out, texture2D(u_Tex, vec2(ratio, 0f.lit)))
                    }
                    // Sweep gradient paint
                    PROGRAM_TYPE_GRADIENT_SWEEP -> {
                        val rpoint = createTemp(VarType.Float2)
                        SET(rpoint["xy"], (u_Transform * vec4(v_Tex.x, v_Tex.y, 0f.lit, 1f.lit))["xy"])
                        val x = rpoint.x
                        val y = rpoint.y
                        val ratio = t_Temp0.x
                        val angle = t_Temp0.y
                        val x0 = u_Gradientp0.x
                        val y0 = u_Gradientp0.y
                        val PI2 = (PI * 2).toFloat().lit

                        SET(angle, atan(y - y0, x - x0))
                        IF(angle lt 0f.lit) { SET(angle, angle + PI2) }
                        SET(ratio, angle / PI2)
                        SET(out, texture2D(u_Tex, fract(vec2(ratio, 0f.lit))))
                    }
                }
            }

            // Update global alpha
            SET(out.a, out.a * u_GlobalAlpha)
            IF(abs(v_Dist) ge LW1) {
                //run {
                val aaAlpha = 1f.lit - (abs(v_Dist) - LW1)
                SET(out["a"], out["a"] * aaAlpha)
                //SET(out["a"], out["a"] * clamp(aaAlpha, 0f.lit, 1f.lit))
            }
        },
    )

    ///////////////
    data class PaintShader(
        val uniforms: AG.UniformValues = AG.UniformValues(),
        val texUniforms: AG.UniformValues = AG.UniformValues(),
        val program: Program = PROGRAM_COMBINED
    )

    val stencilPaintShader = PaintShader(
        AG.UniformValues(u_ProgramType to PROGRAM_TYPE_STENCIL.toFloat(),),
        AG.UniformValues(),
    )

    fun paintToShaderInfo(
        stateTransform: Matrix,
        paint: Paint,
        globalAlpha: Double,
        lineWidth: Double,
    ): PaintShader? = when (paint) {
        is NonePaint -> {
            null
        }
        is ColorPaint -> {
            PaintShader(AG.UniformValues(
                u_ProgramType to PROGRAM_TYPE_COLOR.toFloat(),
                u_Color to paint.toVector3D(),
                u_GlobalAlpha to globalAlpha.toFloat(),
                u_LineWidth to lineWidth.toFloat(),
            ), AG.UniformValues())

        }
        is BitmapPaint -> {
            val mat = Matrix().apply {
                identity()
                preconcat(paint.transform)
                preconcat(stateTransform)
                //if (matrix != null) preconcat(matrix)
                invert()
                scale(1.0 / paint.bitmap.width, 1.0 / paint.bitmap.height)
            }

            //val mat = (paint.transform * stateTransform)
            //mat.scale(1.0 / paint.bitmap.width, 1.0 / paint.bitmap.height)
            //println("mat=$mat")
            PaintShader(AG.UniformValues(
                u_ProgramType to PROGRAM_TYPE_BITMAP.toFloat(),
                u_Transform to mat.toMatrix3D(), // @TODO: Why is this transposed???
                u_GlobalAlpha to globalAlpha.toFloat(),
                u_LineWidth to lineWidth.toFloat(),
                //}, GpuShapeView.PROGRAM_BITMAP)
            ), AG.UniformValues(
                DefaultShaders.u_Tex to paint.bitmap
            ))
        }
        is GradientPaint -> {
            val gradientBitmap = Bitmap32(256, 1)
            gradientBitmap.lock {
                paint.fillColors(gradientBitmap.dataPremult)
            }

            val npaint = paint.copy(transform = Matrix().apply {
                identity()
                preconcat(paint.transform)
                preconcat(stateTransform)
                //if (matrix != null) preconcat(matrix)
            })
            //val mat = stateTransform * paint.gradientMatrix
            val mat = when (paint.kind) {
                GradientKind.LINEAR -> npaint.gradientMatrix
                else -> npaint.transform.inverted()
            }
            PaintShader(
                AG.UniformValues(
                    u_ProgramType to when (paint.kind) {
                        GradientKind.RADIAL -> PROGRAM_TYPE_GRADIENT_RADIAL
                        GradientKind.SWEEP -> PROGRAM_TYPE_GRADIENT_SWEEP
                        else -> PROGRAM_TYPE_GRADIENT_LINEAR
                    },
                    u_Transform to mat.toMatrix3D(),
                    u_Gradientp0 to Vector3D(paint.x0.toFloat(), paint.y0.toFloat(), paint.r0.toFloat()),
                    u_Gradientp1 to Vector3D(paint.x1.toFloat(), paint.y1.toFloat(), paint.r1.toFloat()),
                    u_GlobalAlpha to globalAlpha.toFloat(),
                    u_LineWidth to lineWidth.toFloat(),
                ), AG.UniformValues(
                    DefaultShaders.u_Tex to gradientBitmap
                )
                //when (paint.kind) {
                //    GradientKind.RADIAL -> GpuShapeView.PROGRAM_RADIAL_GRADIENT
                //    GradientKind.SWEEP -> GpuShapeView.PROGRAM_SWEEP_GRADIENT
                //    else -> GpuShapeView.PROGRAM_LINEAR_GRADIENT
                //}
            )
        }
        else -> {
            TODO("paint=$paint")
        }
    }
}
