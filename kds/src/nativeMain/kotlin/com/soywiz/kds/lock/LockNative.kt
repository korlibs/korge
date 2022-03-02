package com.soywiz.kds.lock

actual class Lock actual constructor() {
	actual inline operator fun <T> invoke(callback: () -> T): T = callback()
}
