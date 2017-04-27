package com.soywiz.korge.intellij

import com.intellij.execution.BeforeRunTask
import com.intellij.execution.BeforeRunTaskProvider
import com.intellij.execution.CommonJavaRunConfigurationParameters
import com.intellij.execution.configurations.RunConfiguration
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.roots.OrderRootType
import com.intellij.openapi.util.Key
import com.soywiz.korge.build.KorgeManualServiceRegistration
import com.soywiz.korge.build.ResourceProcessor
import com.soywiz.korio.async.syncTest
import com.soywiz.korio.vfs.toVfs
import org.jdom.Element
import org.jetbrains.jps.model.java.JavaResourceRootType
import org.jetbrains.jps.model.module.JpsModuleSourceRootType
import java.io.File
import java.net.URI

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
		project.runBackgroundTaskWithProgress { progress ->
			KorgeManualServiceRegistration.register()

			val resources = project.moduleManager.modules.flatMap { it.rootManager.getSourceRoots(JavaResourceRootType.RESOURCE) }

			val resourcesVfs = resources.map { it.toVfs() }

			val genresourcesVirtual = resourcesVfs.firstOrNull { it.basename == "genresources" }
			val resourcesVirtual = resourcesVfs.firstOrNull { it.basename == "resources" }

			println(resources)
			println(resourcesVfs)
			println("Regenerating resources")
			println("genresourcesVirtual=$genresourcesVirtual : resourcesVirtual=$resourcesVirtual")

			progress.text = "Regenerating resources"
			if (genresourcesVirtual != null && resourcesVirtual != null) {
				syncTest {
					try {
						// @TODO: Proper discovery of that folder
						val extraOutputVirtual = genresourcesVirtual["../build/resources/main"]
						println("Regenerating resources [1]")
						ResourceProcessor.process(listOf(resourcesVirtual), genresourcesVirtual, extraOutputVirtual) { pi ->
							progress.fraction = pi.fraction
							progress.text = "Processing... ${pi.file}"
						}
						println("Regenerating resources [2]")
					} catch (e: Throwable) {
						e.printStackTrace()
					}
				}
			}
			progress.text = "Done"
			println("/Regenerating resources")
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
