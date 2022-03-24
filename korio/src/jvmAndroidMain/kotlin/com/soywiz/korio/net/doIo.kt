package com.soywiz.korio.net

import kotlinx.coroutines.*

internal suspend fun <T> doIo(block: CoroutineScope.() -> T): T = withContext(Dispatchers.IO, block)
