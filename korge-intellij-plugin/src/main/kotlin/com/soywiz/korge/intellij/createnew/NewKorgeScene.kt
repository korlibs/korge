package com.soywiz.korge.intellij.createnew

import com.intellij.ide.actions.*
import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.project.*
import com.intellij.psi.*
import com.soywiz.korge.intellij.*

class NewKorgeScene : CreateFileFromTemplateAction(
	"Korge Scene",
	"Creates new Korge Scene in Kotlin",
	KorgeIcons.KORGE
), DumbAware {
	override fun getActionName(directory: PsiDirectory?, newName: String, templateName: String?): String = "Korge Scene"

	override fun isAvailable(dataContext: DataContext): Boolean = dataContext.project?.korge?.containsKorge ?: false

	override fun buildDialog(
		project: Project,
		directory: PsiDirectory,
		builder: CreateFileFromTemplateDialog.Builder
	) {
		builder
			.setTitle("New Korge Scene")
			.addKind("Scene", KorgeIcons.KORGE, "KorgeScene")
	}

	//override fun createFileFromTemplate(name: String?, template: FileTemplate?, dir: PsiDirectory?): PsiFile? {
	//	return createFile(name, "KorgeScene.kt", dir)
	//}
}
