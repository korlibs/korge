package com.soywiz.korio.android

import android.content.*
import kotlinx.coroutines.*
import kotlin.coroutines.*

//suspend fun withContext(context: Context, suspend callback: () -> Unit) {
//	coroutineContext.
//}

class AndroidCoroutineContext(val context: Context) : CoroutineContext.Element {
	object Key : CoroutineContext.Key<AndroidCoroutineContext>

	override val key: CoroutineContext.Key<*> = Key

}

suspend fun <T> withAndroidContext(context: Context, callback: suspend CoroutineScope.() -> T): T {
	return withContext(coroutineContext + AndroidCoroutineContext(context), callback)
}

fun CoroutineContext.androidContext(): Context = this[AndroidCoroutineContext.Key]?.context
    ?: error("Android context not set! Please call `withAndroidContext()` method in your coroutine body")

suspend fun androidContext(): Context = coroutineContext.androidContext()
