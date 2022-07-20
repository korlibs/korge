package samples

import com.soywiz.klock.seconds
import com.soywiz.korge.animate.AnimateCancellationException
import com.soywiz.korge.animate.animate
import com.soywiz.korge.input.onClick
import com.soywiz.korge.scene.ScaledScene
import com.soywiz.korge.scene.Scene
import com.soywiz.korge.view.SContainer
import com.soywiz.korge.view.position
import com.soywiz.korge.view.solidRect
import com.soywiz.korim.color.Colors
import com.soywiz.korio.async.launchImmediately
import kotlinx.coroutines.Job

class MainAnimations : ScaledScene(512, 512) {
    override suspend fun SContainer.sceneMain() {
        //println("width=$width, height=$height")
        val rect1 = solidRect(100, 100, Colors.RED)
        val rect2 = solidRect(100, 100, Colors.BLUE)

        var job: Job? = null

        onClick {
            //println("CLICK: ${it.button}")
            if (it.button.isRight) {
                job?.cancel(AnimateCancellationException(completeOnCancel = true))
            } else {
                job?.cancel(AnimateCancellationException(completeOnCancel = false))
            }
        }

        while (true) {
            job = launchImmediately {
                animate(completeOnCancel = false) {
                    //animate(completeOnCancel = true) {
                    //animate {
                    sequence(time = 1.seconds, speed = 256.0) {
                        //wait(0.25.seconds)
                        parallel {
                            //rect1.moveTo(0, 150)
                            rect1.moveToWithSpeed(width - 100, 0.0)
                            rect2.moveToWithSpeed(0.0, height - 100 - 100)
                            //rect1.moveTo(0, height - 100)
                        }
                        parallel {
                            //rect1.moveTo(0, 150)
                            rect1.moveTo(width - 100, height - 100)
                            rect2.moveTo(width - 100, height - 100)
                            //rect1.moveTo(0, height - 100)
                        }
                        parallel(time = 1.seconds) {
                            rect1.hide()
                            rect2.hide()
                        }
                        block {
                            //printStackTrace()
                            //println("ZERO")
                            rect1.position(0, 0)
                            rect2.position(0, 0)
                        }
                        parallel(time = 0.5.seconds) {
                            rect1.show()
                            rect2.show()
                        }
                        wait(0.25.seconds)
                    }
                }
            }
            job.join()
            //println("[a]")
            //delay(1.seconds)
            //println("[b]")
            //job.cancel()
        }
    }
}
