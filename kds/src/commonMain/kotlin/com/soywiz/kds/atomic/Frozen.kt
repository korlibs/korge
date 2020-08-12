package com.soywiz.kds.atomic

expect fun <T> kdsFreeze(value: T): T
//expect val <T> T.kdsIsFrozen: Boolean
//expect fun Any.kdsEnsureNeverFrozen()
