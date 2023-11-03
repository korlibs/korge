package samples

import korlibs.image.bitmap.*
import korlibs.image.format.*
import korlibs.io.async.*
import korlibs.io.file.std.*
import korlibs.korge.input.*
import korlibs.korge.scene.*
import korlibs.korge.view.*
import korlibs.math.geom.*

class MainColorPicker : Scene() {
    override suspend fun SContainer.sceneMain() {
        image(resourcesVfs["korge.png"].readBitmap())

        val magnifier = image(Bitmaps.white).xy(720, 200).scale(10, 10).apply { smoothing = false }

        mouse {
            move {

                //val bmp = stage!!.unsafeRenderToBitmapSync(views!!.renderContext, Rectangle(views.stage.mousePos.x - 5.0, views.stage.mousePos.y - 5.0, 10.0, 10.0), views!!.globalToWindowScaleAvg)
                launch {
                    val bmp = stage!!.renderToBitmap(views!!, Rectangle(views.stage.mousePos.x - 5.0, views.stage.mousePos.y - 5.0, 10.0, 10.0), views!!.globalToWindowScaleAvg)
                    magnifier.bitmap = bmp.slice()
                    invalidateRender()
                }
            }
        }
        addUpdater {
        }
    }
}
