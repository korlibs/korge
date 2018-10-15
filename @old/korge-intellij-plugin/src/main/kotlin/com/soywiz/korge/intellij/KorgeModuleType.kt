package com.soywiz.korge.intellij

import com.intellij.ide.util.projectWizard.JavaModuleBuilder
import com.intellij.openapi.module.JavaModuleType
import com.intellij.openapi.module.ModuleType
import com.intellij.openapi.roots.ModifiableRootModel
import com.intellij.openapi.roots.OrderEntry
import com.intellij.openapi.roots.libraries.Library
import com.intellij.openapi.util.Pair
import org.jetbrains.annotations.NonNls
import org.jetbrains.jps.model.java.JavaResourceRootType
import org.jetbrains.jps.model.module.JpsModuleSourceRootType
import java.io.File
import java.util.ArrayList
import javax.swing.Icon

open class KorgeModuleType : JavaModuleType("korge") {
	companion object {
		val INSTANCE = KorgeModuleType()
	}

	override fun createModuleBuilder(): JavaModuleBuilder {
		return object : JavaModuleBuilder() {
			override fun getModuleType(): ModuleType<*> {
				return INSTANCE
			}

			override fun setupRootModel(rootModel: ModifiableRootModel) {
				super.setupRootModel(rootModel);
				/*
				val contentEntry = rootModel.contentEntries.first()

				contentEntry.file!!.createChildDirectory(null, "genresources")
				contentEntry.file!!.createChildDirectory(null, "resources")
				contentEntry.addSourceFolder("genresources", JavaResourceRootType.RESOURCE)
				contentEntry.addSourceFolder("resources", JavaResourceRootType.RESOURCE)
				*/
			}


			override fun getSourcePaths(): MutableList<Pair<String, String>> {
				return super.getSourcePaths()
			}
		}
	}

	override fun getName(): String = "Korge"
	override fun getDescription(): String = "KorGE Game Engine"
	//override fun getBigIcon(): Icon? = KorgeIcons.KORGE
	override fun getNodeIcon(isOpened: Boolean): Icon = KorgeIcons.KORGE
}
