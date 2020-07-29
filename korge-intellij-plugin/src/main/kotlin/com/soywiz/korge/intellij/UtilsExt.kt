package com.soywiz.korge.intellij

import com.intellij.codeInsight.completion.*
import com.intellij.facet.*
import com.intellij.ide.util.*
import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.application.*
import com.intellij.openapi.components.*
import com.intellij.openapi.module.*
import com.intellij.openapi.progress.*
import com.intellij.openapi.project.*
import com.intellij.openapi.roots.*
import com.intellij.openapi.roots.libraries.*
import com.intellij.openapi.util.*
import com.intellij.util.*
import java.awt.*
import java.util.concurrent.*

fun <T> runReadAction(callback: () -> T): T {
	return ApplicationManager.getApplication().runReadAction(Computable {
		callback()
	})
}

val propertiesComponent: PropertiesComponent get() = PropertiesComponent.getInstance()
val Project.propertiesComponent: PropertiesComponent get() = PropertiesComponent.getInstance()

inline fun <reified T> Project.getService(): T = ServiceManager.getService(this, T::class.java)
inline fun <reified T> getService(): T = ServiceManager.getService(T::class.java)

inline fun <T> UserDataHolder.getOrPutUserData(key: Key<T>, gen: (key: Key<T>) -> T): T = getUserData(key) ?: gen(key).also { putUserData(key, it) }

val DataContext.project: Project? get() = getData(CommonDataKeys.PROJECT)
val Project.rootManager get() = ProjectRootManager.getInstance(this)
val Project.moduleManager get() = com.intellij.openapi.module.ModuleManager.getInstance(this)

val Module.rootManager get() = com.intellij.openapi.roots.ModuleRootManager.getInstance(this)
val Module.facetManager get() = FacetManager.getInstance(this)

fun OrderEnumerator.toLibrarySequence(): Sequence<Library> = sequence {
	val items = arrayListOf<Library>()
	this@toLibrarySequence.forEachLibrary {
		items.add(it)
		true
	}
	yieldAll(items)
}

fun runInUiThread(callback: () -> Unit) {
	EventQueue.invokeLater(Runnable {
		callback()
	})
}

fun runBackgroundTaskGlobal(callback: () -> Unit) {
	Thread {
		callback()
	}.also { it.isDaemon = true }.start()
}

fun Project.runBackgroundTaskWithProgress(callback: (ProgressIndicator) -> Unit) {
	var error: Throwable? = null
	val sema = Semaphore(1)

	sema.acquire()

	val project = this
	ProgressManager.getInstance()
		.run(object : Task.Backgroundable(project, "Title", false, PerformInBackgroundOption.ALWAYS_BACKGROUND) {
			override fun onCancel() {
				println("Cancel")
			}

			override fun run(progressIndicator: ProgressIndicator) {
				try {
					callback(progressIndicator)
				} catch (e: Throwable) {
					e.printStackTrace()
					error = e
				} finally {
					sema.release()
				}
			}
		})

	sema.acquire()

	if (error != null) throw error!!
}

fun CompletionProvider(callback: (
	parameters: CompletionParameters,
	context: ProcessingContext,
	result: CompletionResultSet
) -> Unit): CompletionProvider<CompletionParameters> = object : CompletionProvider<CompletionParameters>() {
	override fun addCompletions(
		parameters: CompletionParameters,
		context: ProcessingContext,
		result: CompletionResultSet
	) {
		callback(parameters, context, result)
	}
}
