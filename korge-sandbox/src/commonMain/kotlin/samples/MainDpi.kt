package samples

import korlibs.korge.scene.*
import korlibs.korge.time.*
import korlibs.korge.view.*
import korlibs.math.geom.*
import korlibs.time.*

class MainDpi : Scene() {
    override suspend fun SContainer.sceneMain() {
        val text = text("-").scale(2.0)

        val rect = solidRect(views.virtualPixelsPerCm * 2, views.virtualPixelsPerCm * 2).xy(0, 400)

        intervalAndNow(0.5.seconds) {
            rect.unscaledSize = Size(views.virtualPixelsPerCm * 2, views.virtualPixelsPerCm * 2)
            text.text = """
            nativeWidth: ${views.nativeWidth}, ${views.nativeHeight}
            devicePixelRatio: ${views.devicePixelRatio}
            virtualWidth: ${views.virtualWidth}, ${views.virtualHeight}
            pixelsPerInch: ${views.pixelsPerInch}
            pixelsPerCm: ${views.pixelsPerCm}
            virtualPixelsPerInch: ${views.virtualPixelsPerInch}
            virtualPixelsPerCm: ${views.virtualPixelsPerCm}
            virtualWidth/virtualPixelsPerCm: ${views.virtualWidth / views.virtualPixelsPerCm}
            nativeWidth/pixelsPerCm: ${views.nativeWidth / views.pixelsPerCm}
        """.trimIndent()
        }
    }
}
