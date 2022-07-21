package com.soywiz.korim.awt

import com.soywiz.kds.mapFloat
import com.soywiz.kmem.clearSafe
import com.soywiz.kmem.flipSafe
import com.soywiz.kmem.positionSafe
import com.soywiz.korim.bitmap.Bitmap
import com.soywiz.korim.bitmap.NativeImage
import com.soywiz.korim.bitmap.ensureNative
import com.soywiz.korim.color.BGRA
import com.soywiz.korim.color.Colors
import com.soywiz.korim.color.RGBA
import com.soywiz.korim.color.RgbaArray
import com.soywiz.korim.paint.BitmapPaint
import com.soywiz.korim.paint.ColorPaint
import com.soywiz.korim.paint.GradientFiller
import com.soywiz.korim.paint.GradientInterpolationMethod
import com.soywiz.korim.paint.GradientKind
import com.soywiz.korim.paint.GradientPaint
import com.soywiz.korim.paint.Paint
import com.soywiz.korim.paint.TransformedPaint
import com.soywiz.korim.vector.Context2d
import com.soywiz.korim.vector.CycleMethod
import com.soywiz.korma.geom.Matrix
import com.soywiz.korma.geom.vector.LineCap
import com.soywiz.korma.geom.vector.LineJoin
import com.soywiz.korma.geom.vector.VectorPath
import com.soywiz.korma.geom.vector.Winding
import com.soywiz.korma.geom.vector.isEmpty
import java.awt.AlphaComposite
import java.awt.BasicStroke
import java.awt.Color
import java.awt.Graphics2D
import java.awt.Image
import java.awt.MultipleGradientPaint
import java.awt.PaintContext
import java.awt.Rectangle
import java.awt.RenderingHints
import java.awt.RenderingHints.*
import java.awt.Transparency
import java.awt.geom.AffineTransform
import java.awt.geom.Point2D
import java.awt.geom.Rectangle2D
import java.awt.image.BufferedImage
import java.awt.image.ColorModel
import java.awt.image.DataBufferInt
import java.awt.image.Raster
import java.awt.image.WritableRaster
import java.nio.Buffer
import java.nio.ByteBuffer


const val AWT_INTERNAL_IMAGE_TYPE_PRE = BufferedImage.TYPE_INT_ARGB_PRE
const val AWT_INTERNAL_IMAGE_TYPE = BufferedImage.TYPE_INT_ARGB

fun BufferedImage.clone(
    width: Int = this.width,
    height: Int = this.height,
    type: Int = AWT_INTERNAL_IMAGE_TYPE_PRE
): BufferedImage {
	val out = BufferedImage(width, height, type)
	//println("BufferedImage.clone:${this.type} -> ${out.type}")
	val g = out.createGraphics(false)
	g.drawImage(this, 0, 0, width, height, null)
	g.dispose()
	return out
}

fun Image.toBufferedImage(premultiplied: Boolean = true): BufferedImage {
    if (this is BufferedImage && this.isAlphaPremultiplied == premultiplied) return this
    val image = this
    val buffered = BufferedImage(image.getWidth(null), image.getHeight(null), if (premultiplied) AWT_INTERNAL_IMAGE_TYPE_PRE else AWT_INTERNAL_IMAGE_TYPE)
    buffered.graphics.drawImage(image, 0, 0, null)
    return buffered
}

fun BufferedImage.toAwtNativeImage() = AwtNativeImage(this)

class AwtNativeImage private constructor(val awtImage: BufferedImage, val dummy: Boolean) : NativeImage(awtImage.width, awtImage.height, awtImage, premultiplied = (awtImage.type == BufferedImage.TYPE_INT_ARGB_PRE)) {
    init {
        check((awtImage.type == BufferedImage.TYPE_INT_ARGB_PRE) || (awtImage.type == BufferedImage.TYPE_INT_ARGB))
    }
    val dataBuffer = awtImage.raster.dataBuffer as DataBufferInt
    val awtData = dataBuffer.data
    constructor(awtImage: BufferedImage) : this(awtConvertImageIfRequired(awtImage), true)
	override val name: String = "AwtNativeImage"

	override fun getContext2d(antialiasing: Boolean): Context2d = Context2d(AwtContext2dRender(awtImage, antialiasing))

    override fun readPixelsUnsafe(x: Int, y: Int, width: Int, height: Int, out: RgbaArray, offset: Int) {
        for (y0 in 0 until height) {
            val iindex = index(x, y0 + y)
            val oindex = offset + (y0 * width)
            com.soywiz.kmem.arraycopy(awtData, iindex, out.ints, oindex, width)
            conv(out.ints, oindex, width)
        }
    }

