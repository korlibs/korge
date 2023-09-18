package korlibs.korge.view

import korlibs.graphics.*
import korlibs.graphics.shader.*
import korlibs.korge.render.*
import korlibs.math.*
import korlibs.math.geom.*

open class ShadedView(
    program: Program,
    size: Size = Size(100f, 100f),
    coordsType: CoordsType = CoordsType.D_0_1,
) : RectBase(Anchor.TOP_LEFT) {
    //constructor(width: Double = 100.0, height: Double = 100.0, callback: ProgramBuilderDefault.() -> Unit) : this(
    //    buildShader(callback), width, height
    //)
    override var unscaledSize: Size = size
        set(value) {
            if (field == value) return
            field = value
            dirtyVertices = true
        }

    override val bwidth: Float get() = widthD.toFloat()
    override val bheight: Float get() = heightD.toFloat()

    init {
        this.program = program
    }

    var padding: Margin = Margin.ZERO
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
        )
    }

    open protected fun updateUniforms(ctx: RenderContext) {
    }


    override fun renderInternal(ctx: RenderContext) {
        if (!visible) return
        updateUniforms(ctx)
        super.renderInternal(ctx)
    }

    companion object {
        inline fun buildShader(name: String? = null, callback: ProgramBuilderDefault.() -> Unit): Program {
            return BatchBuilder2D.PROGRAM.copy(name = name ?: BatchBuilder2D.PROGRAM.name, fragment = FragmentShaderDefault {
                callback()
            })
        }
    }
}
