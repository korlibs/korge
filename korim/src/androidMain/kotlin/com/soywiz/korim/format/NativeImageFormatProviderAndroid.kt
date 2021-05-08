package com.soywiz.korim.format

import android.app.*
import android.graphics.*
import android.graphics.Paint
import android.text.*
import android.view.*
import android.widget.*
import com.soywiz.kds.*
import com.soywiz.kmem.*
import com.soywiz.korio.android.androidContext
import com.soywiz.korim.bitmap.*
import com.soywiz.korim.bitmap.Bitmap
import com.soywiz.korim.color.*
import com.soywiz.korim.paint.*
import com.soywiz.korim.vector.*
import com.soywiz.korma.geom.vector.*
import kotlinx.coroutines.*

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

    override suspend fun decode(data: ByteArray, premultiplied: Boolean): NativeImage =
		AndroidNativeImage(BitmapFactory.decodeByteArray(data, 0, data.size, BitmapFactory.Options().apply { this.inPremultiplied = premultiplied }))

	override fun create(width: Int, height: Int, premultiplied: Boolean?): NativeImage {
		val bmp = android.graphics.Bitmap.createBitmap(width.coerceAtLeast(1), height.coerceAtLeast(1), android.graphics.Bitmap.Config.ARGB_8888)
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

fun Bitmap.toAndroidBitmap(): android.graphics.Bitmap {
    if (this is AndroidNativeImage) return this.androidBitmap
    val bmp32 = this.toBMP32()
    return android.graphics.Bitmap.createBitmap(
        bmp32.data.ints,
        0,
        bmp32.width,
        bmp32.width,
        bmp32.height,
        android.graphics.Bitmap.Config.ARGB_8888
    )
}

class AndroidNativeImage(val androidBitmap: android.graphics.Bitmap) :
    NativeImage(androidBitmap.width, androidBitmap.height, androidBitmap, premultiplied = androidBitmap.isPremultiplied()) {
    override val name: String = "AndroidNativeImage"

    override fun readPixelsUnsafe(x: Int, y: Int, width: Int, height: Int, out: RgbaArray, offset: Int) {
        androidBitmap.getPixels(out.ints, offset, this.width, x, y, width, height)
        BGRA.bgraToRgba(out.ints, offset, width * height)
    }
    override fun writePixelsUnsafe(x: Int, y: Int, width: Int, height: Int, out: RgbaArray, offset: Int) {
        BGRA.rgbaToBgra(out.ints, offset, width * height)
        androidBitmap.setPixels(out.ints, offset, this.width, x, y, width, height)
        BGRA.bgraToRgba(out.ints, offset, width * height)
    }
    override fun setRgba(x: Int, y: Int, v: RGBA) { androidBitmap.setPixel(x, y, v.value) }
    override fun getRgba(x: Int, y: Int): RGBA = RGBA(androidBitmap.getPixel(x, y))

    override fun getContext2d(antialiasing: Boolean): Context2d = Context2d(AndroidContext2dRenderer(androidBitmap, antialiasing))
}

class AndroidContext2dRenderer(val bmp: android.graphics.Bitmap, val antialiasing: Boolean) : com.soywiz.korim.vector.renderer.Renderer() {
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

    fun GraphicsPath.toAndroid(out: Path = Path()): Path {
        //out.reset()
        out.rewind()

        out.fillType = when (this.winding) {
            Winding.EVEN_ODD -> Path.FillType.EVEN_ODD
            Winding.NON_ZERO -> Path.FillType.INVERSE_EVEN_ODD
            else -> Path.FillType.EVEN_ODD
        }
        //kotlin.io.println("Path:")
        this.visitCmds(
            moveTo = { x, y -> out.moveTo(x.toFloat(), y.toFloat()) },
            lineTo = { x, y -> out.lineTo(x.toFloat(), y.toFloat()) },
            quadTo = { cx, cy, ax, ay -> out.quadTo(cx.toFloat(), cy.toFloat(), ax.toFloat(), ay.toFloat()) },
            cubicTo = { cx1, cy1, cx2, cy2, ax, ay ->
                out.cubicTo(
                    cx1.toFloat(),
                    cy1.toFloat(),
                    cx2.toFloat(),
                    cy2.toFloat(),
                    ax.toFloat(),
                    ay.toFloat()
                )
            },
            close = { out.close() }
        )
        //kotlin.io.println("/Path")
        return out
    }

    fun CycleMethod.toTileMode() = when (this) {
        CycleMethod.NO_CYCLE -> Shader.TileMode.CLAMP
        CycleMethod.REFLECT -> Shader.TileMode.MIRROR
        CycleMethod.REPEAT -> Shader.TileMode.REPEAT
    }

    @Suppress("RemoveRedundantQualifierName")
    fun convertPaint(c: com.soywiz.korim.paint.Paint, m: com.soywiz.korma.geom.Matrix, out: Paint, alpha: Double) {
        when (c) {
            is com.soywiz.korim.paint.NonePaint -> {
                out.shader = null
            }
            is com.soywiz.korim.paint.ColorPaint -> {
                val cc = c.color.withScaledAlpha(alpha)
                out.setARGB(cc.a, cc.r, cc.g, cc.b)
                out.shader = null
            }
            is com.soywiz.korim.paint.GradientPaint -> {
                out.shader = when (c.kind) {
                    GradientKind.LINEAR ->
                        LinearGradient(
                            c.x0(m).toFloat(), c.y0(m).toFloat(),
                            c.x1(m).toFloat(), c.y1(m).toFloat(),
                            c.colors.toColorScaledAlpha(alpha), c.stops.toFloatArray(), c.cycle.toTileMode()
                        )
                    GradientKind.RADIAL ->
                        RadialGradient(
                            c.x1(m).toFloat(), c.y1(m).toFloat(), c.r1(m).toFloat(),
                            c.colors.toColorScaledAlpha(alpha), c.stops.toFloatArray(), c.cycle.toTileMode()
                        )
                    GradientKind.SWEEP ->
                        SweepGradient(
                            c.x0(m).toFloat(), c.y0(m).toFloat(),
                            c.colors.toColorScaledAlpha(alpha), c.stops.toFloatArray()
                        )
                    else -> null
                }
            }
            is com.soywiz.korim.paint.BitmapPaint -> {
                val androidBitmap = c.bitmap.toAndroidBitmap()
                val colorAlpha = Colors.WHITE.withAd(alpha)
                val shaderA = LinearGradient(0f, 0f, androidBitmap.width.toFloat(), 0f, colorAlpha.value, colorAlpha.value, Shader.TileMode.CLAMP)
                val shaderB = BitmapShader(androidBitmap, c.cycleX.toTileMode(), c.cycleY.toTileMode())
                out.shader =  ComposeShader(shaderA, shaderB, PorterDuff.Mode.SRC_IN)
            }
        }
    }

    fun DoubleArrayList.toFloatArray() = map(Double::toFloat).toFloatArray()
    fun IntArrayList.toColorScaledAlpha(alpha: Double) = mapInt { RGBA(it).withScaledAlpha(alpha).value }.toIntArray()
    fun RGBA.withScaledAlpha(scale: Double): RGBA = this.withA((this.a * scale.clamp01()).toInt())

    inline fun <T> keep(callback: () -> T): T {
        canvas.save()
        try {
            return callback()
        } finally {
            canvas.restore()
        }
    }

    fun android.graphics.Matrix.setTo(m: com.soywiz.korma.geom.Matrix) = this.apply {
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

    override fun render(state: Context2d.State, fill: Boolean) {
        setState(state, fill)

        keep {
            if (state.clip != null) {
                canvas.clipPath(state.clip!!.toAndroid(androidClipPath))
            }

            paint.style = if (fill) Paint.Style.FILL else android.graphics.Paint.Style.STROKE
            convertPaint(state.fillOrStrokeStyle(fill), state.transform, paint, state.globalAlpha.clamp01())
            paint.isAntiAlias = antialiasing

            //println("-----------------")
            //println(canvas.matrix)
            //println(state.path.toAndroid())
            //println(paint.style)
            //println(paint.color)
            canvas.drawPath(state.path.toAndroid(androidPath), paint)
        }
    }
}

