package com.soywiz.korio.net

import com.soywiz.korio.async.*
import kotlinx.coroutines.*

internal suspend fun <T> doIo(block: suspend CoroutineScope.() -> T): T = withContext(Dispatchers.CIO, block)