    override fun writePixelsUnsafe(x: Int, y: Int, width: Int, height: Int, out: RgbaArray, offset: Int) {
        for (y0 in 0 until height) {
            val iindex = index(x, y0 + y)
            val oindex = offset + (y0 * width)
            com.soywiz.kmem.arraycopy(out.ints, oindex, awtData, iindex, width)
            conv(awtData, iindex, width)
        }
    }

    override fun setRgba(x: Int, y: Int, v: RGBA) { awtData[index(x, y)] = conv(v.value) }
    override fun getRgba(x: Int, y: Int): RGBA = RGBA(conv(awtData[index(x, y)]))

    private val rbufferData: ByteBuffer by lazy { ByteBuffer.allocateDirect(width * height * 4) }

    private var rbufferVersion = -1
	private val rbuffer: ByteBuffer get() {
        if (rbufferVersion != contentVersion) {
            rbufferVersion = contentVersion
            rbufferData.also { buf ->
                buf.clearSafe()
                val ib = buf.asIntBuffer()
                ib.put(dataBuffer.data)
                for (n in 0 until area) ib.put(n, argb2rgba(ib.get(n)))
                buf.positionSafe(width * height * 4)
                buf.flipSafe()
            }
        }
        return rbufferData
	}

    private fun argb2rgba(col: Int): Int = (col shl 8) or (col ushr 24)

    private fun conv(data: Int) = BGRA.bgraToRgba(data)
    private fun conv(data: IntArray, offset: Int, size: Int) = BGRA.bgraToRgba(data, offset, size)

    val buffer: ByteBuffer get() = rbuffer.apply { (this as Buffer).rewind() }
}

//fun createRenderingHints(antialiasing: Boolean): RenderingHints = RenderingHints(mapOf<RenderingHints.Key, Any>())

fun createRenderingHints(antialiasing: Boolean): RenderingHints = RenderingHints(
    if (antialiasing) {
        mapOf(
            KEY_ANTIALIASING to java.awt.RenderingHints.VALUE_ANTIALIAS_ON, RenderingHints.KEY_RENDERING to RenderingHints.VALUE_RENDER_QUALITY, RenderingHints.KEY_COLOR_RENDERING to RenderingHints.VALUE_COLOR_RENDER_QUALITY
            //, RenderingHints.KEY_INTERPOLATION to RenderingHints.VALUE_INTERPOLATION_BILINEAR
            , RenderingHints.KEY_INTERPOLATION to RenderingHints.VALUE_INTERPOLATION_BICUBIC, RenderingHints.KEY_ALPHA_INTERPOLATION to RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY, RenderingHints.KEY_TEXT_ANTIALIASING to RenderingHints.VALUE_TEXT_ANTIALIAS_ON, RenderingHints.KEY_FRACTIONALMETRICS to RenderingHints.VALUE_FRACTIONALMETRICS_ON
        )
    } else {
        mapOf(
            KEY_ANTIALIASING to java.awt.RenderingHints.VALUE_ANTIALIAS_OFF, RenderingHints.KEY_RENDERING to RenderingHints.VALUE_RENDER_SPEED, RenderingHints.KEY_COLOR_RENDERING to RenderingHints.VALUE_COLOR_RENDER_SPEED, RenderingHints.KEY_INTERPOLATION to RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR, RenderingHints.KEY_ALPHA_INTERPOLATION to RenderingHints.VALUE_ALPHA_INTERPOLATION_SPEED, RenderingHints.KEY_TEXT_ANTIALIASING to RenderingHints.VALUE_TEXT_ANTIALIAS_OFF, RenderingHints.KEY_FRACTIONALMETRICS to RenderingHints.VALUE_FRACTIONALMETRICS_OFF
        )
    }
)

fun BufferedImage.createGraphics(antialiasing: Boolean): Graphics2D = this.createGraphics().apply {
	addRenderingHints(createRenderingHints(antialiasing))
}

//private fun BufferedImage.scaled(scale: Double): BufferedImage {
//	val out = BufferedImage(Math.ceil(this.width * scale).toInt(), Math.ceil(this.height * scale).toInt(), this.type)
//	out.createGraphics(antialiasing = true).drawImage(this, 0, 0, out.width, out.height, null)
//	return out
//}

