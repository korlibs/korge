package com.soywiz.kds.lock

actual class Lock actual constructor() {
	actual inline operator fun <T> invoke(callback: () -> T): T = callback()
}

actual class NonRecursiveLock actual constructor() {
    actual inline operator fun <T> invoke(callback: () -> T): T = callback()
}
