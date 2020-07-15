package com.soywiz.korio.async

import kotlinx.coroutines.*
import java.util.concurrent.*
import kotlin.coroutines.*

class EventLoopExecutorService(val context: CoroutineContext) : ExecutorService {
	var shutdown = false

	override fun shutdown() = run { shutdown = true }
	override fun shutdownNow(): MutableList<Runnable> = ArrayList<Runnable>().apply { shutdown() }
	override fun isShutdown(): Boolean = shutdown

	override fun <T : Any?> submit(task: Callable<T>?): Future<T> {
		TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
	}

	override fun <T : Any?> submit(task: Runnable?, result: T): Future<T> {
		TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
	}

	override fun submit(task: Runnable?): Future<*> = submit(task, Unit)
	override fun awaitTermination(timeout: Long, unit: TimeUnit?): Boolean = true

	override fun <T : Any?> invokeAny(tasks: MutableCollection<out Callable<T>>?): T {
		TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
	}

	override fun <T : Any?> invokeAny(tasks: MutableCollection<out Callable<T>>?, timeout: Long, unit: TimeUnit?): T {
		TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
	}

	override fun <T : Any?> invokeAll(tasks: MutableCollection<out Callable<T>>?): MutableList<Future<T>> {
		TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
	}

	override fun <T : Any?> invokeAll(
		tasks: MutableCollection<out Callable<T>>?,
		timeout: Long,
		unit: TimeUnit?
	): MutableList<Future<T>> {
		TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
	}

	override fun isTerminated(): Boolean = true

	override fun execute(command: Runnable) {
		launchImmediately(context) {
			command.run()
		}
		//context.dispatcher.dispatch(context, Runnable { command.run() })
	}
}