package com.soywiz.korio.async

import kotlinx.coroutines.*

private val IO = newFixedThreadPoolContext(16, "IO")

actual val Dispatchers.CIO: CoroutineDispatcher get() = com.soywiz.korio.async.IO
//actual val Dispatchers.CIO: CoroutineDispatcher get() = Dispatchers.IO // @TODO: Enable this once bumped to kotlinx.coroutines 1.7.x
actual val Dispatchers.ResourceDecoder: CoroutineDispatcher get() = CIO
