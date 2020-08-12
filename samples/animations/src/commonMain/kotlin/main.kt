import com.soywiz.klock.*
import com.soywiz.korge.*
import com.soywiz.korge.view.*
import com.soywiz.korim.color.*
import com.soywiz.korge.animate.*
import com.soywiz.korge.input.*
import com.soywiz.korio.async.*
import com.soywiz.korio.lang.*
import kotlinx.coroutines.*

suspend fun main() = Korge(width = 512, height = 512, virtualWidth = 512, virtualHeight = 512) {
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
