package com.soywiz.kds.atomic

actual fun <T> kdsFreeze(value: T): T = value
actual fun <T> kdsIsFrozen(value: T): Boolean = false
