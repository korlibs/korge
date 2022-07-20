package samples

import com.soywiz.korge.scene.Scene
import com.soywiz.korge.time.*
import com.soywiz.korge.ui.*
import com.soywiz.korge.view.*
import com.soywiz.korge.view.filter.*
import com.soywiz.korim.format.*
import com.soywiz.korio.file.std.*

class MainFilterScale : Scene() {
    override suspend fun SContainer.sceneMain() {
        val image = image(resourcesVfs["korge.png"].readBitmap()).xy(100, 100)
            .filterScale(1.0)
            .filters(Convolute3Filter(Convolute3Filter.KERNEL_EDGE_DETECTION))
        //.filters(BlurFilter())
        //.filters(WaveFilter())

        val combo = uiSlider(value = 1.0, max = 1.0, step = 0.01).xy(400, 100).changed { image.filterScale = it  }
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
