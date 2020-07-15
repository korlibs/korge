package com.soywiz.korio.async

import kotlin.coroutines.*

actual fun <T> Promise(coroutineContext: CoroutineContext, executor: (resolve: (T) -> Unit, reject: (Throwable) -> Unit) -> Unit): Promise<T>
    = kotlin.js.Promise(executor).asDynamic()

