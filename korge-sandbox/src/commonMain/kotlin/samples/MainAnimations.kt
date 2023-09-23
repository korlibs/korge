package samples

import korlibs.event.*
import korlibs.image.color.*
import korlibs.io.async.*
import korlibs.korge.animate.*
import korlibs.korge.input.*
import korlibs.korge.scene.*
import korlibs.korge.view.*
import korlibs.time.*
import kotlinx.coroutines.*

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
                animate(completeOnCancel = false) {
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
