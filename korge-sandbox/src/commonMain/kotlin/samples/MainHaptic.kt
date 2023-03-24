package samples

import korlibs.korge.scene.Scene
import korlibs.korge.service.haptic.*
import korlibs.korge.ui.clicked
import korlibs.korge.ui.uiButton
import korlibs.korge.view.SContainer

class MainHaptic : Scene() {
    override suspend fun SContainer.sceneMain() {
        views.hapticFeedback
        uiButton("hello") {
            clicked {
                views.hapticFeedback.emit(HapticFeedbackKind.ALIGNMENT)
            }
        }
    }
}