package com.soywiz.korge

actual suspend fun <T> withKorgeContext(context: Any?, callback: suspend () -> T): T {
	return callback()
}
