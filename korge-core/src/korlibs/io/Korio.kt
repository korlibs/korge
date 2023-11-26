package korlibs.io

import korlibs.io.async.asyncEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlin.coroutines.coroutineContext

fun Korio(entry: suspend CoroutineScope.() -> Unit) = asyncEntryPoint { entry(CoroutineScope(coroutineContext)) }
