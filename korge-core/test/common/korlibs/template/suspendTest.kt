package korlibs.template

import kotlinx.coroutines.test.*
import kotlin.coroutines.AbstractCoroutineContextElement
import kotlin.coroutines.Continuation
import kotlin.coroutines.ContinuationInterceptor
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.intrinsics.COROUTINE_SUSPENDED
import kotlin.coroutines.intrinsics.startCoroutineUninterceptedOrReturn
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

fun suspendTest(callback: suspend TestScope.() -> Unit): TestResult = runTest { callback() }

fun <T : Any> runBlockingNoSuspensions(callback: suspend () -> T): T {
    var completed = false
    lateinit var rresult: T
    var resultEx: Throwable? = null
    var suspendCount = 0

    callback.startCoroutineUndispatched(object : Continuation<T?> {
        override val context: CoroutineContext = object :
            AbstractCoroutineContextElement(ContinuationInterceptor), ContinuationInterceptor {
            override val key: CoroutineContext.Key<*> = ContinuationInterceptor.Key
            override fun <T> interceptContinuation(continuation: Continuation<T>): Continuation<T> =
                continuation.also { suspendCount++ }
        }

        private val unitInstance get() = Unit

        override fun resumeWith(result: Result<T?>) {
            val exception = result.exceptionOrNull()
            if (exception != null) {
                resultEx = exception
                completed = true
                println(exception)
            } else {
                //val value =
                val rvalue = result.getOrThrow() ?: (unitInstance as T)
                rresult = rvalue
                completed = true
            }
        }
    })
    if (!completed) throw RuntimeException("runBlockingNoSuspensions was not completed synchronously! suspendCount=$suspendCount")
    if (resultEx != null) throw resultEx!!
    return rresult
}

private fun <T> (suspend () -> T).startCoroutineUndispatched(completion: Continuation<T>) {
    startDirect(completion) {
        withCoroutineContext(completion.context, null) {
            startCoroutineUninterceptedOrReturn(completion)
        }
    }
}

private inline fun <T> startDirect(completion: Continuation<T>, block: () -> Any?) {
    val value = try {
        block()
    } catch (e: Throwable) {
        completion.resumeWithException(e)
        return
    }
    if (value !== COROUTINE_SUSPENDED) {
        @Suppress("UNCHECKED_CAST")
        completion.resume(value as T)
    }
}

private inline fun <T> withCoroutineContext(context: CoroutineContext, countOrElement: Any?, block: () -> T): T =
    block()
