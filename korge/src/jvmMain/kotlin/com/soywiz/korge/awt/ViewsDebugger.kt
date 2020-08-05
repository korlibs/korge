package com.soywiz.korge.awt

import com.soywiz.kds.*
import com.soywiz.kds.iterators.*
import com.soywiz.kmem.*
import com.soywiz.korev.Event
import com.soywiz.korge.animate.*
import com.soywiz.korge.debug.*
import com.soywiz.korge.input.*
import com.soywiz.korge.internal.*
import com.soywiz.korge.scene.*
import com.soywiz.korge.view.*
import com.soywiz.korge.view.Container
import com.soywiz.korge.view.Image
import com.soywiz.korge.view.ktree.*
import com.soywiz.korgw.*
import com.soywiz.korim.bitmap.*
import com.soywiz.korim.color.*
import com.soywiz.korio.async.*
import com.soywiz.korio.file.*
import com.soywiz.korio.serialization.xml.*
import com.soywiz.korma.geom.*
import java.awt.*
import java.awt.event.*
import java.awt.event.KeyEvent
import java.awt.event.MouseEvent
import java.util.*
import javax.swing.*
import javax.swing.tree.*
import kotlin.coroutines.*
import javax.swing.SwingUtilities
import com.soywiz.korma.geom.Point
import com.soywiz.korev.MouseButton
import com.soywiz.korge.particle.*

val View.treeNode: ViewNode by Extra.PropertyThis<View, ViewNode> { ViewNode(this) }

class ViewNode(val view: View?) : TreeNode {
    val container = view as? Container?
    override fun toString(): String {
        if (view == null) return "null"
        return StringBuilder().apply {
            val nodeName = if (view.name != null) view.name else "#${view.index}"
            append("$nodeName (${view::class.simpleName})")
        }.toString()
    }

    override fun isLeaf(): Boolean = (container == null) || (view is ViewLeaf)
    fun childrenList(): List<View> {
        if (view is ViewLeaf) return listOf()
        return container?.children?.filter { it !is DummyView } ?: listOf()
    }

    override fun getChildAt(childIndex: Int): TreeNode? = childrenList().getOrNull(childIndex)?.treeNode
    override fun getChildCount(): Int = childrenList().size
    override fun getParent(): TreeNode? = view?.parent?.treeNode
    override fun getIndex(node: TreeNode?): Int = childrenList().indexOf((node as? ViewNode?)?.view)
    override fun getAllowsChildren(): Boolean = container != null
    @OptIn(KorgeInternal::class)
    override fun children() = Vector<Any>(childrenList()).elements() as Enumeration<out TreeNode>
}

class ViewDebuggerChanged(val view: View?) : Event()

class EditPropertiesComponent(view: View?, val views: Views) : JPanel(GridLayout(1, 1)) {
    var nodeTree: EditableNodeList? = null

    fun setView(view: View?, coroutineContext: CoroutineContext) {
        removeAll()
        this.nodeTree = null
        if (view == null) return
        val nodes = ArrayList<EditableNode>()
        if (view is KorgeDebugNode) {
            view.getDebugProperties(views)?.let {
                nodes.add(it)
            }
        }
        nodes.add(EditableSection("View") {
            add(view::name.toEditableProperty())
            add(view::colorMul.toEditableProperty(views = views))
            add(view::blendMode.toEditableProperty(BlendMode.values()))
            add(view::alpha.toEditableProperty(0.0, 1.0))
            add(view::speed.toEditableProperty(0.0, 1.0, supportOutOfRange = true))
            add(view::ratio.toEditableProperty(0.0, 1.0, supportOutOfRange = true))
            add(view::x.toEditableProperty(supportOutOfRange = true))
            add(view::y.toEditableProperty(supportOutOfRange = true))
            if (view is RectBase) {
                add(view::anchorX.toEditableProperty(0.0, 1.0, supportOutOfRange = true))
                add(view::anchorY.toEditableProperty(0.0, 1.0, supportOutOfRange = true))
                add(EditableButtonProperty("center") {
                    view.anchorX = 0.5
                    view.anchorY = 0.5
                    //view.anchorX = -view.width / 2
                    //view.anchorY = -view.height / 2
                })
            }
            if (view is AnBaseShape) {
                add(view::dx.toEditableProperty(name = "dx", supportOutOfRange = true))
                add(view::dy.toEditableProperty(name = "dy", supportOutOfRange = true))
                add(EditableButtonProperty("center") {
                    view.dx = (-view.width / 2).toFloat()
                    view.dy = (-view.height / 2).toFloat()
                })
            }
            add(view::scaledWidth.toEditableProperty(name = "width", supportOutOfRange = true))
            add(view::scaledHeight.toEditableProperty(name = "height", supportOutOfRange = true))
            add(view::scale.toEditableProperty(0.01, 2.0, supportOutOfRange = true))
            add(view::scaleY.toEditableProperty(0.01, 2.0, supportOutOfRange = true))
            add(view::scaleX.toEditableProperty(0.01, 2.0, supportOutOfRange = true))
            add(view::rotationDegrees.toEditableProperty(-360.0, 360.0, supportOutOfRange = false))
            add(view::skewX.toEditableProperty(0.0, 2.0, supportOutOfRange = true))
            add(view::skewY.toEditableProperty(0.0, 2.0, supportOutOfRange = true))
        })
        val nodeTree = EditableNodeList(nodes)
        this.nodeTree = nodeTree
        val propertyList = nodeTree.allBaseEditableProperty
        var updating = false
        propertyList.fastForEach { property ->
            property.onChange {
                if (it.triggeredByUser) {
                    updating = true
                    nodeTree.synchronizeProperties()
                    view.stage?.views?.debugSaveView(view)
                }
            }
        }
        add(PropertyPanel(nodeTree, coroutineContext) { view })
        revalidate()
        repaint()
    }

