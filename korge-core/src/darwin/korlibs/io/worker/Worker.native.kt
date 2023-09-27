package korlibs.io.worker

import korlibs.datastructure.thread.*
import korlibs.io.lang.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.*
import kotlin.concurrent.*
import kotlin.reflect.*

actual annotation class WorkerExport()

private class WorkerInfo(
    val requestChannel: Channel<ExecuteInfo<*>>,
    val thread: NativeThread
) : Closeable {
    override fun close() {
        thread.interrupt()
    }
}

private data class ExecuteInfo<T : WorkerTask>(
    val id: Int,
    val clazz: KClass<T>,
    val create: () -> T,
    val params: List<Any?>,
    val deferred: CompletableDeferred<Any?>,
)

@PublishedApi
internal actual val workerImpl: _WorkerImpl = object : _WorkerImpl() {
    override fun insideWorker(): Boolean {
        return false
    }

    override fun createWorker(): Any? {
        val requestChannel = Channel<ExecuteInfo<*>>()
        val thread = nativeThread {
            try {
                runBlocking {
                    while (true) {
                        val item = requestChannel.receive()
                        //println("item: $item")
                        item.deferred.completeWith(runCatching {
                            val instance = item.create()
                            instance.params = item.params
                            instance.execute()
                            var result = instance.result
                            if (result is Deferred<*>) result = result.await()
                            result
                        })
                        //responseChannel.send(ResponseInfo("lol"))
                    }
                }
            } catch (_: Throwable) {
            }
        }
        return WorkerInfo(requestChannel, thread)
    }

    override fun destroyWorker(worker: Any?) {
        (worker as WorkerInfo).close()
        //super.destroyWorker(worker)
    }

    private var lastId = AtomicInt(0)

    override suspend fun <T : WorkerTask> execute(
        worker: Any?,
        clazz: KClass<T>,
        create: () -> T,
        params: Array<out Any?>
    ): Any? {
        val id = lastId.getAndIncrement()
        (worker as WorkerInfo)
        val deferred = CompletableDeferred<Any?>()
        worker.requestChannel.send(ExecuteInfo(id, clazz, create, params.toList(), deferred))
        return deferred.await()
    }
}
