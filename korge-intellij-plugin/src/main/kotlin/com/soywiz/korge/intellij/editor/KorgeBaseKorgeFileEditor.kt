package com.soywiz.korge.intellij.editor

import com.intellij.codeHighlighting.*
import com.intellij.openapi.fileEditor.*
import com.intellij.openapi.project.*
import com.intellij.openapi.util.*
import com.intellij.openapi.vfs.*
import com.soywiz.korag.*
import com.soywiz.korge.*
import com.soywiz.korge.intellij.*
import com.soywiz.korge.scene.*
import com.soywiz.korge.view.*
import com.soywiz.korgw.*
import com.soywiz.korgw.awt.*
import com.soywiz.korinject.*
import com.soywiz.korio.async.*
import com.soywiz.korio.file.*
import com.soywiz.korma.geom.*
import kotlinx.coroutines.*
import java.awt.*
import java.beans.*
import java.io.*
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
    var gameWindow: GameWindow? = null
    var canvas: GLCanvas? = null

	val component by lazy {
		componentsCreated++
		val panel = JPanel()
		panel.layout = GridLayout(1, 1)
        canvas = GLCanvas()
        val canvas = canvas!!
        canvas?.minimumSize = Dimension(64, 64)
        panel.add(canvas)
        println("[A] ${Thread.currentThread()}")
        Thread {
            runBlocking {
                /*
                println("[B] ${Thread.currentThread()}")
                val korge = GLCanvasKorge(canvas)
                println("[D] ${Thread.currentThread()}")
                this@KorgeBaseKorgeFileEditor.korge = korge
                println("[E] ${Thread.currentThread()}")
                korge.launchInContext {
                    println("[F] ${Thread.currentThread()}")
                    injector.jvmAutomapping()
                    val container = sceneContainer(views)
                    container.changeTo(module.mainScene, KorgeFileToEdit(virtualFile.toVfs()))
                    println("[G] ${Thread.currentThread()}")
                }
                println("[H] ${Thread.currentThread()}")
                 */
                gameWindow = GLCanvasGameWindow(canvas)
                Korge(width = canvas.width, height = canvas.height, gameWindow = gameWindow!!, scaleMode = ScaleMode.NO_SCALE, scaleAnchor = Anchor.TOP_LEFT, clipBorders = false) {
                    println("[F] ${Thread.currentThread()}")
                    injector.jvmAutomapping()
                    val container = sceneContainer(views)
                    container.changeTo(module.mainScene, KorgeFileToEdit(virtualFile.toVfs()))
                    println("[G] ${Thread.currentThread()}")
                }
            }
        }.start()
        println("[I] ${Thread.currentThread()}")
		panel
	}

	override fun getComponent(): JComponent = component

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
        gameWindow?.close()
        canvas?.close()
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
