package com.soywiz.korge.service.haptic

import com.soywiz.korge.view.Views
import platform.UIKit.UIImpactFeedbackGenerator
import platform.UIKit.UISelectionFeedbackGenerator

actual fun CreateHapticFeedback(views: Views): HapticFeedback = object : HapticFeedback(views) {
    val uiSelectionFeedbackGenerator = UISelectionFeedbackGenerator()
    val uiImpactFeedbackGenerator = UIImpactFeedbackGenerator()

    override fun emit(kind: Kind) {
        when (kind) {
            Kind.GENERIC -> uiSelectionFeedbackGenerator.selectionChanged()
            Kind.ALIGNMENT -> uiSelectionFeedbackGenerator.selectionChanged()
            Kind.LEVEL_CHANGE -> uiImpactFeedbackGenerator.impactOccurred()
        }
    }
}
