import com.soywiz.klock.*
import com.soywiz.korge.*
import com.soywiz.korge.animate.*
import com.soywiz.korge.tween.*
import com.soywiz.korge.view.*
import com.soywiz.korge.view.filter.*
import com.soywiz.korim.color.*
import com.soywiz.korim.format.*
import com.soywiz.korio.file.std.*
import com.soywiz.korma.interpolation.*

suspend fun main() = Korge(width = 768, height = 512, bgcolor = Colors["#2b2b2b"]) {
	val bitmap = resourcesVfs["korge.png"].readBitmap()

	val wave = WaveFilter()
	image(bitmap) {
		scale(.5)
		position(0, 0)
		filter = wave
	}

	val blur = BlurFilter()
	image(bitmap) {
		scale(.5)
		position(256, 0)
		filter = blur
	}

	val color = ColorMatrixFilter(ColorMatrixFilter.GRAYSCALE_MATRIX)
	image(bitmap) {
		scale(.5)
		position(512, 0)
		filter = color
	}

	val page = PageFilter()
	image(bitmap) {
		scale(.5)
		position(0, 256)
		filter = page
	}

	val conImg = image(bitmap) {
		scale(.5)
		position(256, 256)
		filter = Convolute3Filter(Convolute3Filter.KERNEL_SHARPEN)
	}

	val swizzle = SwizzleColorsFilter()
	image(bitmap) {
		scale(.5)
		position(512, 256)
		filter = swizzle
	}

	animateParallel {
		sequence(looped = true) {
			tween(wave::time[1.seconds], time = 1.seconds, easing = Easing.EASE_IN_OUT)
			tween(wave::time[0.seconds], time = 1.seconds, easing = Easing.EASE_IN_OUT)
		}
		sequence(looped = true) {
			tween(blur::radius[16], time = 1.seconds, easing = Easing.EASE_IN_OUT)
			tween(blur::radius[0], time = 1.seconds, easing = Easing.EASE_IN_OUT)
		}
		sequence(looped = true) {
			tween(color::blendRatio[0], time = 1.seconds, easing = Easing.EASE_IN_OUT)
			tween(color::blendRatio[1], time = 1.seconds, easing = Easing.EASE_IN_OUT)
		}
		sequence(looped = true) {
			tween(page::hratio[0], time = 1.seconds, easing = Easing.EASE_IN_OUT)
			tween(page::hratio[1], time = 1.seconds, easing = Easing.EASE_IN_OUT)
		}
		sequence(looped = true) {
			block { conImg.filter = Convolute3Filter(Convolute3Filter.KERNEL_SHARPEN) }
			wait(1.seconds)
			block { conImg.filter = Convolute3Filter(Convolute3Filter.KERNEL_IDENTITY) }
			wait(1.seconds)
		}
		sequence(looped = true) {
			arrayOf("rgga", "bgga", "bgba", "grba", "gbba", "gbga", "bbga").forEach {
				block { swizzle.swizzle = it }
				wait(0.5.seconds)
			}
		}
	}
}
