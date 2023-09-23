package korlibs.korge.render

import korlibs.datastructure.*
import korlibs.graphics.*
import korlibs.graphics.shader.*
import korlibs.image.bitmap.*
import korlibs.image.color.*
import korlibs.korge.internal.*
import korlibs.korge.view.*
import korlibs.logger.*
import korlibs.math.geom.*
import korlibs.math.geom.shape.*
import korlibs.math.geom.vector.*
import kotlin.native.concurrent.*

@SharedImmutable
private val logger = Logger("RenderContext2D")

/**
 * Helper class using [BatchBuilder2D] that keeps a chain of affine transforms [MMatrix], [ColorTransform] and [blendMode]
 * and allows to draw images and scissors with that transform.
 *
 * [keepMatrix], [keepBlendMode], [keepColor] and [keep] block methods allow to do transformations inside its blocks
 * while restoring its initial state at the end of the block.
 *
 * [setMatrix], [translate], [scale], and [rotate] allows to control the transform matrix.
 *
 * [rect] and [imageScale] allows to render color quads and images.
 *
 * [blendFactors] property allow to specify the blending mode to be used.
 * [multiplyColor] will set the multiplicative color to be usd when drawing rects and images.
 *
 * [scissor] methods allow to specify a scissor rectangle limiting the area where the pixels will be renderer.
 */
