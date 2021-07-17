package com.soywiz.korim.format

import com.soywiz.klock.*
import com.soywiz.kmem.*
import com.soywiz.korim.bitmap.*
import com.soywiz.korim.color.*
import com.soywiz.korim.font.Font
import com.soywiz.korim.paint.*
import com.soywiz.korim.vector.*
import com.soywiz.korim.vector.renderer.Renderer
import com.soywiz.korio.file.*
import com.soywiz.korio.file.std.*
import com.soywiz.korio.util.*
import com.soywiz.korma.geom.*
import com.soywiz.korma.geom.vector.*
import com.soywiz.krypto.encoding.*
import kotlinx.coroutines.*
import org.khronos.webgl.*
import org.khronos.webgl.set
import org.w3c.dom.*
import org.w3c.dom.url.*
import org.w3c.files.*
import kotlinx.browser.*
import kotlin.coroutines.*
import kotlin.js.*
import kotlin.math.*

actual val nativeImageFormatProvider: NativeImageFormatProvider = if (OS.isJsNodeJs) NodeJsNativeImageFormatProvider else HtmlNativeImageFormatProvider

object NodeJsNativeImageFormatProvider : BaseNativeImageFormatProvider() {
    override val formats: ImageFormat by lazy { RegisteredImageFormats.also { it.register(PNG) } }
}

private val tempB = ArrayBuffer(4)
private val tempI = Int32Array(tempB)
private val temp8 = Uint8Array(tempB)

private val isLittleEndian: Boolean by lazy {
    tempI[0] = 1
    temp8[0].toInt() == 1
}
private val isBigEndian get() = !isLittleEndian

private fun bswap32(v: Int): Int {
    return (v ushr 24) or (v shl 24) or ((v and 0xFF00) shl 8) or (v ushr 8) and 0xFF00
}

private fun bswap32(v: IntArray, offset: Int, size: Int) {
    // @TODO: Use Create Uint8Array from the buffer?
    for (n in offset until offset + size) v[n] = bswap32(v[n])
}

open class HtmlNativeImage(val texSourceBase: TexImageSource, width: Int, height: Int) :
	NativeImage(width, height, texSourceBase, true) {
	override val name: String = "HtmlNativeImage"
    var texSource: TexImageSource = texSourceBase
        private set
	val element: HTMLElement get() = texSource.unsafeCast<HTMLElement>()

	constructor(img: HTMLImageElementLike) : this(img, img.width, img.height)
	constructor(canvas: HTMLCanvasElementLike) : this(canvas, canvas.width, canvas.height)

    val lazyCanvasElement: HTMLCanvasElementLike by lazy {
        if (texSource.asDynamic().src !== undefined) {
            BrowserImage.imageToCanvas(texSource.unsafeCast<HTMLImageElementLike>(), width, height)
        } else {
            texSource.unsafeCast<HTMLCanvasElementLike>()
        }.also { texSource = it }
	}

    val ctx: CanvasRenderingContext2D by lazy { lazyCanvasElement.getContext("2d").unsafeCast<CanvasRenderingContext2D>() }

    private var lastRefresh = 0.0.milliseconds
    override fun readPixelsUnsafe(x: Int, y: Int, width: Int, height: Int, out: RgbaArray, offset: Int) {
        if (width <= 0 || height <= 0) return
        val size = width * height

        if (texSourceBase is HTMLVideoElement) {
            // Must refresh
            val now = PerformanceCounter.reference
            val elapsedTime = now - lastRefresh
            if (elapsedTime >= 16.milliseconds) {
                lastRefresh = now
                ctx.clearRect(0.0, 0.0, width.toDouble(), height.toDouble())
                ctx.drawImage(texSourceBase, 0.0, 0.0)
            }
        }
        val idata = ctx.getImageData(x.toDouble(), y.toDouble(), width.toDouble(), height.toDouble())
        val data = idata.data.buffer.asInt32Buffer().unsafeCast<IntArray>()
        arraycopy(data, 0, out.ints, offset, size)
        if (isBigEndian) bswap32(out.ints, offset, size)
    }

    override fun writePixelsUnsafe(x: Int, y: Int, width: Int, height: Int, out: RgbaArray, offset: Int) {
        if (width <= 0 || height <= 0) return
        val size = width * height
        val idata = ctx.createImageData(width.toDouble(), height.toDouble())
        val data = idata.data.buffer.asInt32Buffer().unsafeCast<IntArray>()
        arraycopy(out.ints, offset, data, 0, size)
        if (isBigEndian) bswap32(data, 0, size)
        ctx.putImageData(idata, x.toDouble(), y.toDouble())
    }

    override fun getContext2d(antialiasing: Boolean): Context2d = Context2d(CanvasContext2dRenderer(lazyCanvasElement))
}

