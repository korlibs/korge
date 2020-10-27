package com.soywiz.korio.async

import com.soywiz.korio.lang.*
import kotlinx.coroutines.*
import kotlin.coroutines.*

actual fun <T> runBlockingNoJs(context: CoroutineContext, block: suspend CoroutineScope.() -> T): T {
    unexpected("Calling runBlockingNoJs on JavaScript")
}
