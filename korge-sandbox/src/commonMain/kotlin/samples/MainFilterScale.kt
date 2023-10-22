package samples

import korlibs.image.format.*
import korlibs.io.file.std.*
import korlibs.korge.scene.*
import korlibs.korge.time.*
import korlibs.korge.ui.*
import korlibs.korge.view.*
import korlibs.korge.view.filter.*

class MainFilterScale : Scene() {
    override suspend fun SContainer.sceneMain() {
        val bmp = resourcesVfs["korge.png"].readBitmap()
        val image = image(bmp).xy(100, 100)
            .filterScale(1.0)
            .filters(Convolute3Filter(Convolute3Filter.KERNEL_EDGE_DETECTION))
        val dithering = DitheringFilter(4.0)
        val image2 = image(bmp).xy(600, 100)
            .filterScale(1.0)
            .filters(dithering)
        //.filters(BlurFilter())
        //.filters(WaveFilter())

        val combo = uiSlider(value = 1.0, max = 1.0, step = 0.01).xy(400, 100).changed {
            image.filterScale = it
            image2.filterScale = it
        }
        val combo2 = uiSlider(value = 4.0, min = 1.0, max = 8.0, step = 0.1).xy(400, 50).changed {
            dithering.levels = it
        }
        //val combo = uiComboBox(items = listOf(0.0, 0.01, 0.05, 0.075, 0.125, 0.25, 0.44, 0.5, 0.75, 0.95, 0.99, 1.0)).xy(400, 100).onSelectionUpdate { image.filterScale = it.selectedItem ?: 1.0 }

        // This reproduces a bug (black right and bottom border) at least on macOS with M1
        image.filterScale = 0.99
        delayFrame()
        image.filterScale = 0.95

        /*
        while (true) {
            tween(image::filterScale[0.1], time = 1.seconds)
            tween(image::filterScale[1.0], time = 1.seconds)
        }

         */
    }
}
