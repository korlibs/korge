package com.soywiz.korge.intellij.util

import com.soywiz.korio.async.Signal
import com.soywiz.korio.lang.Closeable

fun <T> Signal<T>.addCallInit(initial: T, handler: (T) -> Unit): Closeable {
	handler(initial)
	return add(handler)
}

fun Signal<Unit>.addCallInit(handler: (Unit) -> Unit): Closeable = addCallInit(Unit, handler)
