package com.soywiz.korio.async

import com.soywiz.korio.lang.*
import kotlin.coroutines.*

open class EmptyContinuation(override val context: CoroutineContext) : Continuation<Unit> {
	override fun resumeWith(result: Result<Unit>) {
		val exception = result.exceptionOrNull()
		exception?.printStackTrace()
	}

	companion object : EmptyContinuation(EmptyCoroutineContext)
}
