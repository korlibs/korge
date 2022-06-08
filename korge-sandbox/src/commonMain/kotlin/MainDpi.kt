import com.soywiz.klock.seconds
import com.soywiz.korge.time.interval
import com.soywiz.korge.time.intervalAndNow
import com.soywiz.korge.view.Stage
import com.soywiz.korge.view.scale
import com.soywiz.korge.view.solidRect
import com.soywiz.korge.view.text
import com.soywiz.korge.view.xy

suspend fun Stage.mainDpi() {
    val text = text("-").scale(2.0)

    val rect = solidRect(views.virtualPixelsPerCm * 2, views.virtualPixelsPerCm * 2).xy(0, 400)

    intervalAndNow(0.5.seconds) {
        rect.width = views.virtualPixelsPerCm * 2
        rect.height = views.virtualPixelsPerCm * 2
        text.text = """
            nativeWidth: ${stage.views.nativeWidth}, ${stage.views.nativeHeight}
            devicePixelRatio: ${stage.views.devicePixelRatio}
            virtualWidth: ${stage.views.virtualWidth}, ${stage.views.virtualHeight}
            pixelsPerInch: ${stage.views.pixelsPerInch}
            pixelsPerCm: ${stage.views.pixelsPerCm}
            virtualPixelsPerInch: ${stage.views.virtualPixelsPerInch}
            virtualPixelsPerCm: ${stage.views.virtualPixelsPerCm}
            virtualWidth/virtualPixelsPerCm: ${stage.views.virtualWidth / stage.views.virtualPixelsPerCm}
            nativeWidth/pixelsPerCm: ${stage.views.nativeWidth / stage.views.pixelsPerCm}
        """.trimIndent()
    }
}
