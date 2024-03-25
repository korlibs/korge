package korlibs.io.async

import android.content.Context
import korlibs.io.android.withAndroidContext
import korlibs.io.file.std.*
import kotlinx.coroutines.*

@Suppress("ACTUAL_WITHOUT_EXPECT", "ACTUAL_TYPE_ALIAS_TO_CLASS_WITH_DECLARATION_SITE_VARIANCE")
actual typealias AsyncEntryPointResult = Unit

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
            val context = contextResult.getOrThrow()
            vfsInitWithAndroidContextOnce(context)
            withAndroidContext(context) {
                callback()
            }
        } else {
            // Do nothing
            println("Test disabled due to ${contextResult.exceptionOrNull()!!.message}")
        }
    }
}
