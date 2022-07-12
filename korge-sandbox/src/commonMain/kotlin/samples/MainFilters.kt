package samples

import com.soywiz.klock.seconds
import com.soywiz.korge.Korge
import com.soywiz.korge.animate.animateParallel
import com.soywiz.korge.scene.ScaledScene
import com.soywiz.korge.tween.get
import com.soywiz.korge.view.SContainer
import com.soywiz.korge.view.filter.BlurFilter
import com.soywiz.korge.view.filter.Convolute3Filter
import com.soywiz.korge.view.filter.PageFilter
import com.soywiz.korge.view.filter.SwizzleColorsFilter
import com.soywiz.korge.view.filter.TransitionFilter
import com.soywiz.korge.view.filter.WaveFilter
import com.soywiz.korge.view.filter.filter
import com.soywiz.korge.view.image
import com.soywiz.korge.view.position
import com.soywiz.korge.view.scale
import com.soywiz.korim.color.Colors
import com.soywiz.korim.format.readBitmap
import com.soywiz.korio.file.std.resourcesVfs
import com.soywiz.korma.interpolation.Easing

class MainFilters : ScaledScene(768, 512) {
    override suspend fun SContainer.sceneMain() {
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

        //val color = ColorMatrixFilter(ColorMatrixFilter.GRAYSCALE_MATRIX)
        //val color = TransitionFilter(TransitionFilter.Transition.DIAGONAL1, reversed = false)
        val color = TransitionFilter(TransitionFilter.Transition.SWEEP, reversed = false, smooth = true)
        //val color = TransitionFilter(TransitionFilter.Transition.CIRCULAR, reversed = false)
        //val color = TransitionFilter(time = 1.0)
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
                //tween(color::blendRatio[0], time = 1.seconds, easing = Easing.EASE_IN_OUT)
                //tween(color::blendRatio[1], time = 1.seconds, easing = Easing.EASE_IN_OUT)
                tween(color::ratio[0], time = 1.seconds, easing = Easing.EASE_IN_OUT)
                tween(color::ratio[1], time = 1.seconds, easing = Easing.EASE_IN_OUT)
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
}
