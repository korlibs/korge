package korlibs.korge.service.haptic

import korlibs.datastructure.extraPropertyThis
import korlibs.time.milliseconds
import korlibs.korge.service.vibration.vibration
import korlibs.korge.view.Views
import korlibs.render.*
import kotlin.native.concurrent.ThreadLocal

val Views.hapticFeedback by extraPropertyThis { HapticFeedback(this) }

typealias HapticFeedbackKind = GameWindow.HapticFeedbackKind

// https://developer.apple.com/design/human-interface-guidelines/ios/user-interaction/haptics/
open class HapticFeedback(val views: Views) {
    // @TODO: Kind patterns are temporal. We have to figure out reasonable times
    open fun emit(kind: HapticFeedbackKind) {
        if (views.gameWindow.hapticFeedbackGenerateSupport) {
            views.gameWindow.hapticFeedbackGenerate(kind)
        } else {
            val amplitude = 0.2
            val pattern = when (kind) {
                HapticFeedbackKind.GENERIC -> arrayOf(200.milliseconds)
                HapticFeedbackKind.ALIGNMENT -> arrayOf(300.milliseconds)
                HapticFeedbackKind.LEVEL_CHANGE -> arrayOf(50.milliseconds, 50.milliseconds, 100.milliseconds)
            }
            views.vibration.vibratePattern(pattern, pattern.map { amplitude }.toTypedArray())
        }
    }
}
