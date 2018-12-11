import com.soywiz.korge.Korge
import com.soywiz.korge.view.filter.Convolute3Filter
import com.soywiz.korge.view.image
import com.soywiz.korge.view.solidRect
import com.soywiz.korim.bitmap.Bitmap32
import com.soywiz.korim.color.Colors
import com.soywiz.korim.color.RGBA
import kotlin.jvm.JvmStatic

object Sample1 {
	@JvmStatic
	fun main(args: Array<String>) = Korge(title = "Sample1") {
		//waveEffectView {
		//colorMatrixEffectView(ColorMatrixEffectView.GRAYSCALE_MATRIX) {
		//convolute3EffectView(Convolute3EffectView.KERNEL_EDGE_DETECTION) {
		/*
		blurEffectView(radius = 1.0) {
			convolute3EffectView(Convolute3EffectView.KERNEL_GAUSSIAN_BLUR) {
				//convolute3EffectView(Convolute3EffectView.KERNEL_BOX_BLUR) {
				swizzleColorsEffectView("bgra") {
					x = 100.0
					y = 100.0
					image(Bitmap32(100, 100) { x, y -> RGBA(156 + x, 156 + y, 0, 255) })
					//solidRect(100, 100, Colors.RED)
				}
				//}
			}
		}.apply {
			tween(this::radius[10.0], time = 5.seconds)
		}
		*/

		//val mfilter = ColorMatrixFilter(ColorMatrixFilter.GRAYSCALE_MATRIX, 0.0)
		//val mfilter = WaveFilter()
		val mfilter = Convolute3Filter(Convolute3Filter.KERNEL_GAUSSIAN_BLUR)
		solidRect(640, 480, Colors.ALICEBLUE)
		image(Bitmap32(100, 100) { x, y -> RGBA(156 + x, 156 + y, 0, 255) }) {
			x = 100.0
			y = 100.0
			//filter = ComposedFilter(SwizzleColorsFilter("bgra"), SwizzleColorsFilter("bgra"))
			//filter = ComposedFilter(
			//	SwizzleColorsFilter("bgra"),
			//	Convolute3Filter(Convolute3Filter.KERNEL_GAUSSIAN_BLUR),
			//	Convolute3Filter(Convolute3Filter.KERNEL_EDGE_DETECTION)
			//)
			//filter = ComposedFilter(mfilter, Convolute3Filter(Convolute3Filter.KERNEL_GAUSSIAN_BLUR))
			alpha = 1.0
			filter = mfilter
			//filter = WaveFilter()
		}.apply {
			//mfilter.amplitudeY = 6
			//mfilter.amplitudeX = 0
			//mfilter.time = 0.5
			//tween(mfilter::time[0.0, 10.0], time = 10.seconds)
			//tween(mfilter::blendRatio[0.0, 1.0], time = 4.seconds)
		}
		//val bmp = SolidRect(100, 100, Colors.RED).renderToBitmap(views)
		//val bmp = view.renderToBitmap(views)
		//bmp.writeTo("/tmp/demo.png".uniVfs, defaultImageFormats)
		//println(bmp)
	}
}