object HtmlNativeImageFormatProvider : NativeImageFormatProvider() {
	override suspend fun decode(data: ByteArray, premultiplied: Boolean): NativeImage = HtmlNativeImage(BrowserImage.decodeToCanvas(data, premultiplied))

	override suspend fun decode(vfs: Vfs, path: String, premultiplied: Boolean): NativeImage {
		//println("HtmlNativeImageFormatProvider.decode($vfs, '$path')")
		return when (vfs) {
			is LocalVfs -> {
				//println("LOCAL: HtmlNativeImageFormatProvider: $vfs, $path")
				HtmlNativeImage(BrowserImage.loadImage(path))
			}
			is UrlVfs -> {
				val jsUrl = vfs.getFullUrl(path)
				//println("URL: HtmlNativeImageFormatProvider: $vfs, $path : $jsUrl")
				HtmlNativeImage(BrowserImage.loadImage(jsUrl, premultiplied))
			}
			else -> {
				//println("OTHER: HtmlNativeImageFormatProvider: $vfs, $path")
				HtmlNativeImage(BrowserImage.decodeToCanvas(vfs[path].readAll(), premultiplied))
			}
		}
	}

	override fun create(width: Int, height: Int, premultiplied: Boolean?): NativeImage {
		return HtmlNativeImage(HtmlCanvas.createCanvas(width, height))
	}

	override fun copy(bmp: Bitmap): NativeImage {
		return HtmlNativeImage(HtmlImage.bitmapToHtmlCanvas(bmp.toBMP32()))
	}

	override suspend fun display(bitmap: Bitmap, kind: Int) {
		if (kind == 1) {
			val img = document.createElement("img")
			img.setAttribute("src", "data:image/png;base64," + PNG.encode(bitmap).toBase64())
			document.body?.appendChild(img)
		} else {
			val img = bitmap.toHtmlNative()
			document.body?.appendChild(img.element)
		}
	}

	override fun mipmap(bmp: Bitmap, levels: Int): NativeImage {
		var out = bmp.ensureNative()
		for (n in 0 until levels) out = mipmap(out)
		return out
	}

	override fun mipmap(bmp: Bitmap): NativeImage {
		val out = NativeImage(ceil(bmp.width * 0.5).toInt(), ceil(bmp.height * 0.5).toInt())
        out.context2d(antialiased = true) {
            renderer.drawImage(bmp, 0.0, 0.0, out.width.toDouble(), out.height.toDouble())
        }
		return out
	}
}

// @TODO: BrowserImage and HtmlImage should be combined!
@Suppress("unused")
object BrowserImage {
    private fun toNodeJsBuffer(@Suppress("UNUSED_PARAMETER") ba: ByteArray): dynamic = js("(Buffer.from(ba.buffer))")

	suspend fun decodeToCanvas(bytes: ByteArray, premultiplied: Boolean = true): HTMLCanvasElementLike {
        if (OS.isJsNodeJs) error("Canvas not available on NodeJS")
        val blob = Blob(arrayOf(bytes), BlobPropertyBag(type = "image/png"))
        val blobURL = URL.createObjectURL(blob)
        try {
            return loadCanvas(blobURL)
        } finally {
            URL.revokeObjectURL(blobURL)
        }
	}

	fun imageToCanvas(img: HTMLImageElementLike): HTMLCanvasElementLike {
        return imageToCanvas(img, img.width, img.height)
	}

    fun imageToCanvas(img: HTMLImageElementLike, width: Int, height: Int): HTMLCanvasElementLike {
        val canvas = HtmlCanvas.createCanvas(width, height)
        //println("[onload.b]")
        val ctx: CanvasRenderingContext2D = canvas.getContext("2d").unsafeCast<CanvasRenderingContext2D>()
        //println("[onload.c]")
        ctx.drawImage(img.unsafeCast<CanvasImageSource>(), 0.0, 0.0)
        return canvas
    }

	suspend fun loadImage(jsUrl: String, premultiplied: Boolean = true): HTMLImageElementLike = suspendCancellableCoroutine { c ->
		// Doesn't work with Kotlin.JS
		//val img = document.createElement("img") as HTMLImageElement
		//println("[1]")
        if (OS.isJsNodeJs) error("Canvas not available on NodeJS")

        val img = document.createElement("img").unsafeCast<HTMLImageElement>()
        img.onload = {
            c.resume(img.unsafeCast<HTMLImageElementLike>())
        }
        img.onerror = { _, _, _, _, _ ->
            c.resumeWithException(RuntimeException("error loading image $jsUrl"))
        }
        img.src = jsUrl
        Unit
	}

