package com.soywiz.korim.format

import com.soywiz.korim.bitmap.*
import com.soywiz.korim.color.*
import org.khronos.webgl.*
import org.w3c.dom.*

// @TODO: BrowserImage and HtmlImage should be combined!
object HtmlImage {
	fun createHtmlCanvas(width: Int, height: Int): HTMLCanvasElementLike {
		return HtmlCanvas.createCanvas(width, height)
	}

	fun renderToHtmlCanvas(
		bmpData: RgbaArray,
		bmpWidth: Int,
		bmpHeight: Int,
		canvas: HTMLCanvasElementLike
	): HTMLCanvasElementLike {
		val pixelCount = bmpData.size
		val ctx = canvas.getContext("2d").unsafeCast<CanvasRenderingContext2D>()
		val idata = ctx.createImageData(bmpWidth.toDouble(), bmpHeight.toDouble())
		val idataData = idata.data
		var m = 0
		for (n in 0 until pixelCount) {
			val c = bmpData[n]

			// @TODO: Kotlin.JS bug Clamped Array should be int inst@TODO: Kotlin.JS bug Clamped Array should be int instead of Byte
			idataData[m++] = c.r.asDynamic()
			idataData[m++] = c.g.asDynamic()
			idataData[m++] = c.b.asDynamic()
			idataData[m++] = c.a.asDynamic()
		}
		ctx.putImageData(idata, 0.0, 0.0)
		return canvas
	}

	fun renderToHtmlCanvas(bmp: Bitmap32, canvas: HTMLCanvasElementLike): HTMLCanvasElementLike =
        renderToHtmlCanvas(bmp.depremultipliedIfRequired().data, bmp.width, bmp.height, canvas)

	fun renderHtmlCanvasIntoBitmap(canvas: HTMLCanvasElementLike, out: RgbaArray): Unit {
		val width = canvas.width
		val height = canvas.height
		val len = width * height
        if (width <= 0 || height <= 0) return
        val ctx = canvas.getContext("2d").unsafeCast<CanvasRenderingContext2D>()
        val data = ctx.getImageData(0.0, 0.0, width.toDouble(), height.toDouble())
        val idata = Int32Array(data.data.buffer).unsafeCast<IntArray>()
        com.soywiz.kmem.arraycopy(idata, 0, out.ints, 0, len)
        //console.log(out);
	}

	fun renderHtmlCanvasIntoBitmap(canvas: HTMLCanvasElementLike, bmp: Bitmap32): Unit {
		renderHtmlCanvasIntoBitmap(canvas, bmp.data)
	}

	fun bitmapToHtmlCanvas(bmp: Bitmap32): HTMLCanvasElementLike {
		return renderToHtmlCanvas(bmp, createHtmlCanvas(bmp.width, bmp.height))
	}

	fun htmlCanvasToDataUrl(canvas: HTMLCanvasElementLike): String = canvas.toDataURL()

	fun htmlCanvasClear(canvas: HTMLCanvasElementLike): Unit {
		val ctx = canvas.getContext("2d").unsafeCast<CanvasRenderingContext2D>()
		ctx.clearRect(
			0.0, 0.0, canvas.width.toDouble(), canvas.height.toDouble()
		)
	}

	//fun htmlCanvasSetSize(canvas: HTMLCanvasElementLike, width: Int, height: Int): HTMLCanvasElementLike {
	//	canvas.width = width
	//	canvas.height = height
	//	return canvas
	//}
}

fun Bitmap.toHtmlNative(): HtmlNativeImage = when (this) {
	is HtmlNativeImage -> this
	else -> HtmlNativeImage(HtmlImage.bitmapToHtmlCanvas(this.toBMP32()))
}
