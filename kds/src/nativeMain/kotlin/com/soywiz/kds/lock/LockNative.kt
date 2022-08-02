package com.soywiz.kds.lock

import kotlinx.atomicfu.locks.SynchronizedObject
import kotlinx.atomicfu.locks.withLock

actual class Lock actual constructor() : SynchronizedObject() {
	actual inline operator fun <T> invoke(callback: () -> T): T = withLock {
        callback()
    }
}
