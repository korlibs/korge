package korlibs.io

import korlibs.io.async.asyncEntryPoint
import korlibs.io.internal.KORIO_VERSION
import kotlinx.coroutines.CoroutineScope
import kotlin.coroutines.coroutineContext

fun Korio(entry: suspend CoroutineScope.() -> Unit) = asyncEntryPoint { entry(CoroutineScope(coroutineContext)) }

object Korio {
	val VERSION = KORIO_VERSION
}
