package com.soywiz.korge.intellij

import com.intellij.framework.*
import com.intellij.framework.addSupport.*
import com.intellij.ide.util.frameworkSupport.*
import com.intellij.openapi.module.*
import com.intellij.openapi.roots.*
import javax.swing.*

/*
class KorgeFramework : FrameworkTypeEx("Korge") {
	override fun createProvider(): FrameworkSupportInModuleProvider {
		return object : FrameworkSupportInModuleProvider() {
			override fun getFrameworkType(): FrameworkTypeEx {
				return this@KorgeFramework
			}

			override fun isEnabledForModuleType(moduleType: ModuleType<*>): Boolean {
				return moduleType == JavaModuleType.getModuleType()
			}

			override fun createConfigurable(model: FrameworkSupportModel): FrameworkSupportInModuleConfigurable {
				return object : FrameworkSupportInModuleConfigurable() {
					override fun addSupport(
						module: Module,
						rootModel: ModifiableRootModel,
						modifiableModelsProvider: ModifiableModelsProvider
					) {
					}

					override fun createComponent(): JComponent? {
						return JCheckBox("Extra Option")
					}
				}
			}
		}
	}

	override fun getPresentableName(): String {
		return "KorGE"
	}

	override fun getIcon(): Icon {
		return KorgeIcons.KORGE
	}
}
*/