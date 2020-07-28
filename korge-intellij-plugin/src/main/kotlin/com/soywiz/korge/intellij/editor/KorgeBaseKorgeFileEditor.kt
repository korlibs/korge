package com.soywiz.korge.intellij.editor

import com.intellij.codeHighlighting.*
import com.intellij.openapi.fileEditor.*
import com.intellij.openapi.project.*
import com.intellij.openapi.util.*
import com.intellij.openapi.vfs.*
import com.soywiz.korag.*
import com.soywiz.korge.scene.*
import com.soywiz.korge.view.*
import com.soywiz.korio.file.*
import java.awt.*
import java.beans.*
import javax.swing.*

data class KorgeFileToEdit(val file: VfsFile)

open class KorgeBaseKorgeFileEditor(
	val project: Project,
	val virtualFile: VirtualFile,
	val module: Module,
	val _name: String
) : com.intellij.diff.util.FileEditorBase() {
	companion object {
		var componentsCreated = 0
	}

	var disposed = false
	var ag: AG? = null
	var views: Views? = null

	val component by lazy {
		componentsCreated++
		val panel = JPanel()
		panel.layout = GridLayout(1, 1)
		/*
		Korge(config = Korge.Config()) {

		}
		 */
		/*
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
		*/

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
		//if (ag?.glcanvas != null) component.remove(ag?.glcanvas)
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
