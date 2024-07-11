package samples

import korlibs.image.format.*
import korlibs.io.file.std.*
import korlibs.korge.animate.*
import korlibs.korge.scene.*
import korlibs.korge.tween.*
import korlibs.korge.view.*
import korlibs.korge.view.filter.*
import korlibs.math.geom.*
import korlibs.math.interpolation.*
import korlibs.time.*

class MainFiltersSample : Scene() {
    override suspend fun SContainer.sceneMain() {
        val bitmap = resourcesVfs["korge.png"].readBitmap()

        val wave = WaveFilter(crestDistance = Vector2D(200, 200), amplitude = Vector2D(24, 24))
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
        val color = TransitionFilter(TransitionFilter.Transition.SWEEP, reversed = false, spread = 1.0)
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

        //addUpdater { invalidate() }

        animate(parallel = true) {
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
                tween(color::ratio[Ratio.ZERO], time = 1.seconds, easing = Easing.EASE_IN_OUT)
                tween(color::ratio[Ratio.ONE], time = 1.seconds, easing = Easing.EASE_IN_OUT)
            }
            sequence(looped = true) {
                tween(page::hratio[Ratio.ZERO], time = 1.seconds, easing = Easing.EASE_IN_OUT)
                tween(page::hratio[Ratio.ONE], time = 1.seconds, easing = Easing.EASE_IN_OUT)
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
