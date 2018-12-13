package com.soywiz.korge.intellij.editor

import com.intellij.openapi.fileEditor.FileEditorPolicy

class KorgeBothFileEditorProvider : KorgeBaseFileEditorProvider() {
	override fun accept(
		project: com.intellij.openapi.project.Project,
		virtualFile: com.intellij.openapi.vfs.VirtualFile
	): Boolean {
		val name = virtualFile.name
		return when {
			name.endsWith(".svg", ignoreCase = true) -> true
			name.endsWith(".pex", ignoreCase = true) -> true
			name.endsWith(".tmx", ignoreCase = true) -> true
			name.endsWith(".scml", ignoreCase = true) -> true
			else -> false
		}
	}

	override fun getPolicy(): FileEditorPolicy = FileEditorPolicy.PLACE_BEFORE_DEFAULT_EDITOR
}
