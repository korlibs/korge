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

enum class AnchorKind { SCALING, ROTATING }

data class AnchorPointResult(
    val anchor: Anchor,
    val angle: Angle,
    val kind: AnchorKind
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

    val viewTree = file.readKTree()

    return createModule {
        val views = this.views
        val gameWindow = this.views.gameWindow
        val stage = views.stage

        views.currentVfs = file.parent

        fun getScaleAnchorPoint(view: View?, distance: Int, kind: AnchorKind): AnchorPointResult? {
            if (view == null) return null
            fun cursorDistanceToPoint(point: Point) = (stage.mouseXY - point).length
            var anchor: Anchor? = null
            var angle: Angle? = null

            for ((currentAnchor, currentAngle) in AnchorPointResult.ANCHOR_POINT_TO_ANGLE) {
                if (cursorDistanceToPoint(view.globalLocalBoundsPointRatio(currentAnchor)) < distance) {
                    anchor = currentAnchor
                    angle = (view.rotation + currentAngle).normalized
                    break
                }
            }

            return if (anchor == null || angle == null) null else AnchorPointResult(anchor, angle, kind)
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
        var selectedViewInitialRotation = 0.degrees
        var currentAnchor: AnchorPointResult? = null
        var gridSnapping = true
        var gridWidth = 20.0
        var gridHeight = 20.0

        fun selectView(view: View?) {
            views.renderContext.debugAnnotateView = view
            views.debugHightlightView(view)
            selectedView = view
        }

        stage.mouse {
            click {
                val view = selectedView
                if (it.button == MouseButton.RIGHT) {
                    val hasView = view != null
                    gameWindow.showContextMenu(listOf(
                        GameWindow.MenuItem("Cut", enabled = hasView) {
                            launchImmediately {
                                viewsDebuggerComponent.pasteboard = view!!.viewTreeToKTree(views)
                                selectView(view?.parent)
                                view!!.removeFromParent()
                            }
                        },
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
                            view?.parent?.sendChildToBack(view)
                        },
                        GameWindow.MenuItem("Bring to front", enabled = hasView) {
                            view?.parent?.sendChildToFront(view)
                        },
                    ))
                }
            }
            down {
                pressing = true
                currentAnchor = getScaleAnchorPoint(selectedView, 10, AnchorKind.SCALING)
                        ?: getScaleAnchorPoint(selectedView, 20, AnchorKind.ROTATING)

                startSelectedMousePos.setTo(views.globalMouseX, views.globalMouseY)
                if (currentAnchor == null) {
                    val view2 = stage.hitTest(it.lastPosStage)
                    if (view2 !== stage) {
                        selectView(view2)
                    } else {
                        selectView(null)
                    }
                } else {
                    val view = selectedView
                    if (view != null) {
                        selectedViewSize.setTo(view.scaledWidth, view.scaledHeight)
                        selectedViewInitialRotation = view.rotation
                    }
                }
                selectedView?.let { view ->
                    startSelectedViewPos.setTo(view.globalX, view.globalY)
                }

                //println("DOWN ON ${it.view}, $view2, ${it.lastPosStage}")
            }
            up {
                pressing = false
            }
            onMoveAnywhere { e ->
                val view = selectedView
                if (pressing && view != null) {
                    val dx = views.globalMouseX - startSelectedMousePos.x
                    val dy = views.globalMouseY - startSelectedMousePos.y
                    val anchor = currentAnchor
                    if (anchor != null) {
                        when (anchor.kind) {
                            AnchorKind.SCALING -> {
                                view.scaledWidth = (selectedViewSize.width + dx)
                                view.scaledHeight = (selectedViewSize.height + dy)
                                if (gridSnapping) {
                                    view.scaledWidth = view.scaledWidth.nearestAlignedTo(gridWidth)
                                    view.scaledHeight = view.scaledHeight.nearestAlignedTo(gridHeight)
                                }
                            }
                            AnchorKind.ROTATING -> {
                                val initialAngle = Angle.between(startSelectedViewPos, startSelectedMousePos)
                                val currentAngle = Angle.between(startSelectedViewPos, views.globalMouseXY)
                                val deltaAngle = currentAngle - initialAngle
                                view.rotation = (selectedViewInitialRotation + deltaAngle)
                                if (e.isShiftDown) {
                                    view.rotationDegrees = view.rotationDegrees.nearestAlignedTo(15.0)
                                }
                            }
                        }
                    } else {
                        view.globalX = (startSelectedViewPos.x + dx)
                        view.globalY = (startSelectedViewPos.y + dy)
                        if (gridSnapping) {
                            view.globalX = view.globalX.nearestAlignedTo(gridWidth)
                            view.globalY = view.globalY.nearestAlignedTo(gridHeight)
                        }
                    }
                    save = true
                    //startSelectedViewPos.setTo(view2.globalX, view2.globalY)
                }
            }
        }

        stage.addUpdater {
            gameWindow.cursor =
                GameWindow.Cursor.fromAngle(getScaleAnchorPoint(selectedView, 10, AnchorKind.SCALING)?.angle)
                    ?: GameWindow.Cursor.fromAngle(getScaleAnchorPoint(selectedView, 20, AnchorKind.ROTATING)?.angle)?.let { GameWindow.Cursor.CROSSHAIR }
                    ?: GameWindow.Cursor.DEFAULT

            if (save) {
                save = false
                launchImmediately {
                    file.writeString(stage.viewTreeToKTree().toOuterXmlIndented().toString())
                }
            }
        }
    }
}
