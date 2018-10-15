package com.soywiz.korge.intellij

import com.intellij.facet.FacetManager
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.module.Module
import com.intellij.openapi.progress.PerformInBackgroundOption
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.openapi.util.Computable
import java.util.concurrent.Semaphore

fun <T> runReadAction(callback: () -> T): T {
	return ApplicationManager.getApplication().runReadAction(Computable {
		callback()
	})
}

val Project.rootManager get() = ProjectRootManager.getInstance(this)
val Project.moduleManager get() = com.intellij.openapi.module.ModuleManager.getInstance(this)

val Module.rootManager get() = com.intellij.openapi.roots.ModuleRootManager.getInstance(this)
val Module.facetManager get() = FacetManager.getInstance(this)

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
