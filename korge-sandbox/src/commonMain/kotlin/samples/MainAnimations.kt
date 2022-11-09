package samples

import com.soywiz.klock.*
import com.soywiz.korev.*
import com.soywiz.korge.animate.*
import com.soywiz.korge.input.*
import com.soywiz.korge.scene.ScaledScene
import com.soywiz.korge.view.SContainer
import com.soywiz.korge.view.position
import com.soywiz.korge.view.solidRect
import com.soywiz.korim.color.Colors
import com.soywiz.korio.async.*
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

        val signal = Signal<Unit>()

        keys {
            down(Key.RETURN) { signal.invoke() }
        }

        val time = Stopwatch()

        while (true) {
            println("START!")
            time.start()
            job = launchImmediately {
                newAnimate(completeOnCancel = false) {
                //animate(completeOnCancel = false) {
                    //animate(completeOnCancel = true) {
                    //animate {
                    sequence(defaultTime = 1.seconds, defaultSpeed = 256.0) {
                        //wait(0.25.seconds)
                        block {
                            println("[0] ${time.elapsed}")
                        }
                        parallel {
                            //rect1.moveTo(0, 150)
                            moveToWithSpeed(rect1, width - 100, 0.0)
                            moveToWithSpeed(rect2, 0.0, height - 100 - 100)
                            //rect1.moveTo(0, height - 100)
                        }
                        block {
                            println("[1] ${time.elapsed}")
                        }
                        parallel {
                            //rect1.moveTo(0, 150)
                            moveTo(rect1, width - 100, height - 100)
                            moveTo(rect2, width - 100, height - 100)
                            //rect1.moveTo(0, height - 100)
                        }
                        block {
                            println("[2] ${time.elapsed}")
                        }
                        parallel(time = 1.seconds) {
                            //alpha(rect1, 0.5)
                            //alpha(rect2, 0.5)
                            hide(rect1)
                            hide(rect2)
                        }
                        block {
                            println("[3] ${time.elapsed}")
                        }
                        block {
                            //printStackTrace()
                            //println("ZERO")
                            rect1.position(0, 0)
                            rect2.position(0, 0)
                        }
                        block {
                            println("[4] ${time.elapsed}")
                        }
                        parallel(time = 0.5.seconds) {
                            show(rect1)
                            show(rect2)
                        }
                        block {
                            println("[5] ${time.elapsed}")
                        }
                        wait(0.25.seconds)
                        block {
                            println("[6] ${time.elapsed}")
                        }
                    }
                }
            }
            job.join()
            //signal.waitOne()
            //println("[a]")
            //delay(1.seconds)
            //println("[b]")
            //job.cancel()
        }
    }
}
