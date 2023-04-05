package samples

import korlibs.time.seconds
import korlibs.korge.scene.Scene
import korlibs.korge.time.intervalAndNow
import korlibs.korge.view.SContainer
import korlibs.korge.view.scale
import korlibs.korge.view.solidRect
import korlibs.korge.view.text
import korlibs.korge.view.xy

class MainDpi : Scene() {
    override suspend fun SContainer.sceneMain() {
        val text = text("-").scale(2.0)

        val rect = solidRect(views.virtualPixelsPerCm * 2, views.virtualPixelsPerCm * 2).xy(0, 400)

        intervalAndNow(0.5.seconds) {
            rect.widthD = views.virtualPixelsPerCm * 2
            rect.heightD = views.virtualPixelsPerCm * 2
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