class AwtContext2dRender(val awtImage: BufferedImage, val antialiasing: Boolean = true, val warningProcessor: ((message: String) -> Unit)? = null) : com.soywiz.korim.vector.renderer.Renderer() {
	//val nativeImage = AwtNativeImage(awtImage)
	override val width: Int get() = awtImage.width
	override val height: Int get() = awtImage.height
	val awtTransform = AffineTransform()
	val g = awtImage.createGraphics(antialiasing = antialiasing)

	val hints = createRenderingHints(antialiasing)

	fun VectorPath.toJava2dPaths(winding: Winding?): List<java.awt.geom.Path2D.Double> {
		if (this.isEmpty()) return listOf()
		val winding = when (winding ?: this.winding) {
            Winding.EVEN_ODD -> java.awt.geom.GeneralPath.WIND_EVEN_ODD
            else -> java.awt.geom.GeneralPath.WIND_NON_ZERO
        }
		//val winding = java.awt.geom.GeneralPath.WIND_NON_ZERO
		//val winding = java.awt.geom.GeneralPath.WIND_EVEN_ODD
		val polylines = ArrayList<java.awt.geom.Path2D.Double>()
		var parts = 0
		var polyline = java.awt.geom.Path2D.Double(winding)
		//kotlin.io.println("---")

		fun flush() {
			if (parts > 0) {
				polylines += polyline
				polyline = java.awt.geom.Path2D.Double(winding)
			}
			parts = 0
		}

		this.visitCmds(
            moveTo = { x, y ->
                //flush()
                polyline.moveTo(x, y)
                //kotlin.io.println("moveTo: $x, $y")
            },
            lineTo = { x, y ->
                polyline.lineTo(x, y)
                //kotlin.io.println("lineTo: $x, $y")
                parts++
            },
            quadTo = { cx, cy, ax, ay ->
                polyline.quadTo(cx, cy, ax, ay)
                parts++
            },
            cubicTo = { cx1, cy1, cx2, cy2, ax, ay ->
                polyline.curveTo(cx1, cy1, cx2, cy2, ax, ay)
                parts++
            },
            close = {
                polyline.closePath()
                //kotlin.io.println("closePath")
                parts++
            }
        )
		flush()
		return polylines
	}

	fun VectorPath.toJava2dPath(winding: Winding?): java.awt.geom.Path2D.Double? {
		return toJava2dPaths(winding).firstOrNull()
	}

	//override fun renderShape(shape: Shape, transform: Matrix, shapeRasterizerMethod: ShapeRasterizerMethod) {
	//	when (shapeRasterizerMethod) {
	//		ShapeRasterizerMethod.NONE -> {
	//			super.renderShape(shape, transform, shapeRasterizerMethod)
	//		}
	//		ShapeRasterizerMethod.X1, ShapeRasterizerMethod.X2, ShapeRasterizerMethod.X4 -> {
	//			val scale = shapeRasterizerMethod.scale
	//			val newBi = BufferedImage(Math.ceil(awtImage.width * scale).toInt(), Math.ceil(awtImage.height * scale).toInt(), awtImage.type)
	//			val bi = Context2d(AwtContext2dRender(newBi, antialiasing = false))
	//			bi.scale(scale, scale)
	//			bi.transform(transform)
	//			bi.draw(shape)
	//			val renderBi = when (shapeRasterizerMethod) {
	//				ShapeRasterizerMethod.X1 -> newBi
	//				ShapeRasterizerMethod.X2 -> newBi.scaled(0.5)
	//				ShapeRasterizerMethod.X4 -> newBi.scaled(0.5).scaled(0.5)
	//				else -> newBi
	//			}
	//			this.g.drawImage(renderBi, 0, 0, null)
	//		}
	//	}
	//}

	override fun drawImage(image: Bitmap, x: Double, y: Double, width: Double, height: Double, transform: Matrix) {
		//transform.toAwt()
		//BufferedImageOp

        //AffineTransformOp(AffineTransformOp.TYPE_BICUBIC)
        this.g.keepTransform {
            this.g.transform(transform.toAwt())
            this.g.drawImage(
                (image.ensureNative() as AwtNativeImage).awtImage,
                x.toInt(), y.toInt(),
                width.toInt(), height.toInt(),
                null
            )
        }
	}

    fun Paint.toAwt(transform: AffineTransform): java.awt.Paint = try {
        this.toAwtUnsafe(transform)
    } catch (e: Throwable) {
        warningProcessor?.invoke("Paint.toAwt: $e")
        Color.PINK
    }

    private val USE_ACCURATE_RADIAL_PAINT = true

