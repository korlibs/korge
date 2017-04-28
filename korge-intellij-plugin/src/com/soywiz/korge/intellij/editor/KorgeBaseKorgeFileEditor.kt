package com.soywiz.korge.intellij.editor

import com.intellij.codeHighlighting.BackgroundEditorHighlighter
import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.fileEditor.FileEditorLocation
import com.intellij.openapi.fileEditor.FileEditorState
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.openapi.vfs.VirtualFile
import com.soywiz.korag.awt.AGAwt
import com.soywiz.korge.Korge
import com.soywiz.korge.intellij.toVfs
import com.soywiz.korge.scene.Module
import com.soywiz.korio.async.EventLoop
import com.soywiz.korio.inject.AsyncInjector
import com.soywiz.korio.vfs.VfsFile
import java.awt.Dimension
import java.awt.GridLayout
import java.beans.PropertyChangeListener
import javax.swing.JComponent
import javax.swing.JPanel

data class KorgeFileToEdit(val file: VfsFile)

open class KorgeBaseKorgeFileEditor(val project: Project, val virtualFile: VirtualFile, val module: Module, name: String) : FileEditor {
	val ag by lazy { AGAwt() }

	val component by lazy {
		val panel = JPanel()
		panel.layout = GridLayout(1, 1)
		println("KorgeParticleFileEditor[1]")
		ag.glcanvas.minimumSize = Dimension(64, 64)
		//ag.glcanvas.size = Dimension(64, 64)
		//panel.add(JLabel("HELLO"))
		panel.add(ag.glcanvas)

		val injector = AsyncInjector()

		injector.map(KorgeFileToEdit(virtualFile.toVfs()))

		ag.onReady.then {
			EventLoop {
				println("KorgeParticleFileEditor[3]")
				Korge.test(module, canvas = ag, injector = injector, trace = true)
				println("KorgeParticleFileEditor[4]")
			}
		}

		panel
	}

	override fun isModified(): Boolean {
		return false
	}

	override fun addPropertyChangeListener(p0: PropertyChangeListener) {
	}

	override fun removePropertyChangeListener(p0: PropertyChangeListener) {
	}

	override fun getName(): String {
		return name
	}

	override fun setState(p0: FileEditorState) {
	}

	override fun getComponent(): JComponent {
		return component
	}

	override fun getPreferredFocusedComponent(): JComponent? {
		return component
	}

	override fun <T : Any?> getUserData(p0: Key<T>): T? {
		return null
	}

	override fun selectNotify() {
	}

	override fun <T : Any?> putUserData(p0: Key<T>, p1: T?) {
	}

	override fun getCurrentLocation(): FileEditorLocation? {
		return null
	}

	override fun deselectNotify() {
		//TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
	}

	override fun getBackgroundHighlighter(): BackgroundEditorHighlighter? {
		return null
		//TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
	}

	override fun isValid(): Boolean {
		return true
	}

	override fun dispose() {
		ag.dispose()
	}
}
