package korlibs.io

import android.content.*
import korlibs.io.async.*
import kotlinx.coroutines.*
import korlibs.io.android.withAndroidContext

fun Korio(context: Context, entry: suspend CoroutineScope.() -> Unit) = asyncEntryPoint { withAndroidContext(context) { entry(CoroutineScope(coroutineContext)) } }