    fun update() {
        nodeTree?.synchronizeProperties()
    }

    init {
        setView(view, EmptyCoroutineContext)
    }
}

class ViewsDebuggerComponent(
    val views: Views,
    rootView: View? = views.stage,
    val coroutineContext: CoroutineContext = views.coroutineContext
) : JPanel(GridLayout(2, 1)) {
    val properties = EditPropertiesComponent(rootView, views).also { add(it) }
    var pasteboard: Xml? = null

    fun attachNewView(newView: View?) {
        if (newView == null) return
        (selectedView as Container?)?.addChild(newView)
        highlight(newView)
        save(newView)
    }

    fun save(newView: View? = selectedView) {
        views?.stage?.views?.debugSaveView(newView)
    }

    fun removeCurrentNode() {
        val parent = selectedView?.parent
        selectedView?.removeFromParent()
        highlight(parent)
        save(parent)
    }

    val tree: JTree = JTree(ViewNode(rootView)).apply {
        val tree = this
        addTreeSelectionListener {
            val viewNode = it.path.lastPathComponent as ViewNode
            properties.setView(viewNode.view, coroutineContext)
            (views ?: rootView?.stage?.views)?.renderContext?.debugAnnotateView = viewNode.view
        }
        addKeyListener(object : KeyAdapter() {
            override fun keyPressed(e: KeyEvent) {
                if (e.keyCode == KeyEvent.VK_DELETE) {
                    removeCurrentNode()
                }
            }
        })
        addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(e: MouseEvent) {
                if (SwingUtilities.isRightMouseButton(e)) {
                    val row = tree.getClosestRowForLocation(e.x, e.y)
                    tree.setSelectionRow(row)
                    val view = selectedView
                    val isContainer = view is Container

                    if (view != null) {
                        val popupMenu = JPopupMenu()
                        popupMenu.add(JMenuItem("Add image").also {
                            it.isEnabled = isContainer
                            it.addActionListener {
                                attachNewView(Image(Bitmaps.white).position(views.virtualWidth * 0.5, views.virtualWidth * 0.5).apply { setSize(100.0, 100.0) })
                            }
                        })
                        popupMenu.add(JMenuItem("Add solid rect").also {
                            it.isEnabled = isContainer
                            it.addActionListener {
                                attachNewView(SolidRect(100, 100, Colors.WHITE).position(views.virtualWidth * 0.5, views.virtualWidth * 0.5))
                            }
                        })
                        popupMenu.add(JMenuItem("Add ellipse").also {
                            it.isEnabled = isContainer
                            it.addActionListener {
                                attachNewView(Ellipse(50.0, 50.0, Colors.WHITE).position(views.virtualWidth * 0.5, views.virtualWidth * 0.5))
                            }
                        })
                        popupMenu.add(JMenuItem("Add container").also {
                            it.isEnabled = isContainer
                            it.addActionListener {
                                attachNewView(Container().position(views.virtualWidth * 0.5, views.virtualWidth * 0.5))
                            }
                        })
                        popupMenu.add(JMenuItem("Add TreeViewRef").also {
                            it.isEnabled = isContainer
                            it.addActionListener {
                                attachNewView(TreeViewRef().position(views.virtualWidth * 0.5, views.virtualWidth * 0.5))
                            }
                        })
                        popupMenu.add(JMenuItem("Add ParticleEmitter").also {
                            it.isEnabled = isContainer
                            it.addActionListener {
                                attachNewView(ParticleEmitterView(ParticleEmitter()).position(views.virtualWidth * 0.5, views.virtualWidth * 0.5))
                            }
                        })
                        popupMenu.add(JMenuItem("Add AnimationViewRef").also {
                            it.isEnabled = isContainer
                            it.addActionListener {
                                attachNewView(AnimationViewRef())
                            }
                        })
                        popupMenu.add(JSeparator())
                        popupMenu.add(JMenuItem("Cut").also {
                            it.addActionListener {
                                launchImmediately(coroutineContext) {
                                    pasteboard = view.viewTreeToKTree(views!!)
                                    removeCurrentNode()
                                }
                            }
                        })
                        popupMenu.add(JMenuItem("Copy").also {
                            it.addActionListener {
                                launchImmediately(coroutineContext) {
                                    pasteboard = view.viewTreeToKTree(views!!)
                                }
                            }
                        })
                        popupMenu.add(JMenuItem("Paste").also {
                            it.addActionListener {
                                val pasteboard = pasteboard
                                launchImmediately(coroutineContext) {
                                    val container = (view as? Container?) ?: view.parent
                                    if (pasteboard != null) {
                                        container?.addChild(pasteboard.ktreeToViewTree(views!!))
                                    }
                                }
                            }
                        })
                        popupMenu.add(JSeparator())
                        popupMenu.add(JMenuItem("Duplicate", KeyEvent.CTRL_DOWN_MASK or KeyEvent.VK_D).also {
                            it.addActionListener {
                                launchImmediately(coroutineContext) {
                                    val view = selectedView
                                    if (view != null) {
                                        val parent = view.parent
                                        val newChild = view.viewTreeToKTree(views).ktreeToViewTree(views)
                                        parent?.addChild(newChild)
                                        highlight(newChild)
                                    }
                                }
                            }
                        })
                        popupMenu.add(JSeparator())
                        popupMenu.add(JMenuItem("Remove view", KeyEvent.VK_DELETE).also {
                            it.addActionListener {
                                removeCurrentNode()
                            }
                        })
                        popupMenu.add(JSeparator())
                        popupMenu.add(JMenuItem("Send to back").also {
                            it.addActionListener {
                                view?.parent?.sendChildToBack(view)
                            }
                        })
                        popupMenu.add(JMenuItem("Bring to front").also {
                            it.addActionListener {
                                view?.parent?.sendChildToFront(view)
                            }
                        })
                        popupMenu.show(e.component, e.x, e.y)
                    }
                }
            }
        })
    }
    val selectedView: View? get() = (tree.selectionPath?.lastPathComponent as? ViewNode)?.view
    val treeScroll = myComponentFactory.scrollPane(tree).also { add(it) }

    //fun setRootView(root: View, coroutineContext: CoroutineContext, views: Views? = null) {
    fun setRootView(root: View) {
        //this.coroutineContext = coroutineContext
        //if (views != null) this.views = views
        //properties.views = views?.views
        tree.model = DefaultTreeModel(root.treeNode)
        update()
    }

    fun update() {
        tree.updateUI()
        properties.update()
    }

    fun highlight(view: View?) {
        update()
        val treeNode = view?.treeNode ?: return
        val path = TreePath((tree.model as DefaultTreeModel).getPathToRoot(treeNode))
        tree.expandPath(path)
        tree.clearSelection()
        tree.addSelectionPath(path)
    }
}

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

