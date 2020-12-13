package com.soywiz.korge.intellij.editor

import com.esotericsoftware.spine.korge.*
import com.intellij.codeHighlighting.*
import com.intellij.ide.*
import com.intellij.ide.actions.*
import com.intellij.ide.actions.CopyAction
import com.intellij.ide.actions.CutAction
import com.intellij.ide.actions.PasteAction
import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.command.*
import com.intellij.openapi.command.undo.*
import com.intellij.openapi.editor.actions.*
import com.intellij.openapi.editor.event.DocumentEvent
import com.intellij.openapi.editor.event.DocumentListener
import com.intellij.openapi.fileEditor.*
import com.intellij.openapi.ide.*
import com.intellij.openapi.project.*
import com.intellij.openapi.util.*
import com.intellij.openapi.vfs.*
import com.soywiz.klock.hr.*
import com.soywiz.klock.milliseconds
import com.soywiz.korag.*
import com.soywiz.korge.*
import com.soywiz.korge.awt.*
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
import com.soywiz.korio.async.*
import com.soywiz.korio.serialization.xml.*
import com.soywiz.korma.geom.*
import com.soywiz.korma.geom.Anchor
import kotlinx.coroutines.*
import java.awt.*
import java.awt.Container
import java.awt.datatransfer.*
import java.awt.event.*
import java.beans.*
import javax.swing.*
import com.soywiz.korev.dispatch
import com.soywiz.korge.input.*

open class KorgeBaseKorgeFileEditor(
    val project: Project,
    val fileToEdit: KorgeFileToEdit,
    val module: Module,
    val _name: String
//) : com.intellij.diff.util.FileEditorBase(), com.intellij.openapi.project.DumbAware, DataProvider by FakeCopyAndPasteProvider, CopyProvider by FakeCopyAndPasteProvider, PasteProvider by FakeCopyAndPasteProvider {
) : com.intellij.diff.util.FileEditorBase() {

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
    val viewsDebuggerActions: ViewsDebuggerActions? get() = viewsDebuggerComponent?.actions

    open class MyViewsDebuggerActions(views: Views) : ViewsDebuggerActions(views) {
        fun Component.createFakeEvent() = KeyEvent(this, 0, 0L, 0, 0, '\u0000', 0)

        private fun executeAction(action: AnAction) {
            ActionManager.getInstance().tryToExecute(action, component.createFakeEvent(), component, null, true)
        }

        override fun requestCopy() = executeAction(CopyAction())
        override fun requestCut() = executeAction(CutAction())
        override fun requestPaste() = executeAction(PasteAction())
        override fun requestDuplicate() {
            requestCopy()
            requestPaste()
        }
    }

	val component by lazy {
		componentsCreated++
		val panel = JPanel()
		panel.layout = GridLayout(1, 1)
        canvas = GLCanvas(checkGl = true)
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
                    views.registerIdeaStuff(project)
                    val app = IdeaUiApplication(project, views)
                    viewsDebuggerComponent = ViewsDebuggerComponent(views, app, actions = MyViewsDebuggerActions(views)).also {
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
                    views.stage.keys.down(com.soywiz.korev.Key.F8) {
                        canvas.logGl = !canvas.logGl
                    }
                    module.apply {
                        injector.configure()
                    }
                    container.changeTo(module.mainScene, fileToEdit)
                    //viewsDebuggerComponent?.setRootView(stage)
                    stage.timers.interval(500.milliseconds) {
                        viewsDebuggerComponent?.update()
                    }
                    //println("[G] ${Thread.currentThread()}")
                }
            }
        }.also { it.isDaemon = true }.start()
        //println("[I] ${Thread.currentThread()}")
        initializeIdeaComponentFactory()
        //createRootStyled()
        RootComponent(this@KorgeBaseKorgeFileEditor).styled.apply {
            createViewsWithDebugger(panel, null, viewsDebuggerComponentHolder)
        }.component.also { component ->
            val listener = object : MouseAdapter() {
                override fun mouseReleased(e: MouseEvent) {
                    //println("mouseReleased")
                    executePendingWriteActions()
                }

                override fun mouseMoved(e: MouseEvent) {
                    //println("mouseMoved")
                    executePendingWriteActions()
                }
            }
            component.addMouseMotionListener(listener)
            component.addMouseListener(listener)
        }
	}

    fun onRepaint() {
        viewsDebuggerComponent?.views?.clearColor = component.background.rgba()
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
                    toolbar {
                        toolbarButton("Grid", "Shows/Hides the grid") {
                            fun updateState() {
                                component.isSelected = viewsDebuggerActions?.gridShowing != false
                            }
                            updateState()
                            click {
                                viewsDebuggerActions?.toggleGrid()
                                updateState()
                            }
                        }
                        toolbarButton("Snapping", "Enables/Disables grid snapping") {
                            fun updateState() {
                                component.isSelected = viewsDebuggerActions?.gridSnapping != false
                            }
                            updateState()
                            click {
                                viewsDebuggerActions?.toggleGridSnapping()
                                updateState()
                            }
                        }
                        val playPauseButton = toolbarButton("Play/Pause", "Plays/Pauses the animations") {
                            fun updateState() {
                                component.isSelected = viewsDebuggerActions?.playing == true
                            }
                            updateState()
                            click {
                                //viewsDebuggerActions?.views?.dispatch(com.soywiz.korge.awt.KTreeSaveEvent())
                                viewsDebuggerActions?.views?.save2Handlers?.invoke(Unit)
                                viewsDebuggerActions?.togglePlay()
                                updateState()
                            }
                        }
                        toolbarButton("Reset", "Resets the animation to its initial version before playing") {
                            fun updateState() {
                                playPauseButton.component.isSelected = viewsDebuggerActions?.playing == true
                            }
                            click {
                                viewsDebuggerActions?.toggleReset()
                                //viewsDebuggerActions?.views?.dispatch(com.soywiz.korge.awt.KTreeRestoreEvent())
                                viewsDebuggerActions?.views?.restore2Handlers?.invoke(Unit)
                                updateState()
                            }
                        }
                        toolbarButton("Settings") {
                            click {
                                val actions = viewsDebuggerActions
                                if (actions != null) {
                                    val settings = showKorgeBaseEditorSettings(KorgeBaseEditorSettings(
                                        width = actions.root.width.toInt(),
                                        height = actions.root.height.toInt(),
                                        gridWidth = actions.grid.width,
                                        gridHeight = actions.grid.height,
                                    ))
                                    if (settings != null) {
                                        actions.root.width = settings.width.toDouble()
                                        actions.root.height = settings.height.toDouble()
                                        actions.grid.width = settings.gridWidth
                                        actions.grid.height = settings.gridHeight
                                    }
                                }
                            }
                        }
                    }
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


    override fun getComponent(): JComponent {
        //println("component is DataProvider: ${component is DataProvider}")
        return component
    }

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
