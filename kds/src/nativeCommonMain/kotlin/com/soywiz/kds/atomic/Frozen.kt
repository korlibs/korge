package com.soywiz.kds.atomic

import kotlin.native.concurrent.ensureNeverFrozen
import kotlin.native.concurrent.freeze
import kotlin.native.concurrent.isFrozen

actual fun <T> T.kdsFreeze(): T = this.freeze()
actual val <T> T.kdsIsFrozen: Boolean get() = this.isFrozen
actual fun Any.kdsEnsureNeverFrozen() = this.ensureNeverFrozen()
