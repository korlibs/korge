package com.soywiz.korge.intellij.editor

import com.intellij.openapi.fileEditor.FileEditorPolicy

class KorgeHiddenFileEditorProvider : KorgeBaseFileEditorProvider() {
	override fun accept(
		project: com.intellij.openapi.project.Project,
		virtualFile: com.intellij.openapi.vfs.VirtualFile
	): Boolean {
		val name = virtualFile.name
		return when {
			name.endsWith(".swf", ignoreCase = true) -> true
			name.endsWith(".ani", ignoreCase = true) -> true
			name.endsWith(".voice.wav", ignoreCase = true) -> true
			name.endsWith(".voice.mp3", ignoreCase = true) -> true
			name.endsWith(".voice.ogg", ignoreCase = true) -> true
			name.endsWith(".voice.lipsync", ignoreCase = true) -> true
			name.endsWith(".wav", ignoreCase = true) -> true
			name.endsWith(".mp3", ignoreCase = true) -> true
			name.endsWith(".ogg", ignoreCase = true) -> true
			else -> false
		}
	}

	override fun getPolicy(): FileEditorPolicy = FileEditorPolicy.HIDE_DEFAULT_EDITOR
}