	suspend fun loadCanvas(jsUrl: String): HTMLCanvasElementLike {
		return imageToCanvas(loadImage(jsUrl))
	}
}

class CanvasContext2dRenderer(private val canvas: HTMLCanvasElementLike) : Renderer() {
	override val width: Int get() = canvas.width.toInt()
	override val height: Int get() = canvas.height.toInt()

	val ctx = canvas.getContext("2d").unsafeCast<CanvasRenderingContext2D>()

	fun Paint.toJsStr(): Any? {
		return when (this) {
			is NonePaint -> "none"
			is ColorPaint -> this.color.htmlColor
			is GradientPaint -> {
				when (kind) {
					GradientKind.LINEAR -> {
						val grad = ctx.createLinearGradient(this.x0, this.y0, this.x1, this.y1)
						for (n in 0 until this.stops.size) {
							val stop = this.stops.getAt(n)
							val color = this.colors.getAt(n)
							grad.addColorStop(stop, RGBA(color).htmlStringSimple)
						}
						grad
					}
                    GradientKind.RADIAL -> {
						val grad = ctx.createRadialGradient(this.x0, this.y0, this.r0, this.x1, this.y1, this.r1)
						for (n in 0 until this.stops.size) {
							val stop = this.stops.getAt(n)
							val color = this.colors.getAt(n)
							grad.addColorStop(stop, RGBA(color).htmlStringSimple)
						}
						grad
					}
                    GradientKind.SWEEP -> {
                        "fuchsia"
                    }
				}
			}
			is BitmapPaint -> {
				ctx.createPattern(this.bitmap.toHtmlNative().texSource.unsafeCast<CanvasImageSource>(), when {
                    repeatX && repeatY -> "repeat"
                    repeatX -> "repeat-x"
                    repeatY -> "repeat-y"
                    else -> "no-repeat"
                })
				//ctx.call("createPattern", this.bitmap.toHtmlNative().canvas)
			}
			else -> "black"
		}
	}

	private inline fun <T> keep(callback: () -> T): T {
		ctx.save()
		try {
			return callback()
		} finally {
			ctx.restore()
		}
	}

    private var cachedFontSize: Double = Double.NaN
    private var cachedFontName: String = ""
	private fun setFont(font: Font, fontSize: Double) {
        if (font.name == cachedFontName && fontSize == cachedFontSize) return
        cachedFontName = font.name
        cachedFontSize = fontSize
		ctx.font = "${fontSize}px '${font.name}'"
	}

    fun CompositeMode.toJsStr() = when (this) {
        CompositeMode.CLEAR -> "clear"
        CompositeMode.COPY -> "copy"
        CompositeMode.SOURCE_OVER -> "source-over"
        CompositeMode.DESTINATION_OVER -> "destination-over"
        CompositeMode.SOURCE_IN -> "source-in"
        CompositeMode.DESTINATION_IN -> "destination-in"
        CompositeMode.SOURCE_OUT -> "source-out"
        CompositeMode.DESTINATION_OUT -> "destination-out"
        CompositeMode.SOURCE_ATOP -> "source-atop"
        CompositeMode.DESTINATION_ATOP -> "destination-atop"
        CompositeMode.XOR -> "xor"
        CompositeMode.LIGHTER -> "lighter"
    }

    fun BlendMode.toJsStr() = when (this) {
        BlendMode.NORMAL -> "normal"
        BlendMode.MULTIPLY -> "multiply"
        BlendMode.SCREEN -> "screen"
        BlendMode.OVERLAY -> "overlay"
        BlendMode.DARKEN -> "darken"
        BlendMode.LIGHTEN -> "lighten"
        BlendMode.COLOR_DODGE -> "color-dodge"
        BlendMode.COLOR_BURN -> "color-burn"
        BlendMode.HARD_LIGHT -> "hard-light"
        BlendMode.SOFT_LIGHT -> "soft-light"
        BlendMode.DIFFERENCE -> "difference"
        BlendMode.EXCLUSION -> "exclusion"
        BlendMode.HUE -> "hue"
        BlendMode.SATURATION -> "saturation"
        BlendMode.COLOR -> "color"
        BlendMode.LUMINOSITY -> "luminosity"
        else -> this.name.lowercase()
    }

    fun CompositeOperation.toJsStr() = when (this) {
        is CompositeMode -> this.toJsStr()
        is BlendMode -> this.toJsStr()
        else -> "source-over" // Default
    }

