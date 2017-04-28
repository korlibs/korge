package com.soywiz.korge.intellij.editor

import com.intellij.openapi.fileEditor.FileEditorPolicy
import com.soywiz.korge.animate.serialization.readAni
import com.soywiz.korge.ext.swf.readSWF
import com.soywiz.korge.scene.Module
import com.soywiz.korge.scene.Scene
import com.soywiz.korge.view.Container


class KorgeAnimationFileEditorProvider : com.intellij.openapi.fileEditor.FileEditorProvider {
	override fun accept(project: com.intellij.openapi.project.Project, virtualFile: com.intellij.openapi.vfs.VirtualFile): Boolean {
		return (virtualFile.extension?.toLowerCase() ?: "") in setOf("swf", "ani")
	}

	override fun createEditor(project: com.intellij.openapi.project.Project, virtualFile: com.intellij.openapi.vfs.VirtualFile): com.intellij.openapi.fileEditor.FileEditor {
		return KorgeBaseKorgeFileEditor(project, virtualFile, EditorModule, "Animation")
	}

	override fun getEditorTypeId(): String = this::class.java.name

	override fun getPolicy(): FileEditorPolicy {
		return FileEditorPolicy.HIDE_DEFAULT_EDITOR
	}

	object EditorModule : Module() {
		override val mainScene: Class<out Scene> = EditorScene::class.java

		class EditorScene(
			val fileToEdit: KorgeFileToEdit
		) : Scene() {
			suspend override fun sceneInit(sceneView: Container) {
				super.sceneInit(sceneView)

				val animationLibrary = when (fileToEdit.file.extensionLC) {
					"swf" -> fileToEdit.file.readSWF(views)
					"ani" -> fileToEdit.file.readAni(views)
					else -> null
				}

				if (animationLibrary != null) {
					sceneView += animationLibrary.createMainTimeLine()
				}
			}
		}
	}
}
