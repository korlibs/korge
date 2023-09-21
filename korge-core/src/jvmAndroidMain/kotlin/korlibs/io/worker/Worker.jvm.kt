package korlibs.io.worker

import kotlinx.atomicfu.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.*
import java.io.Closeable
import kotlin.concurrent.*
import kotlin.reflect.*

actual annotation class WorkerExport()

private class WorkerInfo(
    val requestChannel: Channel<ExecuteInfo>,
    val thread: Thread
) : Closeable {
    override fun close() {
        thread.interrupt()
    }
}

private data class ExecuteInfo(val id: Int, val clazz: KClass<out WorkerTask>, val params: List<Any?>, val deferred: CompletableDeferred<Any?>)

@PublishedApi
internal actual val workerImpl: _WorkerImpl = object : _WorkerImpl() {
    override fun insideWorker(): Boolean {
        return false
    }

    override fun createWorker(): Any? {
        val requestChannel = Channel<ExecuteInfo>()
        val thread = thread {
            try {
                runBlocking {
                    while (true) {
                        val item = requestChannel.receive()
                        //println("item: $item")
                        item.deferred.completeWith(runCatching {
                            val instance = item.clazz.java.getDeclaredConstructor().newInstance()
                            instance.params = item.params
                            instance.execute()
                            var result = instance.result
                            if (result is Deferred<*>) result = result.await()
                            result
                        })
                        //responseChannel.send(ResponseInfo("lol"))
                    }
                }
            } catch (_: InterruptedException) {
            }
        }
        return WorkerInfo(requestChannel, thread)
    }

    override fun destroyWorker(worker: Any?) {
        (worker as WorkerInfo).close()
        //super.destroyWorker(worker)
    }

    private var lastId = atomic(0)

    override suspend fun <T : WorkerTask> execute(
        worker: Any?,
        clazz: KClass<T>,
        create: () -> T,
        params: Array<out Any?>
    ): Any? {
        val id = lastId.getAndIncrement()
        (worker as WorkerInfo)
        val deferred = CompletableDeferred<Any?>()
        worker.requestChannel.send(ExecuteInfo(id, clazz, params.toList(), deferred))
        return deferred.await()
    }
}
