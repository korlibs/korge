package com.soywiz.korge.intellij

import com.intellij.execution.BeforeRunTask
import com.intellij.execution.BeforeRunTaskProvider
import com.intellij.execution.CommonJavaRunConfigurationParameters
import com.intellij.execution.configurations.RunConfiguration
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.util.Key
import org.jdom.Element

open class KorgeUpdateResourceBeforeRunProvider : BeforeRunTaskProvider<UpdateResourceBeforeRunTask>() {
	override fun getDescription(p0: UpdateResourceBeforeRunTask?): String = "KorgeUpdateResourceBeforeRunProvider"

	override fun getName(): String = "KorgeUpdateResourceBeforeRunProvider"

	override fun createTask(p0: RunConfiguration?): UpdateResourceBeforeRunTask? {
		if (p0 is CommonJavaRunConfigurationParameters) {
			return UpdateResourceBeforeRunTask(p0)
		}
		return null
	}

	override fun getId(): Key<UpdateResourceBeforeRunTask> = UpdateResourceBeforeRunTask.KEY

	override fun executeTask(p0: DataContext?, runConfiguration: RunConfiguration, executionEnvironment: ExecutionEnvironment, p3: UpdateResourceBeforeRunTask?): Boolean {
		val project = executionEnvironment.project
		println("[1]")
		project.runBackgroundTaskWithProgress { progress ->
			progress.fraction = 0.10
			progress.text = "90% to finish"
			Thread.sleep(1000L)
			progress.fraction = 0.50
			progress.text = "50% to finish"
			Thread.sleep(1000L)
			progress.fraction = 1.0
			progress.text = "finished"
			Thread.sleep(1000L)
			println("[2]")
		}
		println("[3]")
		return true
	}

	override fun isConfigurable(): Boolean {
		return false
	}

	override fun canExecuteTask(p0: RunConfiguration?, p1: UpdateResourceBeforeRunTask?): Boolean {
		return true
	}

	override fun configureTask(p0: RunConfiguration?, p1: UpdateResourceBeforeRunTask?): Boolean {
		return true
	}
}

data class UpdateResourceBeforeRunTask(val runConfiguration: CommonJavaRunConfigurationParameters) : BeforeRunTask<UpdateResourceBeforeRunTask>(KEY) {
	companion object {
		val KEY = Key<UpdateResourceBeforeRunTask>(UpdateResourceBeforeRunTask::class.java.name)
	}

	override fun writeExternal(element: Element?) = Unit
	override fun readExternal(element: Element?) = Unit
	override fun setEnabled(isEnabled: Boolean) = Unit
	override fun getItemsCount(): Int = 0
	override fun clone(): BeforeRunTask<out BeforeRunTask<*>> = this.copy()
	override fun isEnabled(): Boolean = true
}
