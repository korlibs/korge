package korlibs.image.format

import korlibs.encoding.*
import korlibs.image.bitmap.*
import korlibs.image.color.*
import korlibs.image.font.*
import korlibs.image.paint.*
import korlibs.image.vector.*
import korlibs.image.vector.renderer.*
import korlibs.io.file.*
import korlibs.io.file.std.*
import korlibs.io.util.*
import korlibs.io.wasm.*
import korlibs.math.geom.*
import korlibs.math.geom.vector.*
import korlibs.memory.*
import korlibs.platform.*
import korlibs.time.*
import kotlinx.browser.*
import kotlinx.coroutines.*
import org.khronos.webgl.*
import org.khronos.webgl.set
import org.w3c.dom.*
import org.w3c.dom.url.*
import org.w3c.files.*
import kotlin.coroutines.*
import kotlin.math.*

actual val nativeImageFormatProvider: NativeImageFormatProvider = when {
    Platform.isJsNodeJs -> NodeJsNativeImageFormatProvider
    else -> HtmlNativeImageFormatProvider
}

object NodeJsNativeImageFormatProvider : BaseNativeImageFormatProvider() {
    override val formats: ImageFormat get() = RegisteredImageFormats
    override suspend fun encodeSuspend(image: ImageDataContainer, props: ImageEncodingProps): ByteArray {
        return RegisteredImageFormats.formats.first().encode(image.default)
        //return PNG.encode(image.default.mainBitmap)
    }
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

external interface TexImageSourceJs : TexImageSource, JsAny

open class WasmHtmlNativeImage(val texSourceBase: TexImageSourceJs, width: Int, height: Int)
    : NativeImage(width, height, texSourceBase, premultiplied = true) {
	override val name: String get() = "HtmlNativeImage"
    var texSource: TexImageSourceJs = texSourceBase
        private set
	val element: HTMLElement get() = texSource.unsafeCast<HTMLElement>()

	constructor(img: HTMLImageElementLike) : this(img, img.width, img.height)
	constructor(canvas: HTMLCanvasElementLike) : this(canvas, canvas.width, canvas.height)

    val lazyCanvasElement: HTMLCanvasElementLike by lazy {
        //if (texSource.hasAny("src")) {
        if (texSource is HTMLImageElement) {
            BrowserImage.imageToCanvas(texSource.unsafeCast<HTMLImageElementLike>(), width, height)
        } else {
            texSource.unsafeCast<HTMLCanvasElementLike>()
        }.also { texSource = it }
	}

    val ctx: CanvasRenderingContext2D by lazy {
        //println("lazyCanvasElement: " + lazyCanvasElement)
        lazyCanvasElement.getContext("2d")!!.unsafeCast<CanvasRenderingContext2D>()
    }

    private var lastRefresh = 0.0.milliseconds
    override fun readPixelsUnsafe(x: Int, y: Int, width: Int, height: Int, out: IntArray, offset: Int) {
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
        //val ints = idata.data.buffer.asInt32Array()
        //for (n in 0 until size) out[offset + n] = ints[n]

        val data = idata.data.buffer.asInt32Array()
        arraycopy(data, 0, out, offset, size)
        //println("data=${data[0]}, size=$size, out[offset=$offset]=${out[offset]}")

        if (isBigEndian) bswap32(out, offset, size)
        if (!asumePremultiplied) {
            premultiply(RgbaArray(out), offset, RgbaPremultipliedArray(out), offset, width * height)
        }
    }

    override fun writePixelsUnsafe(x: Int, y: Int, width: Int, height: Int, out: IntArray, offset: Int) {
        if (width <= 0 || height <= 0) return
        val size = width * height
        val temp = IntArray(size)
        arraycopy(out, offset, temp, 0, size)
        if (!asumePremultiplied) depremultiply(RgbaPremultipliedArray(temp), 0, RgbaArray(temp), 0, size)
        if (isBigEndian) bswap32(temp, 0, size)
        val idata = ctx.createImageData(width.toDouble(), height.toDouble())
        arraycopy(temp, 0, idata.data.buffer.asInt32Array(), 0, size)
        ctx.putImageData(idata, x.toDouble(), y.toDouble())
    }

    override fun getContext2d(antialiasing: Boolean): Context2d = Context2d(CanvasContext2dRenderer(lazyCanvasElement))
    fun toDataURL(type: String = "image/png", quality: Double? = null): String = lazyCanvasElement.toDataURL(type, quality?.toJsNumber())
}

object HtmlNativeImageFormatProvider : NativeImageFormatProvider() {
    override suspend fun decodeInternal(data: ByteArray, props: ImageDecodingProps): NativeImageResult {
        return NativeImageResult(WasmHtmlNativeImage(BrowserImage.decodeToCanvas(data, props)))
    }

