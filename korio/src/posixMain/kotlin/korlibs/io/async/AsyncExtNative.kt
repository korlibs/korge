package korlibs.io.async

import kotlin.coroutines.*
import kotlinx.coroutines.*

actual fun asyncEntryPoint(callback: suspend () -> Unit) = runBlocking { callback() }
actual fun asyncTestEntryPoint(callback: suspend () -> Unit) = asyncEntryPoint(callback)