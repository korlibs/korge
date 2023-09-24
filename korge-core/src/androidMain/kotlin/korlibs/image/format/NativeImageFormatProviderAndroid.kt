package korlibs.image.format

import android.app.*
import android.graphics.*
import android.graphics.Matrix
import android.graphics.Paint
import android.os.*
import android.text.*
import android.view.*
import android.widget.*
import korlibs.datastructure.*
import korlibs.image.bitmap.*
import korlibs.image.bitmap.Bitmap
import korlibs.image.color.*
import korlibs.image.paint.*
import korlibs.image.vector.*
import korlibs.io.android.*
import korlibs.math.*
import korlibs.math.geom.*
import korlibs.math.geom.vector.*
import kotlinx.coroutines.*
import java.io.*

actual val nativeImageFormatProvider: NativeImageFormatProvider by lazy {
    try {
        //     java.lang.RuntimeException: Method createBitmap in android.graphics.Bitmap not mocked. See http://g.co/androidstudio/not-mocked for details.
        android.graphics.Bitmap.createBitmap(1, 1, android.graphics.Bitmap.Config.ARGB_8888)
        AndroidNativeImageFormatProvider
    } catch (e: RuntimeException) {
        BaseNativeImageFormatProvider()
    }
}

object AndroidNativeImageFormatProvider : NativeImageFormatProvider() {
    override suspend fun encodeSuspend(image: ImageDataContainer, props: ImageEncodingProps): ByteArray {
        val compressFormat = when (props.mimeType) {
            "image/png" -> android.graphics.Bitmap.CompressFormat.PNG
            "image/jpeg", "image/jpg" -> android.graphics.Bitmap.CompressFormat.JPEG
            "image/webp" -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                android.graphics.Bitmap.CompressFormat.WEBP_LOSSY
            } else {
                android.graphics.Bitmap.CompressFormat.PNG
            }
            else -> android.graphics.Bitmap.CompressFormat.PNG
        }
        return ByteArrayOutputStream().use { bao ->
            image.mainBitmap.toAndroidBitmap().compress(compressFormat, (props.quality * 100).toInt(), bao)
            bao.toByteArray()
        }
    }

    override suspend fun display(bitmap: Bitmap, kind: Int) {
        val ctx = androidContext()
        val androidBitmap = bitmap.toAndroidBitmap()
        val deferred = CompletableDeferred<Unit>()
        (ctx as Activity).runOnUiThread {
            val settingsDialog = Dialog(ctx)
            settingsDialog.window?.requestFeature(Window.FEATURE_NO_TITLE)
            val rlmain = LinearLayout(ctx)
            val llp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT
            )
            val ll1 = LinearLayout(ctx)

            val iv = ImageView(ctx)
            iv.setBackgroundColor(Colors.BLACK.value)
            iv.setImageBitmap(androidBitmap)
            val lp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT
            )
            iv.layoutParams = lp

            ll1.addView(iv)
            rlmain.addView(ll1)
            settingsDialog.setContentView(rlmain, llp)

            settingsDialog.setOnDismissListener {
                deferred.complete(Unit)
            }
            settingsDialog.show()
        }
        deferred.await()
    }

    override suspend fun decodeHeaderInternal(data: ByteArray): ImageInfo {
        val options = BitmapFactory.Options().also { it.inJustDecodeBounds = true }
        Dispatchers.Default { BitmapFactory.decodeByteArray(data, 0, data.size, options) }
        return ImageInfo().also {
            it.width = options.outWidth
            it.height = options.outHeight
        }
    }

    override suspend fun decodeInternal(data: ByteArray, props: ImageDecodingProps): NativeImageResult {
        val info = decodeHeaderInternal(data)
        val originalSize = MSizeInt(info.width, info.height)

        return NativeImageResult(
            image = AndroidNativeImage(
                Dispatchers.Default {
                    for (setPremult in listOf(true, false)) {
                        val bmp = BitmapFactory.decodeByteArray(
                            data, 0, data.size,
                            BitmapFactory.Options().also {
                                if (setPremult) {
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                                        it.inPremultiplied = when {
                                            props.asumePremultiplied -> false
                                            props.premultipliedSure -> true
                                            else -> false
                                        }
                                    }
                                }
                                it.inSampleSize = props.getSampleSize(originalSize.width, originalSize.height)
                                it.inMutable = true
                            }
                        )
                        if (bmp != null) {
                            return@Default bmp
                        }
                    }
                    error("Couldn't decode image")
                }
            ).also {
               if (props.asumePremultiplied) it.asumePremultiplied()
            },
            originalWidth = info.width,
            originalHeight = info.height
        )
    }

    override fun create(width: Int, height: Int, premultiplied: Boolean?): NativeImage {
		val bmp = android.graphics.Bitmap.createBitmap(
            width.coerceAtLeast(1),
            height.coerceAtLeast(1),
            android.graphics.Bitmap.Config.ARGB_8888,
        )
		//bmp.setPixels()
		return AndroidNativeImage(bmp)
	}

	override fun copy(bmp: Bitmap): NativeImage = AndroidNativeImage(bmp.toAndroidBitmap())
}

