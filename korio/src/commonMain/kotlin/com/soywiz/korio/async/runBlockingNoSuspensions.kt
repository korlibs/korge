package com.soywiz.korio.async

import com.soywiz.korio.lang.*
import kotlinx.coroutines.*
import kotlin.coroutines.*
import kotlin.coroutines.intrinsics.*

/**
 * Allows to execute a suspendable block as long as you can ensure no suspending will happen at all..
 */
@UseExperimental(InternalCoroutinesApi::class)
fun <T : Any> runBlockingNoSuspensions(callback: suspend () -> T): T {
	//TODO("runBlockingNoSuspensions not supported yet!")
	var completed = false
	lateinit var rresult: T
	var resultEx: Throwable? = null
	var suspendCount = 0

	callback.startCoroutineUndispatched(object : Continuation<T?> { override val context: CoroutineContext = object :
			AbstractCoroutineContextElement(ContinuationInterceptor), ContinuationInterceptor, Delay {
			override val key: CoroutineContext.Key<*> = ContinuationInterceptor.Key
			override fun <T> interceptContinuation(continuation: Continuation<T>): Continuation<T> = continuation.also { suspendCount++ }
			override fun scheduleResumeAfterDelay(timeMillis: Long, continuation: CancellableContinuation<Unit>) = continuation.resume(Unit)
		}

		override fun resumeWith(result: Result<T?>) {
			val exception = result.exceptionOrNull()
			if (exception != null) {
				resultEx = exception
				completed = true
				//println("COMPLETED WITH EXCEPTION: exception=$exception")
				exception.printStackTrace()
			} else {
				val value = result.getOrThrow()
				val rvalue = value
					?: (Unit as T) // @TODO: Kotlin-js BUG returns undefined instead of Unit! In runBlockingNoSuspensions { uncompress(i.toAsyncInputStream(), o.toAsyncOutputStream()) }
				//if (rvalue == null) error("ERROR: unexpected completed value=$value, rvalue=$rvalue, suspendCount=$suspendCount")
				rresult = rvalue
				completed = true
				//println("COMPLETED WITH RESULT: result=$result, value=$rvalue")
			}
		}
	})
	if (!completed) invalidOp("runBlockingNoSuspensions was not completed synchronously! suspendCount=$suspendCount")
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

private inline fun <T> withCoroutineContext(context: CoroutineContext, countOrElement: Any?, block: () -> T): T = block()

// Fails on JS:     InvalidOperationException: ioSync completed=true, result=null, resultEx=null, suspendCount=3015
///**
// * Allows to execute a suspendable block as long as you can ensure no suspending will happen at all..
// */
//fun <T : Any> runBlockingNoSuspensions(callback: suspend () -> T): T {
//	var completed = false
//	var result: T? = null
//	var resultEx: Throwable? = null
//	var suspendCount = 0
//
//	callback.startCoroutineUndispatched(object : Continuation<T> {
//		override val context: CoroutineContext = object : ContinuationInterceptor {
//			override val key: CoroutineContext.Key<*> = ContinuationInterceptor.Key
//
//			override fun <T> interceptContinuation(continuation: Continuation<T>): Continuation<T> {
//				suspendCount++
//				return continuation
//			}
//		}
//		override fun resume(value: T) {
//			result = value
//			completed = true
//			println("COMPLETED WITH RESULT: result=$result")
//		}
//		override fun resumeWithException(exception: Throwable) {
//			resultEx = exception
//			completed = true
//			println("COMPLETED WITH EXCEPTION: exception=$exception")
//			exception.printStackTrace()
//		}
//	})
//	if (!completed) invalidOp("ioSync was not completed synchronously! suspendCount=$suspendCount")
//	if (resultEx != null) throw resultEx!!
//	if (result != null) return result!!
//	invalidOp("ioSync completed=$completed, result=$result, resultEx=$resultEx, suspendCount=$suspendCount")
//}
