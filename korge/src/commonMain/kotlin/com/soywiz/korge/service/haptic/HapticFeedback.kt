package com.soywiz.korge.service.haptic

import com.soywiz.kds.extraPropertyThis
import com.soywiz.klock.milliseconds
import com.soywiz.korge.service.vibration.vibration
import com.soywiz.korge.view.Views
import kotlin.native.concurrent.ThreadLocal

@ThreadLocal
val Views.hapticFeedback by extraPropertyThis { CreateHapticFeedback(this) }

expect fun CreateHapticFeedback(views: Views): HapticFeedback

// https://developer.apple.com/design/human-interface-guidelines/ios/user-interaction/haptics/
open class HapticFeedback(val views: Views) {
    enum class Kind { GENERIC, ALIGNMENT, LEVEL_CHANGE }

    // @TODO: Kind patterns are temporal. We have to figure out reasonable times
    open fun emit(kind: Kind) {
        val amplitude = 0.2
        val pattern = when (kind) {
            Kind.GENERIC -> arrayOf(200.milliseconds)
            Kind.ALIGNMENT -> arrayOf(300.milliseconds)
            Kind.LEVEL_CHANGE -> arrayOf(50.milliseconds, 50.milliseconds, 100.milliseconds)
        }
        views.vibration.vibratePattern(pattern, pattern.map { amplitude }.toTypedArray())
    }
}