@OptIn(KorgeInternal::class)
class RenderContext2D(
    @property:KorgeInternal
    val batch: BatchBuilder2D,
    @property:KorgeInternal
    val agBitmapTextureManager: AgBitmapTextureManager
) : Extra by Extra.Mixin() {
	init { logger.trace { "RenderContext2D[0]" } }

    val ctx: RenderContext get() = batch.ctx

    var size: Size = Size(0, 0)
    val width: Double get() = size.width
    val height: Double get() = size.height

    inline fun getTexture(slice: BmpSlice): TextureCoords = agBitmapTextureManager.getTexture(slice)

    @KorgeInternal
	val mpool = Pool<MMatrix> { MMatrix() }

    init { logger.trace { "RenderContext2D[1]" } }

    @KorgeInternal
	var m = Matrix.IDENTITY

    /** Blending mode to be used in the renders */
	var blendMode = BlendMode.NORMAL
    /** Multiplicative color to be used in the renders */
	var multiplyColor = Colors.WHITE
    var filtering: Boolean = true

	init { logger.trace { "RenderContext2D[2]" } }

    /** Executes [callback] restoring the initial transformation [MMatrix] at the end */
	inline fun <T> keepMatrix(crossinline callback: () -> T): T {
		val old = m
		try {
			return callback()
		} finally {
			m = old
		}
	}

    /** Executes [callback] restoring the initial [blendFactors] at the end */
	inline fun <T> keepBlendMode(crossinline callback: () -> T): T {
		val oldBlendFactors = this.blendMode
		try {
			return callback()
		} finally {
			this.blendMode = oldBlendFactors
		}
	}

    /** Executes [callback] restoring the initial [multiplyColor] at the end */
    inline fun <T> keepColor(crossinline callback: () -> T): T {
        val multiplyColor = this.multiplyColor
        try {
            return callback()
        } finally {
            this.multiplyColor = multiplyColor
        }
    }

    /** Executes [callback] restoring the initial [filtering] at the end */
    inline fun <T> keepFiltering(crossinline callback: () -> T): T {
        val filtering = this.filtering
        try {
            return callback()
        } finally {
            this.filtering = filtering
        }
    }

    inline fun <T> keepSize(crossinline callback: () -> T): T {
        val size = this.size
        try {
            return callback()
        } finally {
            this.size = size
        }
    }

    /** Executes [callback] restoring the transform matrix, the [blendMode] and the [multiplyColor] at the end */
	inline fun <T> keep(crossinline callback: () -> T): T {
		return keepMatrix {
			keepBlendMode {
				keepColor {
                    keepFiltering {
                        keepSize {
                            callback()
                        }
                    }
				}
			}
		}
	}

    /** Sets the current transform [matrix] */
    fun setMatrix(matrix: Matrix) {
        this.m = matrix
    }

    /** Translates the current transform matrix by [dx] and [dy] */
    fun translate(p: Point) = translate(p.x, p.y)
    fun translate(dx: Float, dy: Float) { m = m.pretranslated(dx, dy) }
	fun translate(dx: Double, dy: Double) { m = m.pretranslated(dx, dy) }

    /** Scales the current transform matrix by [sx] and [sy] */
    fun scale(s: Scale) = translate(s.scaleX, s.scaleY)
    fun scale(sx: Float, sy: Float) { m = m.prescaled(sx, sy) }
	fun scale(sx: Double, sy: Double) { m = m.prescaled(sx, sy) }

    /** Scales the current transform matrix by [scale] */
    fun scale(scale: Float) = scale(scale, scale)
	fun scale(scale: Double) = scale(scale, scale)

    /** Rotates the current transform matrix by [angle] */
	fun rotate(angle: Angle) {
		m = m.prerotated(angle)
	}

    /** Renders a colored rectangle with the [multiplyColor] with the [blendMode] at [x], [y] of size [width]x[height] */
    fun rect(rect: Rectangle, color: RGBA = this.multiplyColor, filtering: Boolean = this.filtering, bmp: BmpSlice = Bitmaps.white, program: Program? = null) {
        batch.drawQuad(
            getTexture(bmp),
            rect.x.toFloat(),
            rect.y.toFloat(),
            rect.width.toFloat(),
            rect.height.toFloat(),
            filtering = filtering,
            m = m,
            colorMul = color,
            blendMode = blendMode,
            program = program,
        )
    }

    /** Renders a colored rectangle with the [multiplyColor] with the [blendMode] at [x], [y] of size [width]x[height] */
    fun rectOutline(rect: Rectangle, border: Float = 1f, color: RGBA = this.multiplyColor, filtering: Boolean = this.filtering) {
        val (x, y, width, height) = rect
        rect(Rectangle(x, y, width, border.toDouble()), color, filtering)
        rect(Rectangle(x, y, border.toDouble(), height), color, filtering)
        rect(Rectangle(x + width - border, y, border.toDouble(), height), color, filtering)
        rect(Rectangle(x, y + height - border, width, border.toDouble()), color, filtering)
    }

    fun ellipse(center: Point, radius: Size, color: RGBA = this.multiplyColor, filtering: Boolean = this.filtering) {
        simplePath(buildVectorPath(VectorPath()) {
            this.ellipse(center, radius)
        }, color, filtering)
    }

    fun ellipseOutline(center: Point, radius: Size, lineWidth: Double = 1.0, color: RGBA = this.multiplyColor, filtering: Boolean = this.filtering) {
        simplePath(buildVectorPath(VectorPath()) {
            this.ellipse(center, radius)
        }.strokeToFill(lineWidth), color, filtering)
    }

    // @TODO: It doesn't handle holes (it uses a triangle fan approach)
    fun simplePath(path: VectorPath, color: RGBA = this.multiplyColor, filtering: Boolean = this.filtering) {
        for (points in path.toPathPointList()) {
            texturedVertexArrayNoTransform(TexturedVertexArray.fromPointArrayList(points, color, matrix = m), filtering)
        }
    }

    fun texturedVertexArrayNoTransform(texturedVertexArray: TexturedVertexArray, filtering: Boolean = this.filtering, matrix: Matrix = Matrix.NIL) {
        batch.setStateFast(Bitmaps.white, filtering, blendMode, null, icount = texturedVertexArray.icount, vcount = texturedVertexArray.vcount)
        batch.drawVertices(texturedVertexArray, matrix)
    }

    fun texturedVertexArray(texturedVertexArray: TexturedVertexArray, filtering: Boolean = this.filtering) {
        batch.setStateFast(Bitmaps.white, filtering, blendMode, null, icount = texturedVertexArray.icount, vcount = texturedVertexArray.vcount)
        batch.drawVertices(texturedVertexArray, m)
    }

    fun quadPaddedCustomProgram(
        x: Float,
        y: Float,
        width: Float,
        height: Float,
        program: Program,
        padding: Margin = Margin.ZERO,
    ) {
        val ctx = batch.ctx
        //programUniforms
        ctx.useBatcher { batch ->
            //batch.texture1212
            //batch.drawQuad(
            //    vertices, ctx.getTex(baseBitmap).base, smoothing, renderBlendMode,
            //    program = FastMaterialBackground.PROGRAM,
            //    premultiplied = baseBitmap.base.premultiplied, wrap = wrapTexture
            //)

            val L = (x - padding.left).toFloat()
            val T = (y - padding.top).toFloat()
            val R = (width + padding.leftPlusRight).toFloat()
            val B = (height + padding.topPlusBottom).toFloat()

            val l = -padding.left.toFloat()
            val t = -padding.top.toFloat()
            val r = (width + padding.right).toFloat()
            val b = (height + padding.bottom).toFloat()

            val vertices = TexturedVertexArray(6, TexturedVertexArray.QUAD_INDICES)
            vertices.quad(
                0,
                L, T, R, B,
                m,
                l, t,
                r, t,
                l, b,
                r, b,
                multiplyColor,
            )
            batch.setStateFast(Bitmaps.white, filtering, blendMode, program, icount = 6, vcount = 4)
            batch.drawVertices(vertices, Matrix.NIL)
            ctx.flush(RenderContext.FlushKind.STATE)
        }
    }

    /** Renders a [texture] with the [blendMode] at [x], [y] scaling it by [scale].
     * The texture colors will be multiplied by [multiplyColor]. Since it is multiplicative, white won't cause any effect. */
    fun imageScale(texture: Texture, pos: Point, scale: Double = 1.0, filtering: Boolean = this.filtering) {
        imageScale(texture, pos.x, pos.y, scale, filtering)
    }
    fun imageScale(texture: Texture, x: Double, y: Double, scale: Double = 1.0, filtering: Boolean = this.filtering) {
        imageScale(texture, x.toFloat(), y.toFloat(), scale.toFloat(), filtering)
    }
	fun imageScale(texture: Texture, x: Float, y: Float, scale: Float = 1f, filtering: Boolean = this.filtering) {
		//println(m)
		batch.drawQuad(
			texture, x, y, (texture.width * scale), (texture.height * scale),
            filtering = filtering,
			m = m,
			colorMul = multiplyColor,
			blendMode = blendMode,
		)
	}

    /** Temporarily sets the [scissor] (visible rendering area) to [x], [y], [width] and [height] while [block] is executed. */
    inline fun scissor(x: Int, y: Int, width: Int, height: Int, block: () -> Unit) = scissor(AGScissor(x, y, width, height), block)

    /** Temporarily sets the [scissor] (visible rendering area) to [x], [y], [width] and [height] while [block] is executed. */
    inline fun scissor(x: Double, y: Double, width: Double, height: Double, block: () -> Unit) = scissor(x.toInt(), y.toInt(), width.toInt(), height.toInt(), block)

    /** Temporarily sets the [scissor] (visible rendering area) to [x], [y], [width] and [height] while [block] is executed. */
    inline fun scissor(x: Float, y: Float, width: Float, height: Float, block: () -> Unit) = scissor(x.toInt(), y.toInt(), width.toInt(), height.toInt(), block)

    /** Temporarily sets the [scissor] (visible rendering area) to [rect] is executed. */
    inline fun scissor(rect: Rectangle?, block: () -> Unit) =
        scissor(AGScissor(rect), block)

    /** Temporarily sets the [scissor] (visible rendering area) to [scissor] is executed. */
    inline fun scissor(scissor: AGScissor, block: () -> Unit) {
        batch.scissor(getTransformedScissor(scissor)) {
            block()
        }
    }

    @PublishedApi internal fun getTransformedScissor(scissor: AGScissor): AGScissor {
        val tl = m.transform(Point(scissor.left, scissor.top))
        val br = m.transform(Point(scissor.right, scissor.bottom))
        return AGScissor.fromBounds(tl.x.toFloat(), tl.y.toFloat(), br.x.toFloat(), br.y.toFloat())
    }
}

inline fun View.renderCtx2d(ctx: RenderContext, crossinline block: (RenderContext2D) -> Unit) {
    ctx.useCtx2d { context ->
        context.keep {
            context.size = Size(this@renderCtx2d.width, this@renderCtx2d.height)
            context.blendMode = renderBlendMode
            context.multiplyColor = renderColorMul
            context.setMatrix(globalMatrix)
            block(context)
        }
    }
}