	private fun setState(state: Context2d.State, fill: Boolean, doSetFont: Boolean) {
		ctx.globalAlpha = state.globalAlpha
        ctx.globalCompositeOperation = state.globalCompositeOperation.toJsStr()
        if (doSetFont) {
            setFont(state.font, state.fontSize)
        }
		if (fill) {
            ctx.fillStyle = state.fillStyle.toJsStr()
		} else {
            ctx.lineWidth = state.lineWidth
			ctx.lineJoin = when (state.lineJoin) {
				LineJoin.BEVEL -> CanvasLineJoin.BEVEL
				LineJoin.MITER -> CanvasLineJoin.MITER
				LineJoin.ROUND -> CanvasLineJoin.ROUND
			}
			ctx.lineCap = when (state.lineCap) {
				LineCap.BUTT -> CanvasLineCap.BUTT
				LineCap.ROUND -> CanvasLineCap.ROUND
				LineCap.SQUARE -> CanvasLineCap.SQUARE
			}
            ctx.strokeStyle = state.strokeStyle.toJsStr()
		}
	}

	private fun transformPaint(paint: Paint) {
		if (paint is TransformedPaint) {
			val m = paint.transform
            //println("Transformed paint: $m")
			ctx.transform(m.a, m.b, m.c, m.d, m.tx, m.ty)
		}
	}

	override fun drawImage(image: Bitmap, x: Double, y: Double, width: Double, height: Double, transform: Matrix) {
		ctx.save()
		try {
			transform.run { ctx.setTransform(a, b, c, d, tx, ty) }
			ctx.drawImage(
				(image.ensureNative() as HtmlNativeImage).texSource.unsafeCast<CanvasImageSource>(),
                x, y, width, height
			)
		} finally {
			ctx.restore()
		}
	}

	override fun render(state: Context2d.State, fill: Boolean) {
		if (state.path.isEmpty()) return

		//println("beginPath")
        //println("RENDER: $width,$height,fill=$fill")
        //println(" fillStyle=${ctx.fillStyle}, transform=${state.transform}")
		keep {
			setState(state, fill, doSetFont = false)
			ctx.beginPath()

			state.path.visitCmds(
				moveTo = { x, y -> ctx.moveTo(x, y) },
				lineTo = { x, y -> ctx.lineTo(x, y) },
				quadTo = { cx, cy, ax, ay -> ctx.quadraticCurveTo(cx, cy, ax, ay) },
				cubicTo = { cx1, cy1, cx2, cy2, ax, ay -> ctx.bezierCurveTo(cx1, cy1, cx2, cy2, ax, ay) },
				close = { ctx.closePath() }
			)

            val m = state.transform
            ctx.transform(m.a, m.b, m.c, m.d, m.tx, m.ty)
			if (fill) {
				transformPaint(state.fillStyle)
                //println("       - Gadient: ${}")
				ctx.fill(state.path.winding.toCanvasFillRule())
				//println("fill: $s")
			} else {
				transformPaint(state.strokeStyle)

				ctx.stroke()
				//println("stroke: $s")
			}
		}
	}

    fun Winding.toCanvasFillRule() = when (this) {
        Winding.NON_ZERO -> CanvasFillRule.NONZERO
        Winding.EVEN_ODD -> CanvasFillRule.EVENODD
    }

    // @TODO: Do this
    /*
	override fun renderText(
        state: Context2d.State,
        font: Font,
        fontSize: Double,
        text: String,
        x: Double,
        y: Double,
        fill: Boolean
	) {
		keep {
			setState(state, fill, fontSize)

			ctx.textBaseline = when (state.verticalAlign) {
				VerticalAlign.TOP -> CanvasTextBaseline.TOP
				VerticalAlign.MIDDLE -> CanvasTextBaseline.MIDDLE
				VerticalAlign.BASELINE -> CanvasTextBaseline.ALPHABETIC
				VerticalAlign.BOTTOM -> CanvasTextBaseline.BOTTOM
                else -> CanvasTextBaseline.TOP
			}
			ctx.textAlign = when (state.horizontalAlign) {
				HorizontalAlign.LEFT -> CanvasTextAlign.LEFT
				HorizontalAlign.CENTER -> CanvasTextAlign.CENTER
				HorizontalAlign.RIGHT -> CanvasTextAlign.RIGHT
                HorizontalAlign.JUSTIFY -> CanvasTextAlign.LEFT
                else -> CanvasTextAlign.LEFT
			}

			if (fill) {
				ctx.fillText(text, x, y)
			} else {
				ctx.strokeText(text, x, y)
			}
		}
	}

	override fun getBounds(font: Font, fontSize: Double, text: String, out: TextMetrics) {
		keep {
			setFont(font, fontSize)
			val metrics = ctx.measureText(text)
			val width = metrics.width.toInt()
			out.bounds.setTo(0.toDouble(), 0.toDouble(), width.toDouble() + 2, fontSize)
		}
	}
    */
}
