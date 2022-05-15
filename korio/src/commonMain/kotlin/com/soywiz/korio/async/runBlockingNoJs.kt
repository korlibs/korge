package com.soywiz.korio.async

import kotlinx.coroutines.CoroutineScope
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

expect fun <T> runBlockingNoJs(context: CoroutineContext = EmptyCoroutineContext, block: suspend CoroutineScope.() -> T): T
