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

                    myComponentFactory
                    if (view != null) {
                        val popupMenu = JPopupMenu()

                        for (factory in myComponentFactory.getViewFactories(views)) {
                            popupMenu.add(JMenuItem("Add ${factory.name}").also {
                                it.isEnabled = isContainer
                                it.addActionListener {
                                    attachNewView(factory.build().also {
                                        it.globalX = views.virtualWidth * 0.5
                                        it.globalY = views.virtualWidth * 0.5
                                    })
                                }
                            })
                        }

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
