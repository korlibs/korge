package samples

import com.soywiz.klock.seconds
import com.soywiz.korge.scene.Scene
import com.soywiz.korge.time.interval
import com.soywiz.korge.time.intervalAndNow
import com.soywiz.korge.view.Container
import com.soywiz.korge.view.Stage
import com.soywiz.korge.view.scale
import com.soywiz.korge.view.solidRect
import com.soywiz.korge.view.text
import com.soywiz.korge.view.xy

class MainDpi : Scene() {
    override suspend fun Container.sceneMain() {
        val text = text("-").scale(2.0)

        val rect = solidRect(views.virtualPixelsPerCm * 2, views.virtualPixelsPerCm * 2).xy(0, 400)

        intervalAndNow(0.5.seconds) {
            rect.width = views.virtualPixelsPerCm * 2
            rect.height = views.virtualPixelsPerCm * 2
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