	fun Paint.toAwtUnsafe(transform: AffineTransform): java.awt.Paint = when (this) {
        is ColorPaint -> convertColor(this.color)
        is TransformedPaint -> {
            val t1 = AffineTransform()
            t1.concatenate(transform)
            t1.concatenate(this.transform.toAwt())

            /*
            val mat = Matrix()
            mat.postconcat(transform.toMatrix())
            mat.postconcat(this.transform)

            println("-------------")
            println("transform=${transform.toMatrix()}")
            println("this.transform=${this.transform}")
            println("t1=${t1.toMatrix()}")
            println("mat=$mat")
            */

            //println("Transformed paint: ${this.transform} --- $transform")
            //t1.preConcatenate(this.transform.toAwt())
            //t1.preConcatenate(transform)

            when (this) {
                is GradientPaint -> {
                    val pairs = this.stops.map(Double::toFloat).zip(this.colors.map { convertColor(RGBA(it)) })
                        .distinctBy { it.first }
                    val stops = pairs.map { it.first }.toFloatArray()
                    val colors = pairs.map { it.second }.toTypedArray()
                    val defaultColor = colors.firstOrNull() ?: Color.PINK

                    //println("    - Gradient: ${Point2D.Double(this.x0, this.y0)} --- ${Point2D.Double(this.x1, this.y1)}")

                    when (this.kind) {
                        GradientKind.LINEAR -> {
                            val valid = (pairs.size >= 2) && ((x0 != x1) || (y0 != y1))
                            if (valid) {
                                java.awt.LinearGradientPaint(
                                    Point2D.Double(this.x0, this.y0),
                                    Point2D.Double(this.x1, this.y1),
                                    stops,
                                    colors,
                                    this.cycle.toAwt(),
                                    this.interpolationMethod.toAwt(),
                                    t1
                                )
                            } else {
                                defaultColor
                            }
                        }
                        GradientKind.RADIAL -> {
                            val valid = (pairs.size >= 2)
                            if (valid) {
                                if (USE_ACCURATE_RADIAL_PAINT) {
                                    AwtKorimGradientPaint(
                                        this,
                                        transform
                                    )
                                } else {
                                    java.awt.RadialGradientPaint(
                                        Point2D.Double(this.x1, this.y1),
                                        this.r1.toFloat(),
                                        Point2D.Double(this.x0, this.y0),
                                        stops,
                                        colors,
                                        this.cycle.toAwt(),
                                        this.interpolationMethod.toAwt(),
                                        t1
                                    )
                                }
                            } else {
                                defaultColor
                            }
                        }
                        else -> {
                            AwtKorimGradientPaint(this, transform)
                        }
                    }

                }
                is BitmapPaint -> {
                    object : java.awt.TexturePaint(
                        this.bitmap.toAwt(),
                        Rectangle2D.Double(0.0, 0.0, this.bitmap.width.toDouble(), this.bitmap.height.toDouble())
                    ) {
                        override fun createContext(
                            cm: ColorModel?,
                            deviceBounds: Rectangle?,
                            userBounds: Rectangle2D?,
                            xform: AffineTransform?,
                            hints: RenderingHints?
                        ): PaintContext {
                            val out = AffineTransform()
                            if (xform != null) {
                                out.concatenate(xform)
                            }
                            out.concatenate(t1)
                            return super.createContext(cm, deviceBounds, userBounds, out, this@AwtContext2dRender.hints)
                        }
                    }
                }
                else -> java.awt.Color(Colors.BLACK.value)
            }
        }
		else -> java.awt.Color(Colors.BLACK.value)
	}

	fun LineCap.toAwt() = when (this) {
        LineCap.BUTT -> BasicStroke.CAP_BUTT
        LineCap.ROUND -> BasicStroke.CAP_ROUND
        LineCap.SQUARE -> BasicStroke.CAP_SQUARE
	}

	fun LineJoin.toAwt() = when (this) {
        LineJoin.BEVEL -> BasicStroke.JOIN_BEVEL
        LineJoin.MITER -> BasicStroke.JOIN_MITER
        LineJoin.ROUND -> BasicStroke.JOIN_ROUND
	}

	inline fun Graphics2D.keepTransform(callback: () -> Unit) {
		val old = AffineTransform(this.transform)
		try {
			callback()
		} finally {
			this.transform = old
		}
	}

    private var oldClipState: VectorPath? = null

