package com.soywiz.korge.intellij

import com.intellij.codeHighlighting.BackgroundEditorHighlighter
import com.intellij.openapi.fileEditor.*
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.openapi.vfs.VirtualFile
import com.soywiz.korag.AG
import com.soywiz.korag.AGContainer
import com.soywiz.korag.awt.AGAwt
import com.soywiz.korge.Korge
import com.soywiz.korge.scene.Module
import com.soywiz.korge.scene.Scene
import com.soywiz.korge.view.Container
import com.soywiz.korge.view.text
import com.soywiz.korio.async.EventLoop
import com.soywiz.korio.async.Signal
import com.soywiz.korio.async.executeInNewThread
import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.LayoutManager
import java.beans.PropertyChangeListener
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JPanel
import java.awt.GridLayout



class KorgeParticleFileEditorProvider : FileEditorProvider {
	override fun accept(project: Project, virtualFile: VirtualFile): Boolean {
		return (virtualFile.extension?.toLowerCase() ?: "") == "pex"
	}

	override fun createEditor(project: Project, virtualFile: VirtualFile): FileEditor {
		return KorgeParticleFileEditor(project, virtualFile)
	}

	override fun getEditorTypeId(): String {
		return "KorgeFileEditor"
	}

	override fun getPolicy(): FileEditorPolicy {
		return FileEditorPolicy.PLACE_BEFORE_DEFAULT_EDITOR
	}
}

object KorgeParticleEditorKorgeModule : Module() {
	override val mainScene: Class<out Scene> = KorgeParticleEditorKorgeScene::class.java
}

class KorgeParticleEditorKorgeScene : Scene() {
	suspend override fun sceneInit(sceneView: Container) {
		super.sceneInit(sceneView)

		sceneView += views.text("HELLO WORLD!")
	}
}

class KorgeParticleFileEditor(val project: Project, val virtualFile: VirtualFile) : FileEditor {
	val component by lazy {
		val panel = JPanel()
		panel.layout = GridLayout(1, 1)
		val ag = AGAwt()
		println("KorgeParticleFileEditor[1]")
		ag.glcanvas.minimumSize = Dimension(64, 64)
		//ag.glcanvas.size = Dimension(64, 64)
		//panel.add(JLabel("HELLO"))
		panel.add(ag.glcanvas)

		ag.onReady.then {
			EventLoop {
				println("KorgeParticleFileEditor[3]")
				Korge.test(KorgeParticleEditorKorgeModule, canvas = ag, trace = true)
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
		return "KorgeParticleEditor"
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
	}

}
