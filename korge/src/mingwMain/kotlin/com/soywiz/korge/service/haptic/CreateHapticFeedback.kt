package com.soywiz.korge.service.haptic

import com.soywiz.korge.view.Views

actual fun CreateHapticFeedback(views: Views): HapticFeedback = HapticFeedback(views)
