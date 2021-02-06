package com.soywiz.kds.lock

expect class Lock() {
	inline operator fun <T> invoke(callback: () -> T): T
}
