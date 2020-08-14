package com.soywiz.korim.format

import com.soywiz.korio.async.*
import kotlin.native.concurrent.*
import kotlin.native.internal.*

@PublishedApi
internal val CommonImageIOWorker by lazy { Worker.start().also { kotlin.native.Platform.isMemoryLeakCheckerActive = false } }

suspend inline fun <T> executeInImageIOWorker(execute: (Worker) -> Future<T>): T {
    //val worker = Worker.start()
    //try {
    //    return execute(worker).await()
    //} finally {
    //    worker.requestTermination()
    //}
    return execute(CommonImageIOWorker).await()
}