abstract class EditorModule() : Module() {
    open val editableNode: EditableNode? = null
}

suspend fun ktreeEditor(file: VfsFile): EditorModule {
    return myComponentFactory.createModule {
        views.name = "ktree"
        var save = false
        val views = this.views
        val gameWindow = this.views.gameWindow
        val stage = views.stage
        val viewTree = file.readKTree(views)
        val currentVfs = file.parent
        val viewsDebuggerComponent = injector.get<ViewsDebuggerComponent>()
        views.currentVfs = currentVfs

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
                                viewsDebuggerComponent.pasteboard = view!!.viewTreeToKTree(views, currentVfs)
                                selectView(view?.parent)
                                view!!.removeFromParent()
                            }
                        },
                        GameWindow.MenuItem("Copy", enabled = hasView) {
                            launchImmediately {
                                viewsDebuggerComponent.pasteboard = view!!.viewTreeToKTree(views, currentVfs)
                            }
                        },
                        GameWindow.MenuItem("Paste in place") {
                            val pasteboard = viewsDebuggerComponent.pasteboard
                            launchImmediately {
                                val container = (view as? Container?) ?: view?.parent ?: stage
                                if (pasteboard != null) {
                                    container.addChild(pasteboard.ktreeToViewTree(views, currentVfs))
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
                    val pickedView = stage.hitTest(it.lastPosStage)
                    val viewLeaf = pickedView.findLastAscendant { it is ViewLeaf }
                    val view = viewLeaf ?: pickedView
                    if (view !== stage) {
                        selectView(view)
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
                com.soywiz.korgw.GameWindow.Cursor.fromAngle(getScaleAnchorPoint(selectedView, 10, AnchorKind.SCALING)?.angle)
                    ?: GameWindow.Cursor.fromAngle(getScaleAnchorPoint(selectedView, 20, AnchorKind.ROTATING)?.angle)?.let { GameWindow.Cursor.CROSSHAIR }
                        ?: GameWindow.Cursor.DEFAULT

            if (save) {
                save = false
                launchImmediately {
                    file.writeString(stage.viewTreeToKTree(views).toOuterXmlIndented().toString())
                }
            }
        }
    }
}
