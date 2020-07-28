package com.soywiz.korge.intellij.createnew

import com.intellij.ide.actions.*
import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.project.*
import com.intellij.psi.*
import com.soywiz.korge.intellij.*

class NewTiledMap : CreateFileFromTemplateAction(
	"Tiled Map",
	"Creates new .tmx Tile Map",
	KorgeIcons.TILED
), DumbAware {
	override fun getActionName(directory: PsiDirectory?, newName: String, templateName: String?): String = "Tiled Map"

	override fun isAvailable(dataContext: DataContext): Boolean = dataContext.project?.korge?.containsKorge ?: false

	override fun buildDialog(
		project: Project,
		directory: PsiDirectory,
		builder: CreateFileFromTemplateDialog.Builder
	) {
		builder
			.setTitle("New Tiled Map")
			.addKind("TileMap", KorgeIcons.TILED, "TiledMap")
	}

	//override fun createFileFromTemplate(name: String?, template: FileTemplate?, dir: PsiDirectory?): PsiFile? {
	//	return createFile(name, "KorgeScene.kt", dir)
	//}
}
