package samples

import com.soywiz.korge.input.mouse
import com.soywiz.korge.scene.Scene
import com.soywiz.korge.view.SContainer
import com.soywiz.korge.view.addUpdater
import com.soywiz.korge.view.image
import com.soywiz.korge.view.scale
import com.soywiz.korge.view.unsafeRenderToBitmapSync
import com.soywiz.korge.view.xy
import com.soywiz.korim.bitmap.Bitmaps
import com.soywiz.korim.bitmap.slice
import com.soywiz.korim.format.readBitmap
import com.soywiz.korio.file.std.resourcesVfs
import com.soywiz.korma.geom.Rectangle

class MainColorPicker : Scene() {
    override suspend fun SContainer.sceneMain() {
        image(resourcesVfs["korge.png"].readBitmap())

        val magnifier = image(Bitmaps.white).xy(720, 200).scale(10, 10).apply { smoothing = false }

        mouse {
            move {
                val bmp = stage!!.unsafeRenderToBitmapSync(views!!.renderContext, Rectangle(views.stage.mouseX - 5.0, views.stage.mouseY - 5.0, 10.0, 10.0), views!!.globalToWindowScaleAvg)
                magnifier.bitmap = bmp.slice()
            }
        }
        addUpdater {
        }
    }
}
