package com.soywiz.korio.net

import kotlinx.coroutines.*

internal suspend fun <T> doIo(block: suspend CoroutineScope.() -> T): T = withContext(Dispatchers.IO, block)
