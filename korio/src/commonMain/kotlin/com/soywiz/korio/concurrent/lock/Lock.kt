package com.soywiz.korio.concurrent.lock

expect class Lock() {
	inline operator fun <T> invoke(callback: () -> T): T
}
