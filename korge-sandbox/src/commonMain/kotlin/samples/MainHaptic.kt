package samples

import com.soywiz.korge.scene.Scene
import com.soywiz.korge.service.haptic.HapticFeedback
import com.soywiz.korge.service.haptic.hapticFeedback
import com.soywiz.korge.ui.clicked
import com.soywiz.korge.ui.uiButton
import com.soywiz.korge.view.SContainer

class MainHaptic : Scene() {
    override suspend fun SContainer.sceneMain() {
        views.hapticFeedback
        uiButton("hello") {
            clicked {
                views.hapticFeedback.emit(HapticFeedback.Kind.ALIGNMENT)
            }
        }
    }
}
