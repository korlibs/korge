package com.soywiz.korge.service.haptic

import com.soywiz.korge.view.Views
import platform.AppKit.*

actual fun CreateHapticFeedback(views: Views): HapticFeedback = object : HapticFeedback(views) {
    override fun emit(kind: Kind) {
        NSHapticFeedbackManager.defaultPerformer.performFeedbackPattern(
            when (kind) {
                Kind.GENERIC -> NSHapticFeedbackPatternGeneric
                Kind.ALIGNMENT -> NSHapticFeedbackPatternAlignment
                Kind.LEVEL_CHANGE -> NSHapticFeedbackPatternLevelChange
            },
            NSHapticFeedbackPerformanceTimeNow
        )
    }
}
