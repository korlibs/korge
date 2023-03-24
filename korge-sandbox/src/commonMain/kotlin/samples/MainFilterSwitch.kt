package samples

import korlibs.time.seconds
import korlibs.time.timesPerSecond
import korlibs.korge.scene.Scene
import korlibs.korge.time.delay
import korlibs.korge.view.SContainer
import korlibs.korge.view.anchor
import korlibs.korge.view.filter.filter
import korlibs.korge.view.filter.ColorMatrixFilter
import korlibs.korge.view.image
import korlibs.korge.view.position
import korlibs.korge.view.scale
import korlibs.image.format.readBitmap
import korlibs.io.async.launch
import korlibs.io.async.launchImmediately
import korlibs.io.file.std.resourcesVfs
import util.*
import kotlin.random.Random

class MainFilterSwitch : Scene() {
    override suspend fun SContainer.sceneMain() {
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
