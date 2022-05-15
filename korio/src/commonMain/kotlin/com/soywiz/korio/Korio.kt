package com.soywiz.korio

import com.soywiz.korio.async.asyncEntryPoint
import com.soywiz.korio.internal.KORIO_VERSION
import kotlinx.coroutines.CoroutineScope
import kotlin.coroutines.coroutineContext

fun Korio(entry: suspend CoroutineScope.() -> Unit) = asyncEntryPoint { entry(CoroutineScope(coroutineContext)) }

object Korio {
	val VERSION = KORIO_VERSION
}
