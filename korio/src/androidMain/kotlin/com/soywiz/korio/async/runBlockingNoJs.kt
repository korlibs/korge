package com.soywiz.korio.async

import kotlinx.coroutines.*
import kotlin.coroutines.*

actual fun <T> runBlockingNoJs(context: CoroutineContext, block: suspend CoroutineScope.() -> T): T {
    return runBlocking { block() }
}
