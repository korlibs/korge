package com.soywiz.kds.atomic

import kotlin.native.concurrent.ensureNeverFrozen
import kotlin.native.concurrent.freeze
import kotlin.native.concurrent.isFrozen

actual fun <T> kdsFreeze(value: T): T = value.freeze()
//actual val <T> T.kdsIsFrozen: Boolean get() = this.isFrozen
//actual fun Any.kdsEnsureNeverFrozen() = this.ensureNeverFrozen()
