import com.soywiz.korge.view.Stage
import com.soywiz.korge.view.clipContainer
import com.soywiz.korge.view.solidRect
import com.soywiz.korge.view.xy
import com.soywiz.korim.color.Colors

suspend fun Stage.mainClipping() {
    clipContainer(100, 100) {
        xy(50, 70)
        solidRect(20, 20, Colors.RED).xy(-10, -10)
    }
}
