package com.soywiz.korio.async

interface AsyncCloseable {
	suspend fun close()

	companion object {
		val DUMMY = object : AsyncCloseable {
			override suspend fun close() = Unit
		}
	}
}

// @TODO: Bug in Kotlin.JS related to inline
// https://youtrack.jetbrains.com/issue/KT-29120
//inline suspend fun <T : AsyncCloseable, R> T.use(callback: T.() -> R): R { // FAILS
//	try {
//		return callback()
//	} finally {
//		close()
//	}
//}

suspend inline fun <T : AsyncCloseable, TR> T.use(callback: T.() -> TR): TR {
	var error: Throwable? = null
	val result = try {
		callback(this)
	} catch (e: Throwable) {
		error = e
		null
	}
	close()
	if (error != null) throw error
	return result as TR
}

suspend inline fun <T : AsyncCloseable, TR> T.useIt(callback: (T) -> TR): TR {
    var error: Throwable? = null
    val result = try {
        callback(this)
    } catch (e: Throwable) {
        error = e
        null
    }
    close()
    if (error != null) throw error
    return result as TR
}
