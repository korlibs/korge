package com.soywiz.korge.intellij

import com.intellij.ide.util.projectWizard.ModuleBuilder
import com.intellij.ide.util.projectWizard.ModuleWizardStep
import com.intellij.ide.util.projectWizard.WizardContext
import com.intellij.openapi.module.JavaModuleType
import com.intellij.openapi.module.ModuleType
import com.intellij.openapi.options.ConfigurationException
import com.intellij.openapi.roots.ModifiableRootModel
import com.intellij.openapi.roots.ui.configuration.ModulesProvider
import javax.swing.JComponent
import javax.swing.JLabel


class KorgeModuleWizardStep : ModuleBuilder() {
	@Throws(ConfigurationException::class)
	override fun setupRootModel(rootModel: ModifiableRootModel) {
		//modifiableRootModel.
	}

	override fun getModuleType(): ModuleType<*> {
		//return JavaModuleType.getModuleType()
		return ModuleType.EMPTY
	}

	override fun createWizardSteps(
		wizardContext: WizardContext,
		modulesProvider: ModulesProvider
	): Array<ModuleWizardStep> {
		return arrayOf(object : ModuleWizardStep() {
			override fun getComponent(): JComponent {
				return JLabel("Put your content here")
			}

			override fun updateDataModel() {
			}
		})
	}
}
