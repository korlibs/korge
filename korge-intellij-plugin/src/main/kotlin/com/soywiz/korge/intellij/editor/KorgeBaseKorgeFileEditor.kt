package com.soywiz.korge.intellij.editor

import com.intellij.codeHighlighting.*
import com.intellij.openapi.fileEditor.*
import com.intellij.openapi.project.*
import com.intellij.openapi.util.*
import com.intellij.openapi.vfs.*
import com.soywiz.korag.*
import com.soywiz.korge.*
import com.soywiz.korge.awt.*
import com.soywiz.korge.intellij.*
import com.soywiz.korge.intellij.ui.*
import com.soywiz.korge.intellij.util.rgba
import com.soywiz.korge.scene.*
import com.soywiz.korge.view.*
import com.soywiz.korgw.*
import com.soywiz.korgw.awt.*
import com.soywiz.korinject.*
import com.soywiz.korio.file.*
import com.soywiz.korma.geom.*
import kotlinx.coroutines.*
import java.awt.*
import java.beans.*
import javax.swing.*

data class KorgeFileToEdit(val originalFile: VirtualFile) {
    val file: VfsFile = originalFile.toTextualVfs()
}

open class KorgeBaseKorgeFileEditor(
	val project: Project,
	val virtualFile: VirtualFile,
	val module: EditorModule,
	val _name: String
) : com.intellij.diff.util.FileEditorBase(), com.intellij.openapi.project.DumbAware  {
    abstract class EditorModule() : Module() {
        open val editableNode: EditableNode? = null
    }

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
        //println("[A] ${Thread.currentThread()}")
        val fileToEdit = KorgeFileToEdit(virtualFile)
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
                //val controlRgba = MetalLookAndFeel.getCurrentTheme().control.rgba()
                val controlRgba = panel.background.rgba()
                Korge(
                    width = 640, height = 480,
                    virtualWidth = 640, virtualHeight = 480,
                    gameWindow = gameWindow!!,
                    scaleMode = ScaleMode.NO_SCALE,
                    //scaleMode = ScaleMode.SHOW_ALL,
                    scaleAnchor = Anchor.TOP_LEFT,
                    clipBorders = false,
                    bgcolor = controlRgba,
                    debug = false
                ) {
                    //println("[F] ${Thread.currentThread()}")
                    injector.jvmAutomapping()
                    val container = sceneContainer(views)
                    views.setVirtualSize(panel.width, panel.height)
                    module.apply {
                        injector.configure()
                    }
                    container.changeTo(module.mainScene, fileToEdit)
                    //println("[G] ${Thread.currentThread()}")
                }
            }
        }.also { it.isDaemon = true }.start()
        //println("[I] ${Thread.currentThread()}")
        val editableNode = module.editableNode
        if (editableNode != null) {
            createRootStyled().apply {
                createPropertyPanelWithEditor(panel, editableNode)
            }.component
        } else {
            panel
        }
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

	override fun isModified(): Boolean = false
	override fun getName(): String = _name
	override fun addPropertyChangeListener(p0: PropertyChangeListener) = Unit
	override fun removePropertyChangeListener(p0: PropertyChangeListener) = Unit
	override fun setState(p0: FileEditorState) = Unit
	override fun getPreferredFocusedComponent(): JComponent? = component
	override fun <T : Any?> getUserData(p0: Key<T>): T? = null
	override fun selectNotify() = Unit
	override fun <T : Any?> putUserData(p0: Key<T>, p1: T?) = Unit
	override fun getCurrentLocation(): FileEditorLocation? = null
	override fun deselectNotify() = Unit
	override fun getBackgroundHighlighter(): BackgroundEditorHighlighter? = null
	override fun isValid(): Boolean = true
}
