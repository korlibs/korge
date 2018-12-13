package com.soywiz.korge.intellij.createnew

import com.intellij.ide.actions.CreateFileFromTemplateAction
import com.intellij.ide.actions.CreateFileFromTemplateDialog
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiDirectory
import com.soywiz.korge.intellij.KorgeIcons

class NewKorgeScene : CreateFileFromTemplateAction(
	"Korge Scene",
	"Creates new Korge Scene in Kotlin",
	KorgeIcons.KORGE
), DumbAware {
	override fun getActionName(directory: PsiDirectory?, newName: String?, templateName: String?): String =
		"Korge Scene"

	override fun buildDialog(
		project: Project?,
		directory: PsiDirectory?,
		builder: CreateFileFromTemplateDialog.Builder?
	) {
		builder?.setTitle("New Korge Scene")
			?.addKind("Scene", KorgeIcons.KORGE, "KorgeScene")
	}

	//override fun createFileFromTemplate(name: String?, template: FileTemplate?, dir: PsiDirectory?): PsiFile? {
	//	return createFile(name, "KorgeScene.kt", dir)
	//}
}