	fun applyState(state: Context2d.State, fill: Boolean, winding: Winding?) {
		val t = state.transform
		awtTransform.setToMatrix(t)
		//g.transform = awtTransform
        //g.transform = AffineTransform()
        if (oldClipState != state.clip) {
            oldClipState = state.clip?.clone()
            g.clip = state.clip?.toJava2dPath(winding)
        }
		if (fill) {
			g.paint = state.fillStyle.toAwt(awtTransform)
		} else {
			g.stroke = BasicStroke(
                state.scaledLineWidth.toFloat(),
                state.lineCap.toAwt(),
                state.lineJoin.toAwt(),
                state.miterLimit.toFloat(),
                state.lineDash?.mapFloat { it.toFloat() }?.toFloatArray(),
                state.lineDashOffset.toFloat()
            )
			g.paint = state.strokeStyle.toAwt(awtTransform)
		}
		val comp = AlphaComposite.SRC_OVER
		g.composite = if (state.globalAlpha == 1.0) AlphaComposite.getInstance(comp) else AlphaComposite.getInstance(
            comp,
            state.globalAlpha.toFloat()
        )
	}

	override fun render(state: Context2d.State, fill: Boolean, winding: Winding?) {
		if (state.path.isEmpty()) return

        //println("AwtNativeImage.render: winding=$winding, state.path.winding=${state.path.winding}: ${state.path}")

		applyState(state, fill, winding)

		val awtPaths = state.path.toJava2dPaths(winding)
		for (awtPath in awtPaths) {
			g.setRenderingHints(hints)
			if (fill) {
				g.fill(awtPath)
			} else {
				g.draw(awtPath)
			}
		}
	}
}

class AwtKorimGradientPaint(
    val paint: GradientPaint,
    val stateTransform: AffineTransform
) : java.awt.Paint {
    val filler = GradientFiller().set(paint, Context2d.State(transform = stateTransform.toMatrix()))

    override fun getTransparency(): Int = Transparency.TRANSLUCENT

    override fun createContext(
        cm: ColorModel?,
        deviceBounds: Rectangle?,
        userBounds: Rectangle2D?,
        xform: AffineTransform?,
        hints: RenderingHints?
    ): PaintContext {
        //println("userBounds=$userBounds, deviceBounds=$deviceBounds, xform=$xform")

        return object : PaintContext {
            val _colorModel = cm ?: ColorModel.getRGBdefault()

            override fun dispose() = Unit
            override fun getColorModel(): ColorModel = _colorModel

            var rasterSide = 0
            lateinit var raster: WritableRaster
            lateinit var out: IntArray

            override fun getRaster(x: Int, y: Int, w: Int, h: Int): Raster {
                val maxSide = kotlin.math.max(kotlin.math.max(w, h), 32)
                if (rasterSide < maxSide) {
                    rasterSide = maxSide
                    raster = _colorModel.createCompatibleWritableRaster(rasterSide, rasterSide)
                    out = IntArray(rasterSide * rasterSide * 4)
                }
                //val raster = _colorModel.createCompatibleWritableRaster(w, h)
                //val out = IntArray(w * h * 4)

                var n = 0
                for (py in 0 until h) {
                    for (px in 0 until w) {
                        val col = filler.getColor((x + px).toDouble(), (y + py).toDouble())
                        //val col = Colors.RED
                        out[n++] = col.r
                        out[n++] = col.g
                        out[n++] = col.b
                        out[n++] = col.a
                    }
                }
                raster.setPixels(0, 0, w, h, out)
                return raster
            }
        }
    }
}

fun AffineTransform.setToMatrix(t: Matrix) {
    setTransform(t.a, t.b, t.c, t.d, t.tx, t.ty)
}

fun Matrix.toAwt() = AffineTransform(a, b, c, d, tx, ty)
fun AffineTransform.toMatrix() = Matrix(scaleX, shearY, shearX, scaleY, translateX, translateY)

fun convertColor(c: RGBA): java.awt.Color = java.awt.Color(c.r, c.g, c.b, c.a)

fun CycleMethod.toAwt() = when (this) {
    CycleMethod.NO_CYCLE, CycleMethod.NO_CYCLE_CLAMP -> MultipleGradientPaint.CycleMethod.NO_CYCLE
    CycleMethod.REPEAT -> MultipleGradientPaint.CycleMethod.REPEAT
    CycleMethod.REFLECT -> MultipleGradientPaint.CycleMethod.REFLECT
}

//fun Paint.toAwt(transform: AffineTransform): java.awt.Paint = this.toAwtUnsafe(transform)

fun GradientInterpolationMethod.toAwt() = when (this) {
    GradientInterpolationMethod.LINEAR -> MultipleGradientPaint.ColorSpaceType.LINEAR_RGB
    GradientInterpolationMethod.NORMAL -> MultipleGradientPaint.ColorSpaceType.SRGB
}
