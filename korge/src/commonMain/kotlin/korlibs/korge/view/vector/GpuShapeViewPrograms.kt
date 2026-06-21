package korlibs.korge.view.vector

import korlibs.graphics.*
import korlibs.graphics.shader.*
import korlibs.image.bitmap.*
import korlibs.image.color.*
import korlibs.image.paint.*
import korlibs.korge.internal.*
import korlibs.math.geom.*
import kotlin.math.*

@KorgeInternal
object GpuShapeViewPrograms {
    object ShapeViewUB : UniformBlock(fixedLocation = 5) {
        val u_ProgramType by float()
        //val u_LineWidth by float()
        val u_Color by vec4()
        val u_ColorMul by vec4()
        val u_GlobalAlpha by float()
        val u_GlobalPixelScale by float()
        val u_Transform by mat4()
        val u_Gradientp0 by vec4()
        val u_Gradientp1 by vec4()
        //val u_TexNew by sampler2D()
    }
    val u_ProgramType get() = ShapeViewUB.u_ProgramType.uniform
    val u_Color get() = ShapeViewUB.u_Color.uniform
    val u_ColorMul get() = ShapeViewUB.u_ColorMul.uniform
    val u_GlobalAlpha get() = ShapeViewUB.u_GlobalAlpha.uniform
    val u_GlobalPixelScale get() = ShapeViewUB.u_GlobalPixelScale.uniform
    val u_Transform get() = ShapeViewUB.u_Transform.uniform
    val u_Gradientp0 get() = ShapeViewUB.u_Gradientp0.uniform
    val u_Gradientp1 get() = ShapeViewUB.u_Gradientp1.uniform
    //val u_TexNew get() = ShapeViewUB.u_TexNew.uniform

    val a_MaxDist: Attribute = Attribute("a_MaxDist", VarType.Float1, normalized = false, precision = Precision.MEDIUM, fixedLocation = 4)
    val a_Dist: Attribute = Attribute("a_Dist", VarType.Float1, normalized = false, precision = Precision.MEDIUM, fixedLocation = 5)
    val v_MaxDist: Varying = Varying("v_MaxDist", VarType.Float1, precision = Precision.MEDIUM)
    val v_Dist: Varying = Varying("v_Dist", VarType.Float1, precision = Precision.MEDIUM)
    val LAYOUT = VertexLayout(DefaultShaders.a_Pos)
    val LAYOUT_TEX = VertexLayout(DefaultShaders.a_Tex)
    val LAYOUT_DIST = VertexLayout(a_Dist)
    val LAYOUT_FILL = VertexLayout(DefaultShaders.a_Pos, DefaultShaders.a_Tex)

    val LAYOUT_POS_DIST = VertexLayout(DefaultShaders.a_Pos, a_Dist)
    val LAYOUT_POS_TEX_FILL_DIST = VertexLayout(DefaultShaders.a_Pos, a_Dist, a_MaxDist)

