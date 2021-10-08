package com.soywiz.kds.atomic

expect fun <T> kdsFreeze(value: T): T
expect fun <T> kdsIsFrozen(value: T): Boolean
//expect fun Any.kdsEnsureNeverFrozen()
