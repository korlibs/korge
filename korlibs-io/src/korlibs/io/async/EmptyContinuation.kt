package korlibs.io.async

import kotlin.coroutines.Continuation
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

open class EmptyContinuation(override val context: CoroutineContext) : Continuation<Unit> {
	override fun resumeWith(result: Result<Unit>) {
		val exception = result.exceptionOrNull()
		exception?.printStackTrace()
	}

	companion object : EmptyContinuation(EmptyCoroutineContext)
}
