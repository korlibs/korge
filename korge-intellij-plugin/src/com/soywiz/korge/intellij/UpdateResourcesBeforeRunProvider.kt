package com.soywiz.korge.intellij

import com.intellij.execution.BeforeRunTask
import com.intellij.execution.BeforeRunTaskProvider
import com.intellij.execution.CommonJavaRunConfigurationParameters
import com.intellij.execution.configurations.RunConfiguration
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.LibraryOrderEntry
import com.intellij.openapi.util.Key
import org.jdom.Element

open class KorgeUpdateResourceBeforeRunProvider : BeforeRunTaskProvider<UpdateResourceBeforeRunTask>() {
	override fun getDescription(p0: UpdateResourceBeforeRunTask?): String = "KorgeUpdateResourceBeforeRunProvider"

	override fun getName(): String = "KorgeUpdateResourceBeforeRunProvider"

	override fun createTask(runConfiguration: RunConfiguration) = when (runConfiguration) {
		is CommonJavaRunConfigurationParameters -> UpdateResourceBeforeRunTask(runConfiguration)
		else -> null
	}

	override fun getId(): Key<UpdateResourceBeforeRunTask> = UpdateResourceBeforeRunTask.KEY

	override fun executeTask(
		p0: DataContext?,
		runConfiguration: RunConfiguration,
		executionEnvironment: ExecutionEnvironment,
		p3: UpdateResourceBeforeRunTask?
	): Boolean {
		val project = executionEnvironment.project
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

	override fun canExecuteTask(p0: RunConfiguration?, p1: UpdateResourceBeforeRunTask?): Boolean {
		return true
	}

	override fun configureTask(p0: RunConfiguration?, p1: UpdateResourceBeforeRunTask?): Boolean {
		return true
	}
}

data class UpdateResourceBeforeRunTask(val runConfiguration: CommonJavaRunConfigurationParameters) :
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
