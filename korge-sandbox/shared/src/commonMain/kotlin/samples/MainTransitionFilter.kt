package samples

import korlibs.image.format.*
import korlibs.io.async.*
import korlibs.io.file.std.*
import korlibs.korge.scene.*
import korlibs.korge.tween.*
import korlibs.korge.view.*
import korlibs.korge.view.filter.*
import korlibs.math.interpolation.*
import korlibs.time.*

class MainTransitionFilter : ScaledScene(768, 512) {
    override suspend fun SContainer.sceneMain() {
        val bitmap = resourcesVfs["korge.png"].readBitmap()
        val transition = TransitionFilter(TransitionFilter.Transition.SWEEP, reversed = false, spread = 1.0, ratio = Ratio.HALF)

        image(transition.transition.bmp)

        image(bitmap) {
            scale(.5)
            position(256, 128)
            filter = transition
        }

        launchImmediately {
            while (true) {
                tween(transition::ratio[Ratio.ONE], time = 0.5.seconds)
                tween(transition::ratio[Ratio.ZERO], time = 0.5.seconds)
            }
        }
    }
}
