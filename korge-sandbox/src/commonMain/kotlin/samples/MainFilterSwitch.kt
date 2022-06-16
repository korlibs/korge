package samples

import com.soywiz.klock.seconds
import com.soywiz.klock.timesPerSecond
import com.soywiz.korge.scene.Scene
import com.soywiz.korge.time.delay
import com.soywiz.korge.time.frameBlock
import com.soywiz.korge.view.Container
import com.soywiz.korge.view.Stage
import com.soywiz.korge.view.anchor
import com.soywiz.korge.view.filter
import com.soywiz.korge.view.filter.ColorMatrixFilter
import com.soywiz.korge.view.image
import com.soywiz.korge.view.position
import com.soywiz.korge.view.scale
import com.soywiz.korim.format.readBitmap
import com.soywiz.korio.async.launchImmediately
import com.soywiz.korio.file.std.resourcesVfs
import kotlinx.coroutines.launch
import kotlin.random.Random

class MainFilterSwitch : Scene() {
    override suspend fun Container.sceneMain() {
        val bitmap = resourcesVfs["korge.png"].readBitmap()
        val images = (0 until 128).map {
            val x = it % 16
            val y = it / 16
            image(bitmap) {
                anchor(.5, .5)
                scale(.05)
                position(64 + 24 * x, 64 + 24 * y)
            }
        }

        // Show FPS overlay
        views.debugViews = true

        // Main render loop
        launchImmediately {
            frameBlock(144.timesPerSecond) {
                while (true) {
                    frame()
                }
            }
        }

        // Update filters every so often
        launch {
            while (true) {
                delay(0.1.seconds)
                for (image in images) {
                    image.filter = if (Random.nextBoolean()) {
                        ColorMatrixFilter(ColorMatrixFilter.SEPIA_MATRIX)
                    } else {
                        ColorMatrixFilter(ColorMatrixFilter.GRAYSCALE_MATRIX)
                    }
                }
                println(views.ag.getStats())
            }
        }
    }
}
