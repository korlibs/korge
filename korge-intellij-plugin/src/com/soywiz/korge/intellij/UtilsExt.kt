package com.soywiz.korge.intellij

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.progress.PerformInBackgroundOption
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ProjectRootManager
import java.util.concurrent.Semaphore
import java.util.concurrent.locks.ReentrantLock

fun runReadAction(callback: () -> Unit) {
	ApplicationManager.getApplication().runReadAction {
		callback()
	}
}

val Project.rootManager get() = ProjectRootManager.getInstance(this)
val Project.moduleManager get() = com.intellij.openapi.module.ModuleManager.getInstance(this)

fun Project.runBackgroundTaskWithProgress(callback: (ProgressIndicator) -> Unit) {
	val sema = Semaphore(1)

	sema.acquire()

	val project = this
	ProgressManager.getInstance().run(object : Task.Backgroundable(project, "Title", false, PerformInBackgroundOption.ALWAYS_BACKGROUND) {
		override fun onCancel() {
			println("Cancel")
		}

		override fun run(progressIndicator: ProgressIndicator) {
			try {
				callback(progressIndicator)
			} finally {
				sema.release()
			}
		}
	})

	sema.acquire()
}
