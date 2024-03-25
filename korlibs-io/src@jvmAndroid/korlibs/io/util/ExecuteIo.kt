package korlibs.io.util

import korlibs.io.async.*
import kotlinx.coroutines.*
import kotlin.coroutines.*

suspend fun <T> jvmExecuteIo(callback: suspend () -> T): T = when {
    coroutineContext.preferSyncIo -> callback()
    else -> withContext(Dispatchers.CIO) { callback() }
}
