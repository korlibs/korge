package com.soywiz.korge.intellij

import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.project.*
import com.soywiz.korio.file.*
import kotlinx.coroutines.*
import org.jetbrains.jps.model.java.*

class KorgeBuildResourcesAction : AnAction() {
	override fun actionPerformed(anActionEvent: AnActionEvent) {
		val project = anActionEvent.project
		if (project != null) KorgeBuildResourcesAction.build(project)
	}

	companion object {
		fun build(project: Project) {
			project.runBackgroundTaskWithProgress { progress ->
				//KorgeManualServiceRegistration.register()

				for (module in project.moduleManager.modules) {

					val resources = module.rootManager.getSourceRoots(JavaResourceRootType.RESOURCE)

					val resourcesVfs = resources.map { it.toVfs() }

					val resourcesVirtual = resourcesVfs.firstOrNull { it.baseName == "resources" }
					val genresourcesVirtual = resourcesVirtual?.parent?.get("genresources")

					println(resources)
					println(resourcesVfs)
					println("Regenerating resources")
					println("genresourcesVirtual=$genresourcesVirtual : resourcesVirtual=$resourcesVirtual")

					progress.text = "Regenerating resources for $module"
					if (resourcesVirtual != null && genresourcesVirtual != null) {
						runBlocking {
							genresourcesVirtual.mkdir()

							try {
								// @TODO: Proper discovery of that folder
								val extraOutputVirtual = genresourcesVirtual["../build/resources/main"]
								println("Regenerating resources [1]")
								//KorgeBuildService.processResourcesFolders()
								/*
								ResourceProcessor().process(
									listOf(resourcesVirtual),
									genresourcesVirtual,
									extraOutputVirtual
								) { pi ->
									progress.fraction = if (pi.fraction <= 0.0) 0.0001 else pi.fraction
									progress.text = "Processing... ${pi.file}"
								}
								 */
								println("Regenerating resources [2]")
							} catch (e: Throwable) {
								e.printStackTrace()
							}
						}
					}
					progress.text = "Done"
					println("/Regenerating resources")
				}
			}
		}
	}
}
