package com.soywiz.korio

import com.soywiz.korio.async.*
import com.soywiz.korio.internal.*
import kotlinx.coroutines.*
import kotlin.coroutines.*

fun Korio(entry: suspend CoroutineScope.() -> Unit) = asyncEntryPoint { entry(CoroutineScope(coroutineContext)) }

object Korio {
	val VERSION = KORIO_VERSION
}
