import com.soywiz.korge.service.haptic.HapticFeedback
import com.soywiz.korge.service.haptic.hapticFeedback
import com.soywiz.korge.ui.UIButton
import com.soywiz.korge.ui.clicked
import com.soywiz.korge.ui.uiButton
import com.soywiz.korge.view.Stage

suspend fun Stage.mainHaptic() {
    views.hapticFeedback
    uiButton("hello") {
        clicked {
            views.hapticFeedback.emit(HapticFeedback.Kind.ALIGNMENT)
        }
    }
}
