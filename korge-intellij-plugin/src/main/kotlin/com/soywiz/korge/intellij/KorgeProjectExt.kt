package com.soywiz.korge.intellij

import com.intellij.openapi.project.*
import com.intellij.openapi.util.*

class KorgeProjectExt(val project: Project) {
	companion object {
		val KEY = Key.create<KorgeProjectExt>("korge.project.ext")
	}

	private var checkRootManagerVersion: Long? = null
	private var containsKorgeCached: Boolean = false

	val containsKorge: Boolean
		get() {
			if (checkRootManagerVersion == null || checkRootManagerVersion != project.rootManager.modificationCount) {
				checkRootManagerVersion = project.rootManager.modificationCount
				containsKorgeCached =
					project.rootManager.orderEntries().librariesOnly().toLibrarySequence().any { it.name?.contains("com.soywiz.korlibs.korge") == true }
				//println("Computed $containsKorgeCached")
			}
			return containsKorgeCached
		}
}

val Project.korge: KorgeProjectExt get() = this.getOrPutUserData(KorgeProjectExt.KEY) { KorgeProjectExt(this) }

//val DataContext.project: Project? get() = getData(CommonDataKeys.PROJECT)
//val Project.rootManager: ProjectRootManager get() = ProjectRootManager.getInstance(this)
