package samples

import korlibs.time.*
import korlibs.korge.scene.*
import korlibs.korge.tween.*
import korlibs.korge.view.*
import korlibs.korge.view.filter.*
import korlibs.image.format.*
import korlibs.io.async.*
import korlibs.io.file.std.*

class MainTransitionFilter : ScaledScene(768, 512) {
    override suspend fun SContainer.sceneMain() {
        val bitmap = resourcesVfs["korge.png"].readBitmap()
        val transition = TransitionFilter(TransitionFilter.Transition.SWEEP, reversed = false, spread = 1.0, ratio = 0.5)

        image(transition.transition.bmp)

        image(bitmap) {
            scale(.5)
            position(256, 128)
            filter = transition
        }

        launchImmediately {
            while (true) {
                tween(transition::ratio[1.0], time = 0.5.seconds)
                tween(transition::ratio[0.0], time = 0.5.seconds)
            }
        }
    }
}