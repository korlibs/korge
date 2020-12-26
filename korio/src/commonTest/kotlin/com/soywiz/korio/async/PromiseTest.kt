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

    suspend fun delayPromise(timeMs: Int): Promise<Unit> = delayPromise(timeMs, coroutineContext)
    suspend fun delayPromiseJob(timeMs: Int): Promise<Unit> = delayPromiseJob(timeMs, coroutineContext)
    suspend fun delayPromiseDeferred(timeMs: Int): Promise<Unit> = delayPromiseDeferred(timeMs, coroutineContext)

    fun delayPromise(timeMs: Int, coroutineContext: CoroutineContext): Promise<Unit> = Promise<Unit> { resolve, reject ->
        launchImmediately(coroutineContext) {
            try {
                delay(timeMs.toLong())
                resolve(Unit)
            } catch (e: Throwable) {
                reject(e)
            }
        }
    }

    fun delayPromiseJob(timeMs: Int, coroutineContext: CoroutineContext): Promise<Unit> = launchImmediately(coroutineContext) {
        delay(timeMs.toLong())
    }.toPromise(coroutineContext)

    fun delayPromiseDeferred(timeMs: Int, coroutineContext: CoroutineContext): Promise<Unit> = asyncImmediately(coroutineContext) {
        delay(timeMs.toLong())
    }.toPromise(coroutineContext)
}