/*
suspend fun androidQuestionAlert(message: String, title: String = "Warning"): Boolean = korioSuspendCoroutine { c ->
	KorioAndroidContext.runOnUiThread {
		val dialog = AlertDialog.Builder(KorioAndroidContext)
			.setTitle(title)
			.setMessage(message)
			.setPositiveButton(android.R.string.yes) { dialog, which ->
				c.resume(true)
			}
			.setNegativeButton(android.R.string.no, android.content.DialogInterface.OnClickListener { dialog, which ->
				c.resume(false)
			})
			.setIcon(android.R.drawable.ic_dialog_alert)
			.setCancelable(false)
			.show()

		dialog.show()
	}

 */

// @TODO: PRemultiplied
fun Bitmap.toAndroidBitmap(): android.graphics.Bitmap {
    if (this is AndroidNativeImage) return this.androidBitmap
    val bmp32 = this.toBMP32()
    bmp32.updateColors { RGBA(it.toAndroidColor()) }
    return android.graphics.Bitmap.createBitmap(
        bmp32.ints,
        0,
        bmp32.width,
        bmp32.width,
        bmp32.height,
        android.graphics.Bitmap.Config.ARGB_8888
    )
}

private fun android.graphics.Bitmap.isPremultipliedSafe(): Boolean = when {
    Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1 -> isPremultiplied
    else -> true
}

class AndroidNativeImage(
    val androidBitmap: android.graphics.Bitmap,
    val originalPremultiplied: Boolean = androidBitmap.isPremultipliedSafe()
) : NativeImage(androidBitmap.width, androidBitmap.height, androidBitmap, premultiplied = originalPremultiplied) {
    override val name: String get() = "AndroidNativeImage"

    override fun readPixelsUnsafe(x: Int, y: Int, width: Int, height: Int, out: IntArray, offset: Int) {
        val outRgba = RgbaArray(out)
        androidBitmap.getPixels(out, offset, width, x, y, width, height) // This returns values in straight alpha
        val count = width * height
        AndroidColor.androidToRgba(RgbaArray(out), offset, count)
        if (originalPremultiplied) {
            for (n in offset until offset + count) out[n] = outRgba[n].premultiplied.value
        }
    }
    override fun writePixelsUnsafe(x: Int, y: Int, width: Int, height: Int, out: IntArray, offset: Int) {
        val outRgba = RgbaArray(out)
        val count = width * height
        val temp = RgbaArray(IntArray(count + offset))
        AndroidColor.rgbaToAndroid(outRgba, offset, count, temp)
        if (originalPremultiplied) {
            for (n in offset until offset + count) out[n] = outRgba[n].asPremultiplied().depremultiplied.value
        }
        androidBitmap.setPixels(temp.ints, 0, width, x, y, width, height) // This expects values in straight alpha
    }

    override fun getContext2d(antialiasing: Boolean): Context2d = Context2d(AndroidContext2dRenderer(androidBitmap, antialiasing))
}

