package com.soywiz.korge.intellij

import com.intellij.framework.FrameworkTypeEx
import com.intellij.framework.addSupport.FrameworkSupportInModuleConfigurable
import com.intellij.framework.addSupport.FrameworkSupportInModuleProvider
import com.intellij.ide.util.frameworkSupport.FrameworkSupportModel
import com.intellij.openapi.module.JavaModuleType
import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleType
import com.intellij.openapi.roots.ModifiableModelsProvider
import com.intellij.openapi.roots.ModifiableRootModel

import javax.swing.*

class KorgeFramework : FrameworkTypeEx("korge") {
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
