package com.soywiz.korge.intellij.createnew

import com.intellij.ide.actions.*
import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.project.*
import com.intellij.psi.*
import com.soywiz.korge.intellij.*

class NewTiledMap : CreateFileFromTemplateAction(
	"Particle Emitter",
	"Creates new .pex Particle Emitter",
	KorgeIcons.PARTICLE
), DumbAware {
	override fun getActionName(directory: PsiDirectory?, newName: String, templateName: String?): String = "Particle Emitter"

	override fun isAvailable(dataContext: DataContext): Boolean = dataContext.project?.korge?.containsKorge ?: false

	override fun buildDialog(
		project: Project,
		directory: PsiDirectory,
		builder: CreateFileFromTemplateDialog.Builder
	) {
		builder
			.setTitle("New Particle Emitter")
			.addKind("ParticleEmitter", KorgeIcons.PARTICLE, "ParticleEmitter")
	}

	//override fun createFileFromTemplate(name: String?, template: FileTemplate?, dir: PsiDirectory?): PsiFile? {
	//	return createFile(name, "KorgeScene.kt", dir)
	//}
}


