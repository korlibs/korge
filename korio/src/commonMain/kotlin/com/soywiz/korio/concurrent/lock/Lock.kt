package com.soywiz.korio.concurrent.lock

internal expect class Lock() {
	inline operator fun <T> invoke(callback: () -> T): T
}
