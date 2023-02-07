package samples

import com.soywiz.klock.*
import com.soywiz.korge.scene.*
import com.soywiz.korge.tween.*
import com.soywiz.korge.view.*
import com.soywiz.korge.view.filter.*
import com.soywiz.korim.format.*
import com.soywiz.korio.async.*
import com.soywiz.korio.file.std.*

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
