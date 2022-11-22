package com.soywiz.korge.util

import com.soywiz.korio.lang.Cancellable
import com.soywiz.korio.lang.Closeable
import com.soywiz.korio.lang.CloseableCancellable
import com.soywiz.korio.lang.cancel
import com.soywiz.korio.lang.cancellable
import kotlin.coroutines.cancellation.CancellationException

class CancellableGroup : CloseableCancellable {
	private val cancellables = arrayListOf<Cancellable>()

    operator fun plusAssign(c: CloseableCancellable) {
        cancellables += c
    }

	operator fun plusAssign(c: Cancellable) {
		cancellables += c
	}

	operator fun plusAssign(c: Closeable) {
		cancellables += c.cancellable()
	}

	fun addCancellable(c: Cancellable) {
		cancellables += c
	}

	fun addCloseable(c: Closeable) {
		cancellables += c.cancellable()
	}

    override fun close() {
        cancel(CancellationException())
    }

    override fun cancel(e: Throwable) {
		cancellables.cancel(e)
	}
}

suspend fun <T> AutoClose(callback: suspend (CancellableGroup) -> T): T {
	val group = CancellableGroup()
	try {
		return callback(group)
	} finally {
		group.cancel()
	}
}