    //val LW = (u_LineWidth)
    //val LW1 = Program.ExpressionBuilder { (LW - 1f.lit) }

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
            SET(v_Tex, DefaultShaders.a_Pos)
            SET(v_Dist, a_Dist)
            SET(v_MaxDist, a_MaxDist)
        },
        fragment = FragmentShaderDefault {
            IF(u_ProgramType eq PROGRAM_TYPE_STENCIL.toFloat().lit) {
                SET(out, vec4(1f.lit, 0f.lit, 0f.lit, 1f.lit))
                RETURN()
            }
            IF(abs(v_Dist) gt v_MaxDist) { DISCARD() }
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
                        //SET(out, texture2D(u_Tex, vec2(0f.lit, 0f.lit)))
                        //SET(out, vec4(1f, 0f, 1f, 1f))
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
            val aaAlpha = 1f.lit - smoothstep(v_MaxDist * u_GlobalPixelScale - 1.5f.lit, v_MaxDist * u_GlobalPixelScale, abs(v_Dist * u_GlobalPixelScale))
            SET(out, out * u_ColorMul * u_GlobalAlpha * aaAlpha)
        },
    )

    ///////////////
    data class PaintShader(
        val uniforms: UniformBlockBuffer<ShapeViewUB> = UniformBlockBuffer(ShapeViewUB),
        val texture: Bitmap?,
        val program: Program = PROGRAM_COMBINED
    )

    val stencilPaintShader = PaintShader(
        UniformBlockBuffer.single(ShapeViewUB) {
            it[ShapeViewUB.u_ProgramType] = PROGRAM_TYPE_STENCIL.toFloat()
        },
        //AGUniformValues { it[u_ProgramType] = PROGRAM_TYPE_STENCIL.toFloat() },
        null
    )

    fun paintToShaderInfo(
        stateTransform: Matrix,
        paint: Paint,
        globalAlpha: Float,
        lineWidth: Float,
    ): PaintShader? = when (paint) {
        is NonePaint -> {
            null
        }
        is ColorPaint -> {
            PaintShader(UniformBlockBuffer.single(ShapeViewUB) {
                it[u_ProgramType] = PROGRAM_TYPE_COLOR.toFloat()
                it[u_Color] = paint.premultiplied
                it[u_GlobalAlpha] = globalAlpha.toFloat()
                //u_LineWidth to lineWidth.toFloat(),
            }, null)

        }
        is BitmapPaint -> {
            val mat = Matrix.IDENTITY
                .preconcated(paint.transform)
                .preconcated(stateTransform)
                //if (matrix != null) preconcat(matrix)
                .inverted()
                .scaled(1.0 / paint.bitmap.width, 1.0 / paint.bitmap.height)

            //val mat = (paint.transform * stateTransform)
            //mat.scale(1.0 / paint.bitmap.width, 1.0 / paint.bitmap.height)
            //println("mat=$mat")
            PaintShader(
                UniformBlockBuffer.single(ShapeViewUB) {
                    it[u_ProgramType] = PROGRAM_TYPE_BITMAP.toFloat()
                    it[u_Transform] = mat.toMatrix4() // @TODO: Why is this transposed???
                    it[u_GlobalAlpha] = globalAlpha.toFloat()
                    //u_LineWidth to lineWidth.toFloat(),
                    //}, GpuShapeView.PROGRAM_BITMAP)
                },
                paint.bitmap
            )
        }
        is GradientPaint -> {
            val gradientBitmap = Bitmap32(256, 1, premultiplied = true)
            gradientBitmap.lock {
                paint.fillColors(RgbaPremultipliedArray(gradientBitmap.ints))
            }

            val npaint = paint.copy(transform = Matrix.IDENTITY
                .preconcated(paint.transform)
                .preconcated(stateTransform)
                //if (matrix != null) preconcat(matrix)
            )
            //val mat = stateTransform * paint.gradientMatrix
            val mat = when (paint.kind) {
                GradientKind.LINEAR -> npaint.gradientMatrix
                else -> npaint.transform.inverted()
            }
            PaintShader(
                UniformBlockBuffer.single(ShapeViewUB) {
                    it[u_ProgramType] = when (paint.kind) {
                        GradientKind.RADIAL -> PROGRAM_TYPE_GRADIENT_RADIAL
                        GradientKind.SWEEP -> PROGRAM_TYPE_GRADIENT_SWEEP
                        else -> PROGRAM_TYPE_GRADIENT_LINEAR
                    }.toFloat()
                    //println("npaint.gradientMatrix=${npaint.gradientMatrix}")
                    //println("mat=${paint.transform}")
                    //println("mat=${npaint.transform}")
                    //println("mat=$mat")
                    //println("mat=${mat.toMatrix4()}")
                    //println("stateTransform=$stateTransform")
                    it[u_Transform] = mat.toMatrix4()
                    it[u_Gradientp0] = Vector4F(paint.x0.toFloat(), paint.y0.toFloat(), paint.r0.toFloat(), 1f)
                    it[u_Gradientp1] = Vector4F(paint.x1.toFloat(), paint.y1.toFloat(), paint.r1.toFloat(), 1f)
                    it[u_GlobalAlpha] = globalAlpha.toFloat()
                    //it[u_LineWidth] = lineWidth.toFloat()
                },
                gradientBitmap
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
