package korlibs.io.worker

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.*
import java.io.*
import java.util.concurrent.atomic.*
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
    override fun init() {
        super.init()
        // @TODO: Required for now for Java >= 19
        // Eventually we should render everything into a texture, and then draw it with metal or OpenGL.
        // For metal: https://developer.apple.com/documentation/metal/metal_sample_code_library/mixing_metal_and_opengl_rendering_in_a_view
        // Or directly render with metal.
        // We could also investigate:
        // - Dawn: https://github.com/hexops/dawn
        // - Angle: https://github.com/google/angle
        // Alternatively we could create a native Window using JNA/FFI and not relying on Java AWT
        System.setProperty("sun.java2d.metal", "false")
        System.setProperty("sun.java2d.opengl", "true")
    }

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

    private var lastId = AtomicInteger(0)

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
