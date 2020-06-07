package com.soywiz.korge.scene

import com.soywiz.klock.*
import com.soywiz.korge.internal.*
import com.soywiz.korge.view.*
import com.soywiz.korio.async.*
import kotlinx.coroutines.*

abstract class CompletableScene<T>() : Scene() {
	private val deferred = CompletableDeferred<T>()
	val completed get() = deferred as Deferred<T>

	abstract suspend fun Container.setup()

	abstract suspend fun process(): T

	final override suspend fun Container.sceneInit() {
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
    time: TimeSpan = 0.milliseconds,
    transition: Transition = AlphaTransition
): R {
	val instance = changeTo(T::class, *injects, time = time, transition = transition)
	return instance.completed.await()
}
