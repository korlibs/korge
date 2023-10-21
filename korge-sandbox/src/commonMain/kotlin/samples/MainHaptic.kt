package samples

import korlibs.korge.scene.*
import korlibs.korge.service.haptic.*
import korlibs.korge.ui.*
import korlibs.korge.view.*
import korlibs.render.*

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
