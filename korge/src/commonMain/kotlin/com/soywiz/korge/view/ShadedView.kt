package com.soywiz.korge.view

import com.soywiz.kmem.*
import com.soywiz.korag.*
import com.soywiz.korag.shader.*
import com.soywiz.korge.render.*
import com.soywiz.korma.geom.*

open class ShadedView(
    program: Program,
    width: Double = 100.0,
    height: Double = 100.0,
    coordsType: CoordsType = CoordsType.D_0_1,
) : RectBase(0.0, 0.0) {
    //constructor(width: Double = 100.0, height: Double = 100.0, callback: ProgramBuilderDefault.() -> Unit) : this(
    //    buildShader(callback), width, height
    //)
    override var width: Double = width; set(v) { field = v; dirtyVertices = true }
    override var height: Double = height; set(v) { field = v; dirtyVertices = true }

    override val bwidth: Double get() = width
    override val bheight: Double get() = height

    init {
        this.program = program
    }

    var padding: Margin = Margin(0.0)
        set(value) {
            field = value
            dirtyVertices = true
        }


    enum class CoordsType {
        D_0_1,
        D_M1_1,
        D_W_H,
    }

    var coordsType: CoordsType = coordsType
        set(value) {
            field = value
            dirtyVertices = true
            //computeVertices()
        }

    protected override fun computeVertices() {
        val L = (sLeft - padding.left).toFloat()
        val T = (sTop - padding.top).toFloat()
        val R = (bwidth + padding.leftPlusRight).toFloat()
        val B = (bheight + padding.topPlusBottom).toFloat()

        var l = -padding.left.toFloat()
        var t = -padding.top.toFloat()
        var r = (bwidth + padding.right).toFloat()
        var b = (bheight + padding.bottom).toFloat()

        val lmin = when (coordsType) {
            CoordsType.D_0_1 -> 0f
            CoordsType.D_M1_1 -> -1f
            CoordsType.D_W_H -> 0f
        }
        val lmax = 1f
        when (coordsType) {
            // @TODO: Adjust values based
            CoordsType.D_0_1, CoordsType.D_M1_1 -> {
                l = l.convertRange(0f, bwidth.toFloat(), lmin, lmax)
                r = r.convertRange(0f, bwidth.toFloat(), lmin, lmax)
                t = t.convertRange(0f, bheight.toFloat(), lmin, lmax)
                b = b.convertRange(0f, bheight.toFloat(), lmin, lmax)
            }
            else -> Unit
        }

        vertices.quad(
            0,
            L, T, R, B,
            globalMatrix,
            l, t,
            r, t,
            l, b,
            r, b,
            renderColorMul,
            renderColorAdd
        )
    }

    open protected fun updateUniforms(uniforms: AG.UniformValues, ctx: RenderContext) {
    }


    override fun renderInternal(ctx: RenderContext) {
        if (!visible) return
        updateUniforms(programUniforms, ctx)
        super.renderInternal(ctx)
    }

    companion object {
        inline fun buildShader(name: String? = null, callback: ProgramBuilderDefault.() -> Unit): Program {
            return BatchBuilder2D.PROGRAM.copy(name = name ?: BatchBuilder2D.PROGRAM.name, fragment = FragmentShaderDefault {
                callback()
                BatchBuilder2D.DO_OUTPUT_FROM(this, out)
            })
        }
    }
}
