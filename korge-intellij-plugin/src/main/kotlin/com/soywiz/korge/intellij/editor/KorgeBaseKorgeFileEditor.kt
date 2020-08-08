package com.soywiz.korge.intellij.editor

import com.esotericsoftware.spine.korge.*
import com.intellij.codeHighlighting.*
import com.intellij.openapi.command.*
import com.intellij.openapi.command.undo.*
import com.intellij.openapi.editor.*
import com.intellij.openapi.editor.event.DocumentAdapter
import com.intellij.openapi.editor.event.DocumentEvent
import com.intellij.openapi.editor.event.DocumentListener
import com.intellij.openapi.fileEditor.*
import com.intellij.openapi.project.*
import com.intellij.openapi.util.*
import com.intellij.openapi.vfs.*
import com.intellij.ui.*
import com.soywiz.klock.hr.*
import com.soywiz.korag.*
import com.soywiz.korge.*
import com.soywiz.korge.awt.*
import com.soywiz.korge.debug.*
import com.soywiz.korge.dragonbones.*
import com.soywiz.korge.ext.swf.*
import com.soywiz.korge.intellij.*
import com.soywiz.korge.intellij.components.*
import com.soywiz.korge.intellij.korui.*
import com.soywiz.korge.intellij.util.*
import com.soywiz.korge.scene.*
import com.soywiz.korge.time.*
import com.soywiz.korge.view.*
import com.soywiz.korgw.*
import com.soywiz.korgw.awt.*
import com.soywiz.korinject.*
import com.soywiz.korio.file.*
import com.soywiz.korma.geom.*
import kotlinx.coroutines.*
import java.awt.*
import java.awt.Container
import java.awt.event.*
import java.beans.*
import javax.swing.*
import javax.swing.event.*
import kotlin.coroutines.*

data class KorgeFileToEdit(val originalFile: VirtualFile, val project: Project) : BaseKorgeFileToEdit(originalFile.toTextualVfs()) {
    val ref = DocumentReferenceManager.getInstance().create(originalFile)
    val doc = ref.document ?: error("Can't get document")
    var lastSavedText = ""

    val documentListener = object : DocumentListener {
        override fun documentChanged(event: DocumentEvent) {
            val newText = event.document.text
            if (newText != lastSavedText) {
                lastSavedText = newText
                //println("documentChanged")
                onChanged(newText)
            } else {
                //println("documentUnchanged")
            }
        }
    }

    init {
        doc.addDocumentListener(documentListener)
    }

    var n = 0

    fun dispose() {
        doc.removeDocumentListener(documentListener)
    }

    override fun save(text: String, message: String) {
        val oldText = doc.text
        if (oldText != text) {
            lastSavedText = text
            clearWriteActionNoWait()
            queueWriteActionNoWait {
                val id = n++
                CommandProcessor.getInstance().executeCommand(project, Runnable {
                    /*
                    val action = object : UndoableAction {
                        override fun undo() {
                            //doc.setText(oldText)
                            //load(oldText)
                        }
                        override fun redo() {
                            //doc.setText(text)
                            //load(text)
                        }
                        override fun getAffectedDocuments() = arrayOf(ref)
                        override fun isGlobal(): Boolean = false
                    }
                    UndoManager.getInstance(project).undoableActionPerformed(action)
                    */

                    doc.setText(text)
                }, message, "korge$id", doc)
            }
        }
    }
}

open class KorgeBaseKorgeFileEditor(
	val project: Project,
	val fileToEdit: KorgeFileToEdit,
	val module: Module,
	val _name: String
) : com.intellij.diff.util.FileEditorBase(), com.intellij.openapi.project.DumbAware  {

	companion object {
		var componentsCreated = 0
	}

	var disposed = false
	var ag: AG? = null
	var views: Views? = null
    var gameWindow: GameWindow? = null
    var canvas: GLCanvas? = null
    val viewsDebuggerComponentHolder = JPanel(LinearLayout(Direction.VERTICAL))
    var viewsDebuggerComponent: ViewsDebuggerComponent? = null

	val component by lazy {
		componentsCreated++
		val panel = JPanel()
		panel.layout = GridLayout(1, 1)
        canvas = GLCanvas()
        val canvas = canvas!!
        canvas.minimumSize = Dimension(64, 64)
        panel.add(canvas)
        //println("[A] ${Thread.currentThread()}")
        Thread {
            runBlocking {
                gameWindow = GLCanvasGameWindowIJ(canvas)
                val listener = object : MouseAdapter() {
                    override fun mouseReleased(e: MouseEvent) {
                        executePendingWriteActions()
                    }

                    override fun mouseMoved(e: MouseEvent?) {
                        executePendingWriteActions()
                    }
                }
                canvas.addMouseListener(listener)
                canvas.addMouseMotionListener(listener)
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
                    views.completedEditing {
                        println("!! completedEditing")
                        executePendingWriteActions()

                    }
                    views.registerSwf()
                    views.registerDragonBones()
                    views.registerSpine()
                    views.ideaProject = project
                    val app = IdeaUiApplication(project, views)
                    viewsDebuggerComponent = ViewsDebuggerComponent(views, app).also {
                        it.styled.fill()
                    }
                    views.ideaComponent = viewsDebuggerComponent
                    invokeLater {
                        viewsDebuggerComponentHolder.add(viewsDebuggerComponent)
                    }

                    //println("[F] ${Thread.currentThread()}")
                    injector.jvmAutomapping()
                    injector.mapInstance<ViewsDebuggerComponent>(viewsDebuggerComponent!!)
                    val container = sceneContainer(views)
                    views.setVirtualSize(panel.width, panel.height)
                    module.apply {
                        injector.configure()
                    }
                    container.changeTo(module.mainScene, fileToEdit)
                    //viewsDebuggerComponent?.setRootView(stage)
                    stage.timers.interval(500.hrMilliseconds) {
                        viewsDebuggerComponent?.update()
                    }
                    //println("[G] ${Thread.currentThread()}")
                }
            }
        }.also { it.isDaemon = true }.start()
        //println("[I] ${Thread.currentThread()}")
        initializeIdeaComponentFactory()
        createRootStyled().apply {
            createViewsWithDebugger(panel, null, viewsDebuggerComponentHolder)
        }.component.also { component ->
            val listener = object : MouseAdapter() {
                override fun mouseReleased(e: MouseEvent) {
                    println("mouseReleased")
                    executePendingWriteActions()
                }

                override fun mouseMoved(e: MouseEvent) {
                    println("mouseMoved")
                    executePendingWriteActions()
                }
            }
            component.addMouseMotionListener(listener)
            component.addMouseListener(listener)
        }
	}

    fun Styled<out Container>.createViewsWithDebugger(
        editor: Component,
        rootNode: Any?,
        viewsDebuggerComponentHolder: JPanel
    ) {
        verticalStack {
            fill()
            horizontalStack {
                fill()
                verticalStack {
                    //minWidth = 32.pt
                    minWidth = 360.pt
                    fill()
                    add(editor.styled {
                        fill()
                    })
                }
                verticalStack {
                    minWidth = 360.pt
                    width = minWidth
                    fillHeight()
                    add(viewsDebuggerComponentHolder.styled {
                        fill()
                    })
                }
            }
        }
    }


    override fun getComponent(): JComponent = component

	override fun dispose() {
		componentsCreated--
        println("!!!!!!!!!! KorgeBaseKorgeFileEditor.DISPOSE")
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
        fileToEdit?.dispose()
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
