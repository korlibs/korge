package com.soywiz.korio.async

import kotlin.coroutines.*
import kotlinx.coroutines.*

expect fun <T> runBlockingNoJs(context: CoroutineContext = EmptyCoroutineContext, block: suspend CoroutineScope.() -> T): T
