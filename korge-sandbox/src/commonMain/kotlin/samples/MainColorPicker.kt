package samples

import korlibs.korge.input.mouse
import korlibs.korge.scene.Scene
import korlibs.korge.view.SContainer
import korlibs.korge.view.addUpdater
import korlibs.korge.view.image
import korlibs.korge.view.scale
import korlibs.korge.view.unsafeRenderToBitmapSync
import korlibs.korge.view.xy
import korlibs.image.bitmap.Bitmaps
import korlibs.image.bitmap.slice
import korlibs.image.format.readBitmap
import korlibs.io.file.std.resourcesVfs
import korlibs.math.geom.MRectangle

class MainColorPicker : Scene() {
    override suspend fun SContainer.sceneMain() {
        image(resourcesVfs["korge.png"].readBitmap())

        val magnifier = image(Bitmaps.white).xy(720, 200).scale(10, 10).apply { smoothing = false }

        mouse {
            move {
                val bmp = stage!!.unsafeRenderToBitmapSync(views!!.renderContext, MRectangle(views.stage.mousePos.xD - 5.0, views.stage.mousePos.yD - 5.0, 10.0, 10.0), views!!.globalToWindowScaleAvg)
                magnifier.bitmap = bmp.slice()
                invalidateRender()
            }
        }
        addUpdater {
        }
    }
}
