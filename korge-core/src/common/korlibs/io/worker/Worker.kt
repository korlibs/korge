package korlibs.io.worker

import korlibs.io.async.*
import korlibs.io.lang.*
import kotlinx.coroutines.*
import kotlin.coroutines.*
import kotlin.reflect.*

@PublishedApi
internal expect val workerImpl: _WorkerImpl

expect annotation class WorkerExport()

val DEBUG_WORKER = Environment["DEBUG_WORKER"] == "true"

open class _WorkerImpl {
    open val isAvailable: Boolean get() = true
    open fun init(): Unit = Unit
    open fun insideWorker(): Boolean = false
    open fun createWorker(): Any? = null
    open fun destroyWorker(worker: Any?) = Unit
    open suspend fun <T : WorkerTask> execute(worker: Any?, clazz: KClass<T>, create: () -> T, params: Array<out Any?>): Any? {
        val task = create()
        task.params = params.toList()
        task.execute()
        val result = task.result
        return when (result) {
            is Deferred<*> -> result.await()
            else -> result
        }
    }
}

@WorkerExport
class DemoWorkerTask : WorkerTask() {
    override fun execute() = runSuspend { params ->
        println("TEST!!!!! $params")
        //delay(1.seconds)
        //error("ERROR!")
        return@runSuspend 11
    }
}

open class WorkerTask {
    private var stackTrace: String? = null
    private var gettingStackTrace = false
    internal var params = listOf<Any?>()
        get() {
            if (!runSuspend) error("Must wrap function around runSuspend")
            return field
        }
    internal var result: Any? = null
    private var runSuspend = false
    protected fun runSuspend(block: suspend (params: List<Any?>) -> Any?) {
        runSuspend = true
        try {
            if (gettingStackTrace) {
                stackTrace = currentStackTrace()
                return
            }
            val deferred = CompletableDeferred<Any?>()
            launchImmediately(EmptyCoroutineContext) {
                deferred.completeWith(runCatching { block(this.params) })
            }
            result = deferred
        } finally {
            runSuspend = false
        }
    }
    open fun execute() = Unit
    fun getModuleStacktrace(): String {
        gettingStackTrace = true
        try {
            execute()
            return stackTrace!!
        } finally {
            gettingStackTrace = false
        }
    }
}

class Worker : Closeable {
    val id = workerImpl.createWorker()

    suspend inline fun <reified T : WorkerTask> execute(noinline create: () -> T, vararg params: Any?): Any? {
        return workerImpl.execute(id, T::class, create, params)
    }

    companion object {
        inline fun init(block: () -> Unit): Unit {
            workerImpl.init()
            if (workerImpl.insideWorker()) {
                if (DEBUG_WORKER) println("INSIDE WORKER")
                return
            } else {
                if (DEBUG_WORKER) println("**NOT** INSIDE WORKER")
            }
            block()
        }

        //fun register(kClass: KClass<out WorkerTask>) {
        //    println("REGISTER: $kClass")
        //}
    }

    override fun close() {
        workerImpl.destroyWorker(id)
    }
}
