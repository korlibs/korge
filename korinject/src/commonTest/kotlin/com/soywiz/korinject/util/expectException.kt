package com.soywiz.korinject.util

inline fun <reified T : Throwable> expectException(callback: () -> Unit) {
	try {
		callback()
		throw ExpectedException("Expected")
	} catch (e: Throwable) {
		if (e !is T) throw e
	}
}

class ExpectedException(msg: String) : Exception(msg)
