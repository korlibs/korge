package com.soywiz.korio.async

import com.soywiz.klock.*
import kotlinx.coroutines.*
import kotlin.coroutines.*
import kotlin.test.*

class PromiseTest {
    @Test
    fun test() = suspendTest {
        val startTime = DateTime.now()
        delayPromise(100).await()
        delayPromiseJob(100).await()
        delayPromiseDeferred(100).await()
        val endTime = DateTime.now()
        assertTrue(endTime - startTime >= 300.milliseconds)
    }

    fun delayPromise(timeMs: Int): Promise<Unit> = Promise<Unit> { resolve, reject ->
        launchImmediately(EmptyCoroutineContext) {
            try {
                delay(timeMs.toLong())
                resolve(Unit)
            } catch (e: Throwable) {
                reject(e)
            }
        }
    }

    fun delayPromiseJob(timeMs: Int): Promise<Unit> = launchImmediately(EmptyCoroutineContext) {
        delay(timeMs.toLong())
    }.toPromise(EmptyCoroutineContext)

    fun delayPromiseDeferred(timeMs: Int): Promise<Unit> = asyncImmediately(EmptyCoroutineContext) {
        delay(timeMs.toLong())
    }.toPromise(EmptyCoroutineContext)
}
