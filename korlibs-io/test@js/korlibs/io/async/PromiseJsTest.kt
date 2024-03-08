package korlibs.io.async

import korlibs.time.*
import korlibs.memory.*
import korlibs.io.*
import korlibs.io.util.*
import korlibs.platform.*
import kotlin.test.*

class PromiseJsTest {
    @Test
    fun test() = suspendTest({ !Platform.isJsNodeJs }) {
        val startTime = DateTime.now()
        val value = delay(100)
        assertTrue(value is JsPromise<*>)
        assertTrue(value.asDynamic().then != null)
        value.await()
        val endTime = DateTime.now()
        assertEquals(true, endTime - startTime >= 100.milliseconds)
    }

    fun delay(ms: Int): Promise<Unit> = Promise { resolve, reject -> jsGlobal.setTimeout({ resolve(Unit) }, ms) }
}

@JsName("Promise")
private external class JsPromise<T>