    override suspend fun decodeInternal(vfs: Vfs, path: String, props: ImageDecodingProps): NativeImageResult {
        return NativeImageResult(when (vfs) {
            is LocalVfs -> {
                //println("LOCAL: HtmlNativeImageFormatProvider: $vfs, $path")
                WasmHtmlNativeImage(BrowserImage.loadImage(path, props))
            }
            is UrlVfs -> {
                val jsUrl = vfs.getFullUrl(path)
                //println("URL: HtmlNativeImageFormatProvider: $vfs, $path : $jsUrl")
                WasmHtmlNativeImage(BrowserImage.loadImage(jsUrl, props))
            }
            else -> {
                //println("OTHER: HtmlNativeImageFormatProvider: $vfs, $path")
                WasmHtmlNativeImage(BrowserImage.decodeToCanvas(vfs[path].readAll(), props))
            }
        })
    }

    override suspend fun encodeSuspend(image: ImageDataContainer, props: ImageEncodingProps): ByteArray {
        return image.default.mainBitmap.toHtmlNative().toDataURL(props.mimeType, props.quality).split("base64,").last().fromBase64()
    }

    override fun create(width: Int, height: Int, premultiplied: Boolean?): NativeImage {
		return WasmHtmlNativeImage(HtmlCanvas.createCanvas(width, height))
	}

	override fun copy(bmp: Bitmap): NativeImage {
		return WasmHtmlNativeImage(HtmlImage.bitmapToHtmlCanvas(bmp.toBMP32()))
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
            renderer.drawImage(bmp, Point.ZERO, out.size.toFloat())
        }
		return out
	}
}

@JsFun("(ba) => { return (Buffer.from(ba.buffer)); }")
private external fun toNodeJsBuffer(@Suppress("UNUSED_PARAMETER") ba: Int8Array): JsAny?

external interface CanvasImageSourceJs : CanvasImageSource, JsAny

// @TODO: BrowserImage and HtmlImage should be combined!
@Suppress("unused")
object BrowserImage {
	suspend fun decodeToCanvas(bytes: ByteArray, props: ImageDecodingProps = ImageDecodingProps.DEFAULT): HTMLCanvasElementLike {
        if (Platform.isJsNodeJs) error("Canvas not available on NodeJS")
        val blob = Blob(jsArrayOf(bytes.toInt8Array()), BlobPropertyBag(type = "image/png"))
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
        val ctx: CanvasRenderingContext2D = canvas.getContext("2d")!!.unsafeCast<CanvasRenderingContext2D>()
        //println("[onload.c]")
        ctx.drawImage(img.unsafeCast<CanvasImageSourceJs>(), 0.0, 0.0)
        return canvas
    }

	suspend fun loadImage(jsUrl: String, props: ImageDecodingProps = ImageDecodingProps.DEFAULT): HTMLImageElementLike = suspendCancellableCoroutine { c ->
		// Doesn't work with Kotlin.JS
		//val img = document.createElement("img") as HTMLImageElement
		//println("[1]")
        if (Platform.isJsNodeJs) error("Canvas not available on NodeJS")

        val img = document.createElement("img").unsafeCast<HTMLImageElement>()
        img.onload = {
            c.resume(img.unsafeCast<HTMLImageElementLike>())
            null
        }
        img.onerror = { _, _, _, _, _ ->
            c.resumeWithException(RuntimeException("error loading image $jsUrl"))
            null
        }
        img.src = jsUrl
        Unit
	}

