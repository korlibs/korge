package korlibs.korge.scene

import korlibs.time.TimeSpan
import korlibs.time.milliseconds
import korlibs.korge.view.Container
import korlibs.korge.view.SContainer
import korlibs.io.async.launchImmediately
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Deferred
import kotlin.time.*

abstract class CompletableScene<T>() : Scene() {
	private val deferred = CompletableDeferred<T>()
	val completed get() = deferred as Deferred<T>

	abstract suspend fun Container.setup()

	abstract suspend fun process(): T

	final override suspend fun SContainer.sceneInit() {
		setup()
		launchImmediately {
			try {
				deferred.complete(process())
			} catch (e: Throwable) {
				deferred.completeExceptionally(e)
			}
		}
	}
}

suspend inline fun <reified T : CompletableScene<R>, R> SceneContainer.changeToResult(
    vararg injects: Any,
    time: Duration = 0.milliseconds,
    transition: Transition = AlphaTransition
): R {
	val instance = changeTo(T::class, *injects, time = time, transition = transition)
	return instance.completed.await()
}
