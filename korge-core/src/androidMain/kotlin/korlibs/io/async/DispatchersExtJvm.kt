package korlibs.io.async

import kotlinx.coroutines.*

actual val Dispatchers.CIO: CoroutineDispatcher get() = Dispatchers.IO
actual val Dispatchers.ResourceDecoder: CoroutineDispatcher get() = CIO