class AndroidContext2dRenderer(val bmp: android.graphics.Bitmap, val antialiasing: Boolean) : korlibs.image.vector.renderer.Renderer() {
    override val width: Int get() = bmp.width
    override val height: Int get() = bmp.height
    //val paint = TextPaint(TextPaint.ANTI_ALIAS_FLAG).apply {
    val paint =
        Paint(Paint.DITHER_FLAG or Paint.FILTER_BITMAP_FLAG or TextPaint.ANTI_ALIAS_FLAG or TextPaint.SUBPIXEL_TEXT_FLAG).apply {
            hinting = Paint.HINTING_ON
            isAntiAlias = true
            isFilterBitmap = true
            isDither = true
        }
    val canvas = Canvas(bmp)
    val matrixValues = FloatArray(9)
    var androidMatrix = android.graphics.Matrix()

    fun VectorPath.toAndroid(out: Path = Path(), winding: Winding?): Path {
        //out.reset()
        out.rewind()

        out.fillType = when (winding ?: this.winding) {
            Winding.EVEN_ODD -> Path.FillType.EVEN_ODD
            Winding.NON_ZERO -> Path.FillType.WINDING
            //Winding.NON_ZERO -> Path.FillType.INVERSE_EVEN_ODD
        }
        //kotlin.io.println("Path:")
        this.visitCmds(
            moveTo = { (x, y) -> out.moveTo(x.toFloat(), y.toFloat()) },
            lineTo = { (x, y) -> out.lineTo(x.toFloat(), y.toFloat()) },
            quadTo = { (cx, cy), (ax, ay) -> out.quadTo(cx.toFloat(), cy.toFloat(), ax.toFloat(), ay.toFloat()) },
            cubicTo = { (cx1, cy1), (cx2, cy2), (ax, ay) -> out.cubicTo(cx1.toFloat(), cy1.toFloat(), cx2.toFloat(), cy2.toFloat(), ax.toFloat(), ay.toFloat()) },
            close = { out.close() }
        )
        //kotlin.io.println("/Path")
        return out
    }

    fun CycleMethod.toTileMode() = when (this) {
        CycleMethod.NO_CYCLE, CycleMethod.NO_CYCLE_CLAMP -> Shader.TileMode.CLAMP
        CycleMethod.REFLECT -> Shader.TileMode.MIRROR
        CycleMethod.REPEAT -> Shader.TileMode.REPEAT
    }

    @Suppress("RemoveRedundantQualifierName")
    fun convertPaint(c: korlibs.image.paint.Paint, m: korlibs.math.geom.Matrix, out: Paint, alpha: Float) {
        when (c) {
            is korlibs.image.paint.NonePaint -> {
                out.shader = null
            }
            is korlibs.image.paint.ColorPaint -> {
                out.color = c.color.withScaledAlpha(alpha).toAndroidColor()
                out.shader = null
            }
            is korlibs.image.paint.GradientPaint -> {
                val colors = c.colors.toColorScaledAlpha(alpha)
                    .apply {
                        for (n in indices) this[n] = RGBA(this[n]).toAndroidColor()
                    }
                val stops = c.stops.toFloatArray()
                out.shader = when (c.kind) {
                    GradientKind.LINEAR ->
                        LinearGradient(
                            c.x0.toFloat(), c.y0.toFloat(),
                            c.x1.toFloat(), c.y1.toFloat(),
                            colors, stops, c.cycle.toTileMode()
                        )
                    GradientKind.RADIAL ->
                        RadialGradient(
                            c.x1.toFloat(), c.y1.toFloat(), c.r1.toFloat(),
                            colors, stops, c.cycle.toTileMode()
                        )
                    GradientKind.SWEEP ->
                        SweepGradient(c.x0.toFloat(), c.y0.toFloat(), colors, stops)
                    else -> null
                }
            }
            is korlibs.image.paint.BitmapPaint -> {
                val androidBitmap = c.bitmap.toAndroidBitmap()
                val colorAlpha = Colors.WHITE.withAf(alpha)
                val shaderA = LinearGradient(0f, 0f, androidBitmap.width.toFloat(), 0f, colorAlpha.value, colorAlpha.value, Shader.TileMode.REPEAT)
                val shaderB = BitmapShader(androidBitmap, c.cycleX.toTileMode(), c.cycleY.toTileMode())
                out.shader = ComposeShader(shaderA, shaderB, PorterDuff.Mode.SRC_IN)
            }
        }
        // Apply shader transformation
        val mat = if (c is TransformedPaint) c.transform * m else m
        out.shader?.setLocalMatrix(mat.toAndroid())
    }