	suspend fun loadCanvas(jsUrl: String): HTMLCanvasElementLike {
		return imageToCanvas(loadImage(jsUrl))
	}
}

abstract external class CanvasRenderingContext2DEx : CanvasRenderingContext2D {
    val createConicGradient: ((startAngle: Double, x: Double, y: Double) -> CanvasGradient)?
}

class CanvasContext2dRenderer(private val canvas: HTMLCanvasElementLike) : Renderer() {
	override val width: Int get() = canvas.width.toInt()
	override val height: Int get() = canvas.height.toInt()

	val ctx = canvas.getContext("2d")!!.unsafeCast<CanvasRenderingContext2DEx>()

    fun CanvasGradient.addColors(paint: GradientPaint): CanvasGradient {
        val grad = this
        for (n in 0 until paint.stops.size) {
            val stop = paint.stops.getAt(n)
            val color = paint.colors.getAt(n)
            grad.addColorStop(stop, RGBA(color).htmlStringSimple)
        }
        return this
    }

    fun Paint.toJsStr(): JsAny? {
		return when (this) {
			is NonePaint -> "none".toJsString()
			is ColorPaint -> this.color.htmlColor.toJsString()
			is GradientPaint -> {
				when (kind) {
					GradientKind.LINEAR -> {
						ctx.createLinearGradient(this.x0, this.y0, this.x1, this.y1).addColors(this)
					}
                    GradientKind.RADIAL -> {
						ctx.createRadialGradient(this.x0, this.y0, this.r0, this.x1, this.y1, this.r1).addColors(this)
					}
                    GradientKind.SWEEP -> {
                        when {
                            ctx.createConicGradient != null -> ctx.createConicGradient!!(this.startAngle.radians, this.x0, this.y0).unsafeCast<CanvasGradient>().addColors(this)
                            else -> "fuchsia".toJsString()
                        }
                    }
				}
			}
			is BitmapPaint -> {
				ctx.createPattern(this.bitmap.toHtmlNative().texSource.unsafeCast<CanvasImageSourceJs>(), when {
                    repeatX && repeatY -> "repeat"
                    repeatX -> "repeat-x"
                    repeatY -> "repeat-y"
                    else -> "no-repeat"
                })
				//ctx.call("createPattern", this.bitmap.toHtmlNative().canvas)
			}
			else -> "black".toJsString()
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
    private var cachedFontName: String? = null
	private fun setFont(font: Font?, fontSize: Double) {
        if (font?.name == cachedFontName && fontSize == cachedFontSize) return
        cachedFontName = font?.name
        cachedFontSize = fontSize
		ctx.font = "${fontSize}px '${font?.name ?: "default"}'"
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
		ctx.globalAlpha = state.globalAlpha.toDouble()
        ctx.globalCompositeOperation = state.globalCompositeOperation.toJsStr()
        if (doSetFont) {
            setFont(state.font, state.fontSize.toDouble())
        }
		if (fill) {
            ctx.fillStyle = state.fillStyle.toJsStr()
		} else {
            ctx.lineWidth = state.lineWidth.toDouble()
			ctx.lineJoin = when (state.lineJoin) {
				//LineJoin.BEVEL -> CanvasLineJoin.BEVEL // @TODO: WASM BUG
				//LineJoin.MITER -> CanvasLineJoin.MITER // @TODO: WASM BUG
				//LineJoin.ROUND -> CanvasLineJoin.ROUND // @TODO: WASM BUG
                LineJoin.BEVEL -> "bevel".cast<CanvasLineJoin>()
                LineJoin.MITER -> "miter".cast<CanvasLineJoin>()
                LineJoin.ROUND -> "round".cast<CanvasLineJoin>()
			}
			ctx.lineCap = when (state.lineCap) {
				//LineCap.BUTT -> CanvasLineCap.BUTT // @TODO: WASM BUG
				//LineCap.ROUND -> CanvasLineCap.ROUND // @TODO: WASM BUG
				//LineCap.SQUARE -> CanvasLineCap.SQUARE // @TODO: WASM BUG
                LineCap.BUTT -> "butt".cast<CanvasLineCap>()
                LineCap.ROUND -> "round".cast<CanvasLineCap>()
                LineCap.SQUARE -> "square".cast<CanvasLineCap>()
			}
            ctx.strokeStyle = state.strokeStyle.toJsStr()
		}
	}

    fun CanvasTransform.transform(m: Matrix) {
        transform(m.a.toDouble(), m.b.toDouble(), m.c.toDouble(), m.d.toDouble(), m.tx.toDouble(), m.ty.toDouble())
    }

	private fun transformPaint(paint: Paint) {
		if (paint is TransformedPaint) {
            //ctx.transform(paint.transform.inverted())
            ctx.transform(paint.transform)
            //println("Transformed paint: $m")
		}
	}

	override fun drawImage(image: Bitmap, pos: Point, size: Size, transform: Matrix) {
		ctx.save()
		try {
			transform.run { ctx.setTransform(a.toDouble(), b.toDouble(), c.toDouble(), d.toDouble(), tx.toDouble(), ty.toDouble()) }
			ctx.drawImage(
				(image.ensureNative() as WasmHtmlNativeImage).texSource.unsafeCast<CanvasImageSourceJs>(),
                pos.x, pos.y, size.width, size.height
			)
		} finally {
			ctx.restore()
		}
	}

    fun doVisit(path: VectorPath) {
        path.visitCmds(
            moveTo = { (x, y) -> ctx.moveTo(x.toDouble(), y.toDouble()) },
            lineTo = { (x, y) -> ctx.lineTo(x.toDouble(), y.toDouble()) },
            quadTo = { (cx, cy), (ax, ay) -> ctx.quadraticCurveTo(cx.toDouble(), cy.toDouble(), ax.toDouble(), ay.toDouble()) },
            cubicTo = { (cx1, cy1), (cx2, cy2), (ax, ay) -> ctx.bezierCurveTo(cx1.toDouble(), cy1.toDouble(), cx2.toDouble(), cy2.toDouble(), ax.toDouble(), ay.toDouble()) },
            close = { ctx.closePath() }
        )
    }

	override fun renderFinal(state: Context2d.State, fill: Boolean, winding: Winding?) {
		if (state.path.isEmpty()) return

		//println("beginPath")
        //println("RENDER: $width,$height,fill=$fill")
        //println(" fillStyle=${ctx.fillStyle}, transform=${state.transform}")
		keep {
			setState(state, fill, doSetFont = false)

            val clip = state.clip
            if (clip != null) {
                ctx.beginPath()
                doVisit(clip)
                ctx.clip((winding ?: clip.winding).toCanvasFillRule())
            }

			ctx.beginPath()

            doVisit(state.path)

            ctx.transform(state.transform)
			if (fill) {
				transformPaint(state.fillStyle)
                //println("       - Gadient: ${}")
				ctx.fill((winding ?: state.path.winding).toCanvasFillRule())
				//println("fill: $s")
			} else {
				transformPaint(state.strokeStyle)
                val lineDash = state.lineDash
                if (lineDash != null) {
                    ctx.lineDashOffset = state.lineDashOffset.toDouble()
                    ctx.setLineDash(lineDash.map { it.toDouble() }.mapToJsArray { it.toJsNumber() })
                }
                ctx.stroke()
				//println("stroke: $s")
			}
            Unit
		}
	}

    fun Winding.toCanvasFillRule() = when (this) {
        //Winding.NON_ZERO -> CanvasFillRule.NONZERO // @TODO: Circumvents WASM issue
        //Winding.EVEN_ODD -> CanvasFillRule.EVENODD // @TODO: Circumvents WASM issue
        Winding.NON_ZERO -> NONZERO() // @TODO: Circumvents WASM issue
        Winding.EVEN_ODD -> EVENODD() // @TODO: Circumvents WASM issue
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

@JsFun("() => { return 'nonzero'; }") external private fun NONZERO(): CanvasFillRule
@JsFun("() => { return 'evenodd'; }") external private fun EVENODD(): CanvasFillRule
fun <T : JsAny> String.cast(): T {
    return this.toJsString().unsafeCast()
}
