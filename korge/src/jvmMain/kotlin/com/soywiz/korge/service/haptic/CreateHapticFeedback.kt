package com.soywiz.korge.service.haptic

import com.soywiz.kmem.Platform
import com.soywiz.korge.view.Views
import com.soywiz.korgw.osx.*

actual fun CreateHapticFeedback(views: Views): HapticFeedback = object : HapticFeedback(views) {
    override fun emit(kind: Kind) {
        when {
            Platform.os.isMac -> {
                val kindInt = when (kind) {
                    Kind.GENERIC -> KIND_GENERIC
                    Kind.ALIGNMENT -> KIND_ALIGNMENT
                    Kind.LEVEL_CHANGE -> KIND_LEVEL_CHANGE
                }
                val performanceTime = PERFORMANCE_TIME_NOW

                NSClass("NSHapticFeedbackManager")
                    .msgSend("defaultPerformer")
                    .msgSend("performFeedbackPattern:performanceTime:", kindInt.toLong(), performanceTime.toLong())
            }
            else -> {
                super.emit(kind)
            }
        }
    }

    val KIND_GENERIC = 0
    val KIND_ALIGNMENT = 1
    val KIND_LEVEL_CHANGE = 2

    val PERFORMANCE_TIME_DEFAULT = 0
    val PERFORMANCE_TIME_NOW = 1
    val PERFORMANCE_TIME_DRAW_COMPLETED = 2
}
