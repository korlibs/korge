package korlibs.io.net

import korlibs.io.async.*
import kotlinx.coroutines.*

internal suspend fun <T> doIo(block: suspend CoroutineScope.() -> T): T = withContext(Dispatchers.CIO, block)
