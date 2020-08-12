package com.soywiz.kds.atomic

actual fun <T> kdsFreeze(value: T): T = value
//actual val <T> T.kdsIsFrozen: Boolean get() = false
//actual fun Any.kdsEnsureNeverFrozen() = Unit
