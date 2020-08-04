package com.soywiz.korge.intellij.editor.formats

import com.soywiz.kds.iterators.*
import com.soywiz.kmem.*
import com.soywiz.korev.*
import com.soywiz.korge.input.*
import com.soywiz.korge.intellij.editor.*
import com.soywiz.korge.view.*
import com.soywiz.korge.view.ktree.*
import com.soywiz.korgw.*
import com.soywiz.korio.async.*
import com.soywiz.korio.file.*
import com.soywiz.korio.serialization.xml.*
import com.soywiz.korma.geom.*

suspend fun ktreeEditor(file: VfsFile): KorgeBaseKorgeFileEditor.EditorModule {
    var save = false

    val viewTree = file.readXml().ktreeToViewTree()

    return createModule {
        val views = this.views
        val gameWindow = this.views.gameWindow
        val stage = views.stage
        // Dirty hack
        views.stage.removeChildren()

        if (viewTree is Container) {
            viewTree.children.toList().fastForEach {
                //println("ADDING: $it")
                stage.addChild(it)
            }
        }

        views.debugSavedHandlers.add {
            save = true
        }
        views.debugHightlightView(stage)

        var pressing = false
        var selectedView: View? = null
        val startSelectedViewPos = Point()
        val startSelectedMousePos = Point()
        stage.mouse {
            click {
                val view = selectedView
                if (it.button == MouseButton.RIGHT) {
                    val hasView = view != null
                    gameWindow.showContextMenu(listOf(
                        GameWindow.MenuItem("Copy", enabled = hasView) {
                            launchImmediately {
                                viewsDebuggerComponent.pasteboard = view!!.viewTreeToKTree(views)
                            }
                        },
                        GameWindow.MenuItem("Paste in place") {
                            val pasteboard = viewsDebuggerComponent.pasteboard
                            launchImmediately {
                                val container = (view as? Container?) ?: view?.parent ?: stage
                                if (pasteboard != null) {
                                    container.addChild(pasteboard.ktreeToViewTree(views))
                                }
                            }
                        },
                        null,
                        GameWindow.MenuItem("Send to back", enabled = hasView) {
                            val parent = view!!.parent
                            if (parent != null) {
                                parent.swapChildren(view, parent.firstChild!!)
                            }
                        },
                        GameWindow.MenuItem("Bring to front", enabled = hasView) {
                            val parent = view!!.parent
                            if (parent != null) {
                                parent.swapChildren(view, parent.lastChild!!)
                            }
                        },
                    ))
                }
            }
            down {
                pressing = true
                val view2 = stage.hitTest(it.lastPosStage)
                if (view2 !== stage) {
                    views.debugHightlightView(view2)
                    selectedView = view2
                    if (view2 != null) {
                        startSelectedViewPos.setTo(view2.globalX, view2.globalY)
                        startSelectedMousePos.setTo(views.globalMouseX, views.globalMouseY)
                    }
                } else {
                    views.renderContext.debugAnnotateView = null
                    selectedView = null
                    views.debugHightlightView(selectedView)
                }
                //println("DOWN ON ${it.view}, $view2, ${it.lastPosStage}")
            }
            up {
                pressing = false
            }
            onMoveAnywhere {
                val view = selectedView
                if (pressing && view != null) {
                    val dx = views.globalMouseX - startSelectedMousePos.x
                    val dy = views.globalMouseY - startSelectedMousePos.y
                    view.globalX = (startSelectedViewPos.x + dx).nearestAlignedTo(20.0)
                    view.globalY = (startSelectedViewPos.y + dy).nearestAlignedTo(20.0)
                    //startSelectedViewPos.setTo(view2.globalX, view2.globalY)
                }
            }
        }

        stage.addUpdater {
            val view = selectedView
            var cursor = GameWindow.Cursor.DEFAULT
            fun distanceToPoint(point: Point) = (stage.mouseXY - point).length
            if (view != null) {
                var angle: Angle? = null
                angle = when {
                    distanceToPoint(view.globalLocalBoundsPointRatio(1.0, 0.5)) < 10 -> (45).degrees * 0
                    distanceToPoint(view.globalLocalBoundsPointRatio(1.0, 1.0)) < 10 -> (45).degrees * 1
                    distanceToPoint(view.globalLocalBoundsPointRatio(0.5, 1.0)) < 10 -> (45).degrees * 2
                    distanceToPoint(view.globalLocalBoundsPointRatio(0.0, 1.0)) < 10 -> (45).degrees * 3
                    distanceToPoint(view.globalLocalBoundsPointRatio(0.0, 0.5)) < 10 -> (45).degrees * 4
                    distanceToPoint(view.globalLocalBoundsPointRatio(0.0, 0.0)) < 10 -> (45).degrees * 5
                    distanceToPoint(view.globalLocalBoundsPointRatio(0.5, 0.0)) < 10 -> (45).degrees * 6
                    distanceToPoint(view.globalLocalBoundsPointRatio(1.0, 0.0)) < 10 -> (45).degrees * 7
                    else -> null
                }
                if (angle != null) {
                    val realAngle = (view.rotation + angle).normalized
                    cursor = GameWindow.Cursor.fromAngle(realAngle)
                }
            }

            gameWindow.cursor = cursor

            if (save) {
                save = false
                launchImmediately {
                    file.writeString(stage.viewTreeToKTree().toOuterXmlIndented().toString())
                }
            }
        }
    }
}
