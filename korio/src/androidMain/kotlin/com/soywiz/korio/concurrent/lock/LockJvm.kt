package com.soywiz.korio.concurrent.lock

internal actual class Lock actual constructor() {
	actual inline operator fun <T> invoke(callback: () -> T): T = synchronized(this) { callback() }
}
