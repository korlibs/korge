package com.soywiz.kds.atomic

import kotlin.native.concurrent.freeze
import kotlin.native.concurrent.isFrozen

actual fun <T> kdsFreeze(value: T): T = value.freeze()
actual fun <T> kdsIsFrozen(value: T): Boolean = value.isFrozen
//actual val <T> T.kdsIsFrozen: Boolean get() = this.isFrozen
//actual fun Any.kdsEnsureNeverFrozen() = this.ensureNeverFrozen()
