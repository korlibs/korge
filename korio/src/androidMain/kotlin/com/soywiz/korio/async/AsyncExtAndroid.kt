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
        val contextResult = runCatching {
            val getContextMethod = Class.forName("androidx.test.core.app.ApplicationProvider").getMethod("getApplicationContext")
            getContextMethod.invoke(null) as Context
        }
        if (contextResult.isSuccess) {
            withAndroidContext(contextResult.getOrThrow()) {
                callback()
            }
        } else {
            // Do nothing
            println("Test disabled due to ${contextResult.exceptionOrNull()!!.message}")
        }
    }
}

