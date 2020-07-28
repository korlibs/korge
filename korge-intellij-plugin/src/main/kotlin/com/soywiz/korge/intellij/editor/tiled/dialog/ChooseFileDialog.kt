package com.soywiz.korge.intellij.editor.tiled.dialog

import com.intellij.openapi.fileChooser.*
import com.intellij.openapi.project.*
import com.intellij.openapi.vfs.*

data class ProjectContext(val project: Project, val file: VirtualFile)

fun ProjectContext?.chooseFile(): VirtualFile? {
	return FileChooser.chooseFile(
		FileChooserDescriptor(true, false, false, false, false, false),
		this?.project, this?.file
	)
}
