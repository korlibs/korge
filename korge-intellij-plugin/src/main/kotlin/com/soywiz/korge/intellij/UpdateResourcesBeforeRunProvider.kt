package com.soywiz.korge.intellij

import com.intellij.execution.*
import com.intellij.execution.configurations.*
import com.intellij.execution.runners.*
import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.project.*
import com.intellij.openapi.roots.*
import com.intellij.openapi.util.*

open class KorgeUpdateResourceBeforeRunProvider : BeforeRunTaskProvider<UpdateResourceBeforeRunTask>() {
	override fun getDescription(p0: UpdateResourceBeforeRunTask?): String = "KorgeUpdateResourceBeforeRunProvider"

	override fun getName(): String = "KorgeUpdateResourceBeforeRunProvider"

	override fun createTask(runConfiguration: RunConfiguration) = when (runConfiguration) {
		is CommonProgramRunConfigurationParameters -> UpdateResourceBeforeRunTask(runConfiguration)
		else -> null
	}

	override fun getId(): Key<UpdateResourceBeforeRunTask> = UpdateResourceBeforeRunTask.KEY

	override fun executeTask(context: DataContext, configuration: RunConfiguration, env: ExecutionEnvironment, task: UpdateResourceBeforeRunTask): Boolean {
		val project = env.project
		if (project.hasKorge()) {
			println("KORGE detected in $project")
			KorgeBuildResourcesAction.build(project)
		} else {
			println("KORGE NOT detected in $project")
		}
		return true
	}

	override fun isConfigurable(): Boolean {
		return false
	}

	override fun canExecuteTask(configuration: RunConfiguration, task: UpdateResourceBeforeRunTask): Boolean {
		return true
	}
}

data class UpdateResourceBeforeRunTask(val runConfiguration: CommonProgramRunConfigurationParameters) :
	BeforeRunTask<UpdateResourceBeforeRunTask>(KEY) {
	companion object {
		val KEY = Key<UpdateResourceBeforeRunTask>(UpdateResourceBeforeRunTask::class.java.name)
	}

	//override fun writeExternal(element: Element?) = Unit
	//override fun readExternal(element: Element?) = Unit
	//override fun setEnabled(isEnabled: Boolean) = Unit
	//override fun getItemsCount(): Int = 0
	override fun clone(): BeforeRunTask<out BeforeRunTask<*>> = this.copy()

	//override fun isEnabled(): Boolean = runConfiguration.project.hasKorge()
	override fun isEnabled(): Boolean = true
}

private fun Project.hasKorge(): Boolean {
	val project = this
	for (module in project.moduleManager.modules) {
		for (orderEntry in module.rootManager.orderEntries) {
			if (orderEntry is LibraryOrderEntry) {
				val libraryName = orderEntry.libraryName ?: ""
				if (libraryName.contains("korge-core")) return true
			}
		}
	}
	return false
}
