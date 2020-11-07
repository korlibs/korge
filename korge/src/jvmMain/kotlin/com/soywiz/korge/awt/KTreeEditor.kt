package com.soywiz.korge.awt

import com.soywiz.kds.*
import com.soywiz.kmem.*
import com.soywiz.korev.*
import com.soywiz.korge.input.*
import com.soywiz.korge.render.*
import com.soywiz.korge.scene.*
import com.soywiz.korge.view.*
import com.soywiz.korge.view.ktree.*
import com.soywiz.korgw.*
import com.soywiz.korim.color.*
import com.soywiz.korio.async.*
import com.soywiz.korio.file.*
import com.soywiz.korio.serialization.xml.*
import com.soywiz.korma.geom.*
import com.soywiz.korma.geom.vector.*
import kotlin.math.*
import com.soywiz.korge.debug.*

enum class AnchorKind { SCALING, ROTATING }

class AnchorPointResult(
    val view: View,
    val anchor: Anchor,
    val angle: Angle,
    val kind: AnchorKind
) {
    val viewInitialMatrix = view.localMatrix.copy()
    val viewInitialMatrixInv = view.localMatrix.inverted()
    val viewInitialGlobalMatrix = view.globalMatrix.copy()
    val viewInitialGlobalMatrixInv = view.globalMatrixInv.copy()
    val localPos = Point(view.x, view.y)
    val localBounds = view.getLocalBounds().copy()
    val localAnchorXY = localBounds.getAnchoredPosition(anchor)
    val globalAnchorXY = view.localToGlobal(localAnchorXY)

    fun globalToLocal(p: Point) = viewInitialGlobalMatrixInv.transform(p)
    fun localToGlobal(p: Point) = viewInitialGlobalMatrix.transform(p)

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

//class KTreeSaveEvent : Event()
//class KTreeRestoreEvent : Event()

val Views.save2Handlers by extraProperty { Signal<Unit>() }
val Views.restore2Handlers by extraProperty { Signal<Unit>() }

abstract class BaseKorgeFileToEdit(val file: VfsFile) {
    val onRequestSave = Signal<String>()
    val onChanged = Signal<String>()
    abstract fun save(text: String, message: String)
}

suspend fun ktreeEditor(fileToEdit: BaseKorgeFileToEdit): Module {
    val file = fileToEdit.file
    return myComponentFactory.createModule {
        val viewsDebuggerComponent = injector.get<ViewsDebuggerComponent>()
        val actions = viewsDebuggerComponent.actions
        actions.playing = false

        views.name = "ktree"
        views.editingMode = true
        //var save = false
        val views = this.views
        val gameWindow = this.views.gameWindow
        val stage = views.stage
        // Dirty hack
        //views.stage.removeChildren()

        val camera = actions.camera
        val root = actions.root
        //root.width = camera.width
        //root.height = camera.height

        root.extraBuildDebugComponent = { views, view, container ->
            container.uiCollapsableSection("Document") {
                uiEditableValue(listOf(root::width, root::height), min = 0.0, max = 4096.0, clamp = true, name = "Document Size")
                uiEditableValue(actions.grid::size, min = 1, max = 500, clamp = true, name = "Grid Size")
                uiEditableValue(listOf(actions.grid::width, actions.grid::height), min = 1, max = 500, clamp = true, name = "Grid Size")
            }
        }

        val currentVfs = file.parent
        views.currentVfs = currentVfs

        //val grid get() = actions.grid
        //val gridSnapping get() = actions.gridSnapping
        //val gridShowing get() = actions.gridShowing

        val grid by com.soywiz.korio.util.redirect(actions::grid)
        val gridSnapping by com.soywiz.korio.util.redirect(actions::gridSnapping)
        val gridShowing by com.soywiz.korio.util.redirect(actions::gridShowing)

        viewsDebuggerComponent.setRootView(root)

        fun getScaleAnchorPoint(view: View?, distance: Int, kind: AnchorKind): AnchorPointResult? {
            if (view == null) return null
            fun cursorDistanceToPoint(point: Point) = (stage.mouseXY - point).length
            var anchor: Anchor? = null
            var angle: Angle? = null

            for ((currentAnchor, currentAngle) in AnchorPointResult.ANCHOR_POINT_TO_ANGLE) {
                if (cursorDistanceToPoint(view.globalLocalBoundsPointRatio(currentAnchor)) < distance) {
                    val inside = view.getLocalBoundsOptimized().contains(view.globalToLocal(stage.mouseXY))
                    if (kind == AnchorKind.ROTATING && inside) continue

                    anchor = currentAnchor
                    angle = (view.rotation + currentAngle).normalized
                    break
                }
            }

            return if (anchor == null || angle == null) null else AnchorPointResult(view, anchor, angle, kind)
        }

        fun load(tree: Container) {
            root.removeChildren()
            root.addChildren(tree.children.toList())
            if (tree is KTreeRoot) {
                root.width = tree.width
                root.height = tree.height
                root.grid.width = tree.grid.width
                root.grid.height = tree.grid.height
            }
        }

        fun load(text: String) {
            launchImmediately(views.coroutineContext) {
                //Xml(text).ktreeToViewTree(views, file.parent, parent = root)
                load(Xml(text).ktreeToViewTree(views, file.parent) as Container)
            }
        }

        suspend fun load(file: VfsFile) {
            load(file.readKTree(views) as Container)
        }

        fun serialize(): String {
            return root.viewTreeToKTree(views).toOuterXmlIndentedString()
        }

        fun save(message: String) {
            fileToEdit.save(serialize(), message)
        }

        fileToEdit.onChanged {
            load(it)
        }

        views.debugSavedHandlers.add {
            save(it.toString())
        }

        var saved: String? = null

        //println("KORGE.KTreeEditor")

        views.save2Handlers {
            //println("KTreeSaveEvent!")
            saved = serialize()
        }
        views.restore2Handlers {
            //println("KTreeRestoreEvent! : $saved")
            if (saved != null) {
                load(saved!!)
            }
        }
        //views.addEventListener<KTreeSaveEvent> {
        //    println("KTreeSaveEvent!")
        //    saved = serialize()
        //}
        //views.addEventListener<KTreeRestoreEvent> {
        //}

        load(fileToEdit.file)

        actions.selectView(root)

        var pressing = false
        val startSelectedViewPos = Point()
        val startSelectedMousePos = Point()
        var startCameraPos = Point()
        val selectedViewSize = Size()
        var selectedViewInitialRotation = 0.degrees
        var currentAnchor: AnchorPointResult? = null
        var movingCameraMouse = false

        stage.keys {
            down { e ->
                when (e.key) {
                    Key.UP -> actions.moveView(0, -1, e.shift)
                    Key.DOWN -> actions.moveView(0, +1, e.shift)
                    Key.LEFT -> actions.moveView(-1, 0, e.shift)
                    Key.RIGHT -> actions.moveView(+1, 0, e.shift)
                }
            }
            up { e ->
            }
            /*
            upNew { e ->
                val view = actions.selectedView
                if (view != null) {
                    when (e.key) {
                        Key.DELETE -> actions.removeCurrentNode()
                        Key.BACKSPACE -> actions.removeCurrentNode()
                        Key.D -> if (e.ctrlOrMeta) launchImmediately { actions.duplicate() }
                        Key.X -> if (e.ctrlOrMeta) launchImmediately { actions.cut() }
                        Key.C -> if (e.ctrlOrMeta) launchImmediately { actions.copy() }
                        Key.V -> if (e.ctrlOrMeta) launchImmediately { actions.paste() }
                    }
                }
            }
            */
        }

        var action = ""

        fun spaceBarIsBeingPressed(): Boolean = views.input.keys[Key.SPACE]

        views.onBeforeRender {
            val ctx = it.debugLineRenderContext


        }

        views.onAfterRender {
            val ctx = it.debugLineRenderContext
            val mat = camera.content.globalMatrix
            ctx.draw(mat) {
                val transform = camera.content.globalMatrix.toTransform()
                val dx = transform.scaleX * grid.width
                val dy = transform.scaleY * grid.height
                //println("dxy: $dx, $dy")
                val smallX = dx < 3
                val smallY = dy < 3

                if (!smallX && !smallY) {
                    ctx.draw(camera.content.globalMatrix) {
                        if (gridShowing) {
                            grid.draw(ctx, RectangleInt(0.0, 0.0, root.width, root.height))
                        }
                    }
                }
            }
            val rectBounds = Rectangle.fromBounds(mat.transform(0.0, 0.0), mat.transform(root.width, root.height))
            ctx.drawVector(Colors.BLACK) {
                rect(rectBounds)
            }
            ctx.drawVector(Colors.WHITE) {
                rect(rectBounds.left - 1, rectBounds.top - 1, rectBounds.width + 2, rectBounds.height + 2)
            }
        }

        stage.mouse {
            scroll {
                //println("${it.scrollDeltaX}, ${it.scrollDeltaY}, ${it.scrollDeltaZ}")
                val multiplier = if (it.isShiftDown) 4 else 1
                val delta = (32.0 * multiplier).withSign(it.scrollDeltaY)
                when {
                    it.isAltDown -> camera.cameraX += delta
                    it.isCtrlDown -> camera.cameraY += delta
                    else -> camera.setZoomAt(camera.localMouseXY(views), camera.cameraZoom * (1.0 - (0.1 * multiplier).withSign(delta)))
                }
                //it.isCtrlDown
            }

            click {
                val view = actions.selectedView
                if (it.button == MouseButton.RIGHT) {
                    val hasView = view != null
                    gameWindow.showContextMenu(listOf(
                        GameWindow.MenuItem("Cut", enabled = hasView) { actions.requestCut() },
                        GameWindow.MenuItem("Copy", enabled = hasView) { actions.requestCopy() },
                        GameWindow.MenuItem("Paste") { actions.requestPaste() },
                        GameWindow.MenuItem("Duplicate") { actions.requestDuplicate() },
                        null,
                        GameWindow.MenuItem("Send to back", enabled = hasView) { actions.sendToBack() },
                        GameWindow.MenuItem("Bring to front", enabled = hasView) { actions.sendToFront() },
                    ))
                }
            }
            down {
                startSelectedMousePos.setTo(views.globalMouseX, views.globalMouseY)
                pressing = true
                action = ""
                movingCameraMouse = spaceBarIsBeingPressed() || (it.lastEvent.button == MouseButton.MIDDLE)

                if (movingCameraMouse) {
                    startCameraPos.setTo(camera.cameraX, camera.cameraY)
                } else {
                    currentAnchor = getScaleAnchorPoint(actions.selectedView, 10, AnchorKind.SCALING)
                        ?: getScaleAnchorPoint(actions.selectedView, 20, AnchorKind.ROTATING)

                    if (currentAnchor == null) {
                        val pickedView = stage.hitTest(it.lastPosStage)
                        val viewLeaf = pickedView.findLastAscendant { it is ViewLeaf }
                        val view = viewLeaf ?: pickedView
                        if (view !== stage && view !== root && view !== camera) {
                            actions.selectView(view)
                        } else {
                            actions.selectView(null)
                        }
                    } else {
                        val view = actions.selectedView
                        if (view != null) {
                            selectedViewSize.setTo(view.scaledWidth, view.scaledHeight)
                            selectedViewInitialRotation = view.rotation
                        }
                    }
                    actions.selectedView?.let { view ->
                        startSelectedViewPos.setTo(view.globalX, view.globalY)
                    }

                    //println("DOWN ON ${it.view}, $view2, ${it.lastPosStage}")
                }
            }
            up {
                pressing = false
                movingCameraMouse = false
                if (action != "") {
                    views.debugSaveView(action, actions.selectedView)
                }
                //save = true
            }

            onMoveAnywhere { e ->
                val view = actions.selectedView
                var dx = views.globalMouseX - startSelectedMousePos.x
                var dy = views.globalMouseY - startSelectedMousePos.y
                when {
                    pressing && movingCameraMouse -> {
                        camera.cameraX = startCameraPos.x - dx / camera.cameraZoom
                        camera.cameraY = startCameraPos.y - dy / camera.cameraZoom
                    }
                    pressing && view != null -> {
                        val anchor = currentAnchor
                        when (anchor?.kind) {
                            AnchorKind.SCALING -> {
                                action = "scaled"
                                //val dy = dx * (anchor.localBounds.height / anchor.localBounds.width)
                                //if (gridSnapping) {
                                //    dx = dx.nearestAlignedTo(gridWidth.toDouble())
                                //    dy = dy.nearestAlignedTo(gridHeight.toDouble())
                                //}
                                val newGlobalAnchorXY = anchor.globalAnchorXY + Point(dx, dy)
                                var newLocalAnchorXY = anchor.globalToLocal(newGlobalAnchorXY)
                                val oldLocalBounds = anchor.localBounds
                                val newLocalBounds = oldLocalBounds.clone()

                                //view.setPositionRelativeTo(root, grid.snap(view.getPositionRelativeTo(root)))

                                if (actions.gridSnapping) {
                                    val transformed = view.parent!!.getPointRelativeTo(newLocalAnchorXY, root)
                                    val snappedTransformed = grid.snap(transformed)
                                    newLocalAnchorXY = view.parent!!.getPointRelativeToInv(snappedTransformed, root)
                                }

                                when (anchor.anchor.sx) {
                                    0.0 -> {
                                        newLocalBounds.left = newLocalAnchorXY.x
                                        newLocalBounds.right = oldLocalBounds.right
                                    }
                                    0.5 -> Unit
                                    1.0 -> {
                                        newLocalBounds.left = oldLocalBounds.left
                                        newLocalBounds.right = newLocalAnchorXY.x
                                    }
                                }
                                when (anchor.anchor.sy) {
                                    0.0 -> {
                                        newLocalBounds.top = newLocalAnchorXY.y
                                        newLocalBounds.bottom = oldLocalBounds.bottom

                                        //val newHeight = newLocalBounds.width * (oldLocalBounds.height / oldLocalBounds.width)
                                        //newLocalBounds.top += newLocalBounds.height - newHeight
                                    }
                                    0.5 -> Unit
                                    1.0 -> {
                                        newLocalBounds.top = oldLocalBounds.top
                                        newLocalBounds.bottom = newLocalAnchorXY.y

                                        //newLocalBounds.height = newLocalBounds.width * (oldLocalBounds.height / oldLocalBounds.width)
                                    }
                                }

                                if (true) {
                                    //newLocalBounds.width = newLocalBounds.width
                                }

                                //if (gridSnapping) {
                                //    newLocalBounds.width = newLocalBounds.width.nearestAlignedTo(gridWidth)
                                //    newLocalBounds.height = newLocalBounds.height.nearestAlignedTo(gridHeight)
                                //}


                                //println("localAnchorXY=${anchor.localAnchorXY}, newLocalAnchorXY=$newLocalAnchorXY, newLocalBounds=$newLocalBounds, oldLocalBounds=${anchor.localBounds}")
                                //println("anchor=$anchor, newLocalBounds[${System.identityHashCode(newLocalBounds)}]=$newLocalBounds  -- oldLocalBounds[${System.identityHashCode(oldLocalBounds)}]=$oldLocalBounds")
                                //val pos = anchor.localPos + anchor.viewInitialMatrix.transform(newLocalBounds.topLeft)
                                //val delta = anchor.viewInitialMatrix.transform(anchor.localPos - newLocalBounds.topLeft)

                                view.scaledWidth = newLocalBounds.width
                                view.scaledHeight = newLocalBounds.height
                                //view.width = newLocalBounds.width
                                //view.height = newLocalBounds.height

                                //println(newLocalBounds.topLeft + Point(view.anchorDispX, view.anchorDispY))
                                val globalPos = anchor.localToGlobal(newLocalBounds.topLeft + Point(view.anchorDispX, view.anchorDispY))
                                //val globalPos = anchor.localToGlobal(newLocalBounds.topLeft - oldLocalBounds.topLeft)

                                view.globalX = globalPos.x
                                view.globalY = globalPos.y

                                //if (gridSnapping) {
                                //    view.globalX = view.globalX.nearestAlignedTo(gridWidth)
                                //    view.globalY = view.globalY.nearestAlignedTo(gridHeight)
                                //}

                                /*
                                    if (gridSnapping) {
                                        view.globalX = view.globalX
                                        view.globalY = view.globalY
                                    }
                                    view.scaledWidth = (selectedViewSize.width + dx)
                                    view.scaledHeight = (selectedViewSize.height + dy)
                                    */
                            }
                            AnchorKind.ROTATING -> {
                                action = "rotated"
                                val initialAngle = Angle.between(startSelectedViewPos, startSelectedMousePos)
                                val currentAngle = Angle.between(startSelectedViewPos, views.globalMouseXY)
                                val deltaAngle = currentAngle - initialAngle
                                view.rotation = (selectedViewInitialRotation + deltaAngle)
                                if (e.isShiftDown) {
                                    view.rotation = view.rotation.degrees.nearestAlignedTo(15.0).degrees
                                }
                            }
                            null -> {
                                action = "moved"
                                view.globalX = (startSelectedViewPos.x + dx)
                                view.globalY = (startSelectedViewPos.y + dy)
                                if (gridSnapping) {
                                    view.setPositionRelativeTo(root, grid.snap(view.getPositionRelativeTo(root)))
                                }
                            }
                        }
                        //save = true
                        //startSelectedViewPos.setTo(view2.globalX, view2.globalY)
                    }
                }
            }
        }

        stage.addUpdater {
            gameWindow.cursor = when {
                movingCameraMouse || spaceBarIsBeingPressed() -> GameWindow.Cursor.MOVE
                else -> com.soywiz.korgw.GameWindow.Cursor.fromAngle(getScaleAnchorPoint(actions.selectedView, 10, AnchorKind.SCALING)?.angle)
                    ?: GameWindow.Cursor.fromAngle(getScaleAnchorPoint(actions.selectedView, 20, AnchorKind.ROTATING)?.angle)?.let { GameWindow.Cursor.CROSSHAIR }
                    ?: GameWindow.Cursor.DEFAULT
            }


            //if (save) {
            //    save = false
            //    fileToEdit.save(stage.viewTreeToKTree(views))
            //}
        }
    }
}
