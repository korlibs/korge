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

data class AnchorPointResult(
    val anchor: Anchor,
    val angle: Angle
) {
    companion object {
        val ANCHOR_POINT_TO_ANGLE = mapOf(
            Anchor.MIDDLE_RIGHT to ((45).degrees * 0),
            Anchor.BOTTOM_RIGHT to ((45).degrees * 1),
            Anchor.BOTTOM_CENTER to ((45).degrees * 2),
            Anchor.BOTTOM_LEFT to ((45).degrees * 3),
            Anchor.MIDDLE_LEFT to ((45).degrees * 4),
            Anchor.TOP_LEFT to ((45).degrees * 5),
            Anchor.TOP_CENTER to ((45).degrees * 6),
            Anchor.TOP_RIGHT to ((45).degrees * 7),
        )
    }
}

suspend fun ktreeEditor(file: VfsFile): KorgeBaseKorgeFileEditor.EditorModule {
    var save = false

    val viewTree = file.readXml().ktreeToViewTree()

    return createModule {
        val views = this.views
        val gameWindow = this.views.gameWindow
        val stage = views.stage

        fun getScaleAnchorPoint(view: View?): AnchorPointResult? {
            if (view == null) return null
            fun cursorDistanceToPoint(point: Point) = (stage.mouseXY - point).length
            var anchor: Anchor? = null
            var angle: Angle? = null

            for ((currentAnchor, currentAngle) in AnchorPointResult.ANCHOR_POINT_TO_ANGLE) {
                if (cursorDistanceToPoint(view.globalLocalBoundsPointRatio(currentAnchor)) < 10) {
                    anchor = currentAnchor
                    angle = (view.rotation + currentAngle).normalized
                    break
                }
            }

            return if (anchor == null || angle == null) null else AnchorPointResult(anchor, angle)
        }

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
        val selectedViewSize = Size()
        var currentAnchor: AnchorPointResult? = null
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
                currentAnchor = getScaleAnchorPoint(selectedView)
                startSelectedMousePos.setTo(views.globalMouseX, views.globalMouseY)
                if (currentAnchor == null) {
                    val view2 = stage.hitTest(it.lastPosStage)
                    if (view2 !== stage) {
                        views.debugHightlightView(view2)
                        selectedView = view2
                        if (view2 != null) {
                            startSelectedViewPos.setTo(view2.globalX, view2.globalY)
                        }
                    } else {
                        views.renderContext.debugAnnotateView = null
                        selectedView = null
                        views.debugHightlightView(selectedView)
                    }
                } else {
                    val view = selectedView
                    if (view != null) {
                        selectedViewSize.setTo(view.scaledWidth, view.scaledHeight)
                    }
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
                    if (currentAnchor != null) {
                        view.scaledWidth = (selectedViewSize.width + dx).nearestAlignedTo(20.0)
                        view.scaledHeight = (selectedViewSize.height + dy).nearestAlignedTo(20.0)
                    } else {
                        view.globalX = (startSelectedViewPos.x + dx).nearestAlignedTo(20.0)
                        view.globalY = (startSelectedViewPos.y + dy).nearestAlignedTo(20.0)
                    }
                    //startSelectedViewPos.setTo(view2.globalX, view2.globalY)
                }
            }
        }

        stage.addUpdater {
            gameWindow.cursor = GameWindow.Cursor.fromAngle(getScaleAnchorPoint(selectedView)?.angle)

            if (save) {
                save = false
                launchImmediately {
                    file.writeString(stage.viewTreeToKTree().toOuterXmlIndented().toString())
                }
            }
        }
    }
}
