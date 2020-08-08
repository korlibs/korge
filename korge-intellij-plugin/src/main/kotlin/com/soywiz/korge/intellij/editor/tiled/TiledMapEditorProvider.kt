package com.soywiz.korge.intellij.editor.tiled

import com.intellij.diff.util.*
import com.intellij.openapi.command.*
import com.intellij.openapi.command.undo.*
import com.intellij.openapi.fileEditor.*
import com.intellij.openapi.project.*
import com.intellij.openapi.util.*
import com.intellij.openapi.vfs.*
import com.soywiz.korge.intellij.*
import com.soywiz.korge.intellij.editor.*
import com.soywiz.korge.intellij.editor.tiled.dialog.*
import com.soywiz.korge.intellij.util.*
import java.beans.*
import javax.swing.*

class TiledMapEditorProvider : FileEditorProvider, DumbAware {
	override fun getEditorTypeId(): String = this::class.java.name
	override fun getPolicy(): FileEditorPolicy = FileEditorPolicy.PLACE_BEFORE_DEFAULT_EDITOR

	override fun accept(
		project: Project,
		virtualFile: VirtualFile
	): Boolean {
		val name = virtualFile.name
		return when {
			name.endsWith(".tmx", ignoreCase = true) -> true
			else -> false
		}
	}

	override fun createEditor(project: Project, file: VirtualFile): FileEditor {
		val tmxFile = file.toVfs()
		val history = HistoryManager()
		val undoManager = UndoManager.getInstance(project)
		val ref = DocumentReferenceManager.getInstance().create(file)
		val refs = arrayOf(ref)

		val fileEditor: FileEditorBase = object : FileEditorBase(), DumbAware {
            val fileEditor: FileEditorBase = this
			val panel = TiledMapEditorPanel(
				tmxFile, history,
				registerHistoryShortcuts = false,
				projectCtx = ProjectContext(project, file),
				onSaveXml = { xmlText ->
				    ref.document?.let { doc ->
                        runWriteActionNoWait() {
				    	//run {
					    	println("DOCUMENT SET TEXT")
						    doc.setText(xmlText)
				    	}
				    }
			    }
            )
			override fun isModified(): Boolean = panel.history.isModified
			override fun getName(): String = "Editor"
			override fun setState(state: FileEditorState) = Unit
			override fun getComponent(): JComponent = panel
			override fun getPreferredFocusedComponent(): JComponent? = null
			override fun <T : Any?> getUserData(key: Key<T>): T? = null
			override fun <T : Any?> putUserData(key: Key<T>, value: T?) = Unit
			override fun getCurrentLocation(): FileEditorLocation? = null
			override fun isValid(): Boolean = true
			override fun addPropertyChangeListener(listener: PropertyChangeListener) = Unit
			override fun removePropertyChangeListener(listener: PropertyChangeListener) = Unit
			override fun dispose() = Unit
		}

		history.onAdd { entry ->
			CommandProcessor.getInstance().executeCommand(project, {
				undoManager.undoableActionPerformed(object : UndoableAction {
					override fun redo() {
						history.moveTo(entry.cursor)
					}
					override fun undo() {
						history.moveTo(entry.cursor - 1)
					}
					//override fun isGlobal(): Boolean = false
					override fun isGlobal(): Boolean = true
					override fun getAffectedDocuments() = refs
				})
			}, entry.name, "tilemap", UndoConfirmationPolicy.DO_NOT_REQUEST_CONFIRMATION, refs[0].document)
		}

		return fileEditor
	}
}
