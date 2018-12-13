package com.soywiz.korge.intellij.editor

import com.intellij.codeHighlighting.BackgroundEditorHighlighter
import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.fileEditor.FileEditorLocation
import com.intellij.openapi.fileEditor.FileEditorState
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.openapi.vfs.VirtualFile
import com.soywiz.korag.AGAwt
import com.soywiz.korge.Korge
import com.soywiz.korge.intellij.toVfs
import com.soywiz.korge.scene.Module
import com.soywiz.korge.ui.UIFactory
import com.soywiz.korge.ui.UISkin
import com.soywiz.korge.view.Views
import com.soywiz.korio.async.EventLoop
import com.soywiz.korinject.AsyncInjector
import com.soywiz.korinject.jvmAutomapping
import com.soywiz.korio.vfs.VfsFile
import java.awt.Dimension
import java.awt.GridLayout
import java.beans.PropertyChangeListener
import javax.swing.JComponent
import javax.swing.JPanel

data class KorgeFileToEdit(val file: VfsFile)

open class KorgeBaseKorgeFileEditor(
	val project: Project,
	val virtualFile: VirtualFile,
	val module: Module,
	val _name: String
) : FileEditor {
	companion object {
		var componentsCreated = 0
	}

	var disposed = false
	var ag: AGAwt? = null
	var views: Views? = null

	val component by lazy {
		componentsCreated++
		val panel = JPanel()
		panel.layout = GridLayout(1, 1)
		println("KorgeParticleFileEditor[1]")
		val ag = AGAwt()
		this.ag = ag
		ag.glcanvas.minimumSize = Dimension(64, 64)
		//ag.glcanvas.size = Dimension(64, 64)
		//panel.add(JLabel("HELLO"))
		panel.add(ag.glcanvas)

		val injector = AsyncInjector()
			//.mapSingleton { UISkin(get(), get()) }
			//.mapSingleton { UIFactory() }
			.mapPrototype { KorgeBaseFileEditorProvider.EditorModule.EditorScene(get(), get()) }
		//.jvmAutomapping()

		injector.mapInstance(KorgeFileToEdit(virtualFile.toVfs()))

		ag.onReady.then {
			EventLoop {
				try {
					Korge.test(
						Korge.Config(
							module,
							container = ag,
							injector = injector,
							trace = false,
							constructedViews = { views = it })
					)
				} finally {
					if (disposed) dispose()
				}
			}
		}

		panel
	}

	override fun getComponent(): JComponent {
		return component
	}

	override fun dispose() {
		componentsCreated--
		println("KorgeBaseKorgeFileEditor.componentsCreated: $componentsCreated")
		if (componentsCreated != 0) {
			println("   !!!! componentsCreated != 0")
		}
		views?.dispose()
		views = null
		if (ag?.glcanvas != null) component.remove(ag?.glcanvas)
		ag?.dispose()
		ag = null
		disposed = true
		System.gc()
	}

	override fun isModified(): Boolean {
		return false
	}

	override fun getName(): String {
		return _name
	}

	override fun addPropertyChangeListener(p0: PropertyChangeListener) {
	}

	override fun removePropertyChangeListener(p0: PropertyChangeListener) {
	}

	override fun setState(p0: FileEditorState) {
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
}
