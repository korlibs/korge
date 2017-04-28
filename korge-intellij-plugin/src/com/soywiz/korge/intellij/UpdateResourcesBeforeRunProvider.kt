package com.soywiz.korge.intellij

import com.intellij.execution.BeforeRunTask
import com.intellij.execution.BeforeRunTaskProvider
import com.intellij.execution.CommonJavaRunConfigurationParameters
import com.intellij.execution.configurations.RunConfiguration
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.util.Key
import com.soywiz.korge.build.KorgeManualServiceRegistration
import com.soywiz.korge.build.ResourceProcessor
import com.soywiz.korio.async.syncTest
import org.jdom.Element
import org.jetbrains.jps.model.java.JavaResourceRootType

open class KorgeUpdateResourceBeforeRunProvider : BeforeRunTaskProvider<UpdateResourceBeforeRunTask>() {
	override fun getDescription(p0: UpdateResourceBeforeRunTask?): String = "KorgeUpdateResourceBeforeRunProvider"

	override fun getName(): String = "KorgeUpdateResourceBeforeRunProvider"

	override fun createTask(runConfiguration: RunConfiguration): UpdateResourceBeforeRunTask? {
		val project = runConfiguration.project
		if (runConfiguration is CommonJavaRunConfigurationParameters) {
			for (module in project.moduleManager.modules) {
				println(module)
			}
			return UpdateResourceBeforeRunTask(runConfiguration)
		}
		return null
	}

	override fun getId(): Key<UpdateResourceBeforeRunTask> = UpdateResourceBeforeRunTask.KEY

	override fun executeTask(p0: DataContext?, runConfiguration: RunConfiguration, executionEnvironment: ExecutionEnvironment, p3: UpdateResourceBeforeRunTask?): Boolean {
		val project = executionEnvironment.project
		KorgeBuildResourcesAction.build(project)
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
