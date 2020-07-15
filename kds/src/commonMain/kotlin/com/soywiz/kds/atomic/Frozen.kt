package com.soywiz.kds.atomic

expect fun <T> T.kdsFreeze(): T
expect val <T> T.kdsIsFrozen: Boolean
expect fun Any.kdsEnsureNeverFrozen()
