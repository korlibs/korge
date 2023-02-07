package com.soywiz.korge.service.haptic

import com.soywiz.kds.extraPropertyThis
import com.soywiz.klock.milliseconds
import com.soywiz.korge.service.vibration.vibration
import com.soywiz.korge.view.Views
import com.soywiz.korgw.*
import kotlin.native.concurrent.ThreadLocal

@ThreadLocal
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