    fun DoubleArrayList.toFloatArray() = map(Double::toFloat).toFloatArray()
    fun IntArrayList.toColorScaledAlpha(alpha: Float) = mapInt { RGBA(it).withScaledAlpha(alpha).value }.toIntArray()
    fun RGBA.withScaledAlpha(scale: Float): RGBA = this.withA((this.a * scale.clamp01()).toInt())

    inline fun <T> keep(callback: () -> T): T {
        canvas.save()
        try {
            return callback()
        } finally {
            canvas.restore()
        }
    }

    fun korlibs.math.geom.Matrix.toAndroid() = android.graphics.Matrix().setTo(this)

    fun android.graphics.Matrix.setTo(m: korlibs.math.geom.Matrix) = this.apply {
        matrixValues[Matrix.MSCALE_X] = m.a.toFloat()
        matrixValues[Matrix.MSKEW_X] = m.b.toFloat()
        matrixValues[Matrix.MSKEW_Y] = m.c.toFloat()
        matrixValues[Matrix.MSCALE_Y] = m.d.toFloat()
        matrixValues[Matrix.MTRANS_X] = m.tx.toFloat()
        matrixValues[Matrix.MTRANS_Y] = m.ty.toFloat()
        matrixValues[Matrix.MPERSP_0] = 0f
        matrixValues[Matrix.MPERSP_1] = 0f
        matrixValues[Matrix.MPERSP_2] = 1f
        this.setValues(matrixValues)
    }

    fun android.graphics.Matrix.setTo(m: korlibs.math.geom.MMatrix) = this.apply {
        matrixValues[Matrix.MSCALE_X] = m.a.toFloat()
        matrixValues[Matrix.MSKEW_X] = m.b.toFloat()
        matrixValues[Matrix.MSKEW_Y] = m.c.toFloat()
        matrixValues[Matrix.MSCALE_Y] = m.d.toFloat()
        matrixValues[Matrix.MTRANS_X] = m.tx.toFloat()
        matrixValues[Matrix.MTRANS_Y] = m.ty.toFloat()
        matrixValues[Matrix.MPERSP_0] = 0f
        matrixValues[Matrix.MPERSP_1] = 0f
        matrixValues[Matrix.MPERSP_2] = 1f
        this.setValues(matrixValues)
    }

    private fun setState(state: Context2d.State, fill: Boolean) {
        //canvas.matrix = androidMatrix.setTo(state.transform)
        paint.strokeWidth = state.scaledLineWidth.toFloat()
    }

    private val androidClipPath = Path()
    private val androidPath = Path()

    override fun renderFinal(state: Context2d.State, fill: Boolean, winding: Winding?) {
        setState(state, fill)

        keep {
            if (state.clip != null) {
                val clipPath = state.clip!!.toAndroid(androidClipPath, winding)
                canvas.clipPath(clipPath)
            }

            paint.style = if (fill) Paint.Style.FILL else android.graphics.Paint.Style.STROKE
            paint.strokeCap = state.lineCap.toAndroid()
            paint.strokeJoin = state.lineJoin.toAndroid()
            convertPaint(state.fillOrStrokeStyle(fill), state.transform, paint, state.globalAlpha.clamp01().toFloat())
            paint.pathEffect = when {
                state.lineDash != null -> DashPathEffect(state.lineDash!!.mapFloat { it.toFloat() }.toFloatArray(), state.lineDashOffset.toFloat())
                else -> null
            }
            paint.isAntiAlias = antialiasing

            //println("-----------------")
            //println(canvas.matrix)
            //println(state.path.toAndroid())
            //println(paint.style)
            //println(paint.color)

            canvas.drawPath(state.path.toAndroid(androidPath, winding), paint)
        }
    }
}

fun LineCap.toAndroid(): Paint.Cap = when (this) {
    LineCap.BUTT -> Paint.Cap.BUTT
    LineCap.SQUARE -> Paint.Cap.SQUARE
    LineCap.ROUND -> Paint.Cap.ROUND
}

fun LineJoin.toAndroid(): Paint.Join = when (this) {
    LineJoin.BEVEL -> Paint.Join.BEVEL
    LineJoin.ROUND -> Paint.Join.ROUND
    LineJoin.MITER -> Paint.Join.MITER
}
