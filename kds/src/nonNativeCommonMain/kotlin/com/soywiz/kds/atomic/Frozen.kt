package com.soywiz.kds.atomic

actual fun <T> T.kdsFreeze(): T = this
actual val <T> T.kdsIsFrozen: Boolean get() = false
actual fun Any.kdsEnsureNeverFrozen() = Unit
