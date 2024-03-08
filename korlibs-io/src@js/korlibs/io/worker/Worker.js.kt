package korlibs.io.worker

import korlibs.io.*
import korlibs.io.async.*
import korlibs.io.lang.*
import korlibs.js.*
import korlibs.platform.*
import kotlinx.coroutines.*
import org.w3c.dom.events.*
import org.w3c.workers.*
import kotlin.coroutines.*
import kotlin.js.Promise
import kotlin.reflect.*

actual typealias WorkerExport = JsExport

//val isWorker: Boolean get() = js("(typeof importScripts === 'function')").unsafeCast<Boolean>()
val isWorker: Boolean get() = js("(typeof DedicatedWorkerGlobalScope === 'function')").unsafeCast<Boolean>()
var workerUrl: String? = js("(globalThis.location)") ?: (if (isDenoJs) Deno.mainModule else ".")

@PublishedApi
internal actual val workerImpl: _WorkerImpl = object : _WorkerImpl() {
    override val isAvailable: Boolean get() = workerUrl != null

    override fun insideWorker(): Boolean {
        //println("import.meta.url: " + js("(import.meta.url)"))
        //println("DedicatedWorkerGlobalScope: " + js("(globalThis.DedicatedWorkerGlobalScope)"))
        //println("isWorker: $isWorker")
        //if (isDenoJs) println("Deno.mainModule: " + js("(Deno.mainModule)"))
        workerUrl = when {
            isDenoJs -> Deno.mainModule
            else -> JSStackTrace.current().entries.lastOrNull()?.file
        }

        if (workerUrl == null) {
            println("workerUrl = null, STACKTRACE:" + Exception().stackTraceToString())
        }
        //Exception().printStackTrace()

        if (isWorker && isAvailable) {
            jsGlobalThis.asDynamic().onmessage = { evt: ServiceWorkerMessageEvent ->
                val input = evt.data.unsafeCast<Array<Any?>>()
                val id = input[0].unsafeCast<String>()
                val moduleUrl = input[1].unsafeCast<String?>() ?: workerUrl ?: "."
                val clazzName = input[2].unsafeCast<String>()
                val params = input.drop(3)
                //console.log("RECEIVED EVENT", input)
                //console.log("jsImport(mainUrl): ${mainUrl}")
                val imported = (jsImport(moduleUrl) as Promise<*>).then { imported ->
                    //console.log("RECEIVED EVENT ---> imported", imported)
                    val taskClass = imported.asDynamic()[clazzName]
                    val workerTask = jsNew(taskClass).unsafeCast<WorkerTask>()
                    workerTask.params = params.toList()
                    launchImmediately(EmptyCoroutineContext) {
                        try {
                            workerTask.execute()
                            val result = workerTask.result
                            when (result) {
                                is Deferred<*> -> jsGlobalThis.asDynamic().postMessage(arrayOf(id, "ok", result.await()))
                                else -> jsGlobalThis.asDynamic().postMessage(arrayOf(id, "ok", result))
                            }
                            Unit
                        } catch (e: dynamic) {
                            jsGlobalThis.asDynamic().postMessage(arrayOf(id, "error", e.stack))
                        }
                    }
                }
            }
        }

        return isWorker
    }

    override fun createWorker(): Any? {
        //return js("(new Worker(import.meta.url, { type: 'module', deno: { permissions: 'inherit' } }))");
        val workerUrl = workerUrl

        if (DEBUG_WORKER) println("CREATE WORKER at URL: $workerUrl")

        val worker: org.w3c.dom.Worker =
            js("(new Worker(workerUrl, { type: 'module', deno: { permissions: 'inherit' } }))")
        Environment.getAll()
        EnvironmentInternal.getAll()
        //worker.postMessage("url", jsObject(Environment.getAll()))
        return worker
    }

    private var lastId = 0

    fun <T : Any> KClass<T>.instantiate(): T {
        val dthis = this.asDynamic()
        for (key in JsObject.keys(dthis)) {
            val value = dthis[key]
            if (value !is String) {
                return jsNew(value)
            }
        }
        //return jsNew(this)
        error("Can't instantiate")
    }

    override suspend fun <T : WorkerTask> execute(
        worker: Any?,
        clazz: KClass<T>,
        create: () -> T,
        params: Array<out Any?>
    ): Any? {
        if (!isAvailable) {
            return super.execute(worker, clazz, create, params)
        }

        val stacktrace = JSStackTrace.parse(clazz.instantiate().getModuleStacktrace())
        val module = stacktrace.entries[1].file

        val deferred = CompletableDeferred<Any?>()
        val messageId = lastId++
        val worker = worker.unsafeCast<org.w3c.dom.Worker>()
        var func: ((Event) -> Unit)? = null
        func = { evt: Event ->
            val e = evt.unsafeCast<ServiceWorkerMessageEvent>()
            val input = e.data.unsafeCast<Array<Any?>>()
            val id = input[0]
            val kind = input[1]
            val result = input[2]
            if (messageId == id) {
                //console.log("RECEIVED MESSAGE FROM WORKER", input)
                worker.removeEventListener("message", func)
                if (kind == "ok") deferred.complete(result) else deferred.completeExceptionally(Throwable("$result"))
            }
        }
        worker.addEventListener("message", func)

        worker.unsafeCast<org.w3c.dom.Worker>().postMessage(arrayOf(messageId) + arrayOf(module, clazz.simpleName, *params))
        //super.sendMessage(worker, message)

        return deferred.await()
    }
}

