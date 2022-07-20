package com.soywiz.korio.async

import android.content.Context
import com.soywiz.korio.android.withAndroidContext
import com.soywiz.korio.dynamic.Dyn
import kotlinx.coroutines.*

actual fun asyncEntryPoint(callback: suspend () -> Unit) {
	CoroutineScope(Dispatchers.Main).launch {
		callback()
	}
}

actual fun asyncTestEntryPoint(callback: suspend () -> Unit) {
    runBlocking {
        val getContextMethod = Class.forName("androidx.test.core.app.ApplicationProvider").getMethod("getApplicationContext")
        withAndroidContext(getContextMethod.invoke(null) as Context) {
            callback()
        }
    }
}

