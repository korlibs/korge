package samples

import com.soywiz.korge.scene.Scene
import com.soywiz.korge.service.haptic.HapticFeedback
import com.soywiz.korge.service.haptic.hapticFeedback
import com.soywiz.korge.ui.UIButton
import com.soywiz.korge.ui.clicked
import com.soywiz.korge.ui.uiButton
import com.soywiz.korge.view.Container
import com.soywiz.korge.view.Stage

class MainHaptic : Scene() {
    override suspend fun Container.sceneMain() {
        views.hapticFeedback
        uiButton("hello") {
            clicked {
                views.hapticFeedback.emit(HapticFeedback.Kind.ALIGNMENT)
            }
        }
    }
}
