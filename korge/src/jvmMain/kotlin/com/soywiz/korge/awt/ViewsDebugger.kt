package com.soywiz.korge.awt

import com.soywiz.kds.*
import com.soywiz.kds.iterators.*
import com.soywiz.korev.Event
import com.soywiz.korge.animate.*
import com.soywiz.korge.debug.*
import com.soywiz.korge.internal.*
import com.soywiz.korge.view.*
import com.soywiz.korge.view.Container
import com.soywiz.korge.view.ktree.*
import com.soywiz.korio.async.*
import com.soywiz.korio.serialization.xml.*
import java.awt.*
import java.awt.event.*
import java.awt.event.KeyEvent
import java.awt.event.MouseEvent
import java.util.*
import javax.swing.*
import javax.swing.tree.*
import kotlin.coroutines.*
import javax.swing.SwingUtilities
import javax.swing.SwingUtilities.*

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
    init {
        views.debugHighlighters.add { view ->
            //println("HIGHLIGHTING: $view")
            println("ViewsDebuggerActions.highlight: $views")
            views.renderContext.debugAnnotateView = view
            invokeLater {
                val treeNode = view?.treeNode
                if (treeNode != null) {
                    val path = TreePath((tree.model as DefaultTreeModel).getPathToRoot(treeNode))
                    println("   - $path")
                    tree.expandPath(path)
                    //tree.clearSelection()
                    tree.selectionPath = path
                    tree.scrollPathToVisible(path)
                    tree.revalidate()
                    //tree.repaint()
                }
                update()
            }
        }
    }

    val actions = ViewsDebuggerActions(views, this)
    val properties = EditPropertiesComponent(rootView, views).also { add(it) }

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
                    actions.removeCurrentNode()
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

                        val popupMenu = myComponentFactory.createPopupMenu()

                        for (factory in myComponentFactory.getViewFactories(views)) {
                            popupMenu.add(myComponentFactory.createMenuItem("Add ${factory.name}").also {
                                it.isEnabled = isContainer
                                it.addActionListener {
                                    actions.attachNewView(factory.build().also {
                                        it.globalX = views.virtualWidth * 0.5
                                        it.globalY = views.virtualWidth * 0.5
                                    })
                                }
                            })
                        }

                        popupMenu.add(myComponentFactory.createSeparator())
                        popupMenu.add(myComponentFactory.createMenuItem("Cut").also {
                            it.addActionListener {
                                launchImmediately(coroutineContext) {
                                    actions.cut()
                                }
                            }
                        })
                        popupMenu.add(myComponentFactory.createMenuItem("Copy").also {
                            it.addActionListener {
                                launchImmediately(coroutineContext) {
                                    actions.copy()
                                }
                            }
                        })
                        popupMenu.add(myComponentFactory.createMenuItem("Paste").also {
                            it.addActionListener {
                                launchImmediately(coroutineContext) {
                                    actions.paste()
                                }
                            }
                        })
                        popupMenu.add(myComponentFactory.createSeparator())
                        popupMenu.add(myComponentFactory.createMenuItem("Duplicate", KeyEvent.CTRL_DOWN_MASK or KeyEvent.VK_D).also {
                            it.addActionListener {
                                launchImmediately(coroutineContext) {
                                    actions.duplicate()
                                }
                            }
                        })
                        popupMenu.add(myComponentFactory.createSeparator())
                        popupMenu.add(myComponentFactory.createMenuItem("Remove view", KeyEvent.VK_DELETE).also {
                            it.addActionListener {
                                actions.removeCurrentNode()
                            }
                        })
                        popupMenu.add(myComponentFactory.createSeparator())
                        popupMenu.add(myComponentFactory.createMenuItem("Send to back").also {
                            it.addActionListener {
                                actions.sendToBack()
                            }
                        })
                        popupMenu.add(myComponentFactory.createMenuItem("Bring to front").also {
                            it.addActionListener {
                                actions.sendToFront()
                            }
                        })
                        popupMenu.show(e.component, e.x, e.y)
                    }
                }
            }
        })
    }
    val selectedView: View? get() = actions.selectedView
    val treeScroll = myComponentFactory.scrollPane(tree).also { add(it) }

    //fun setRootView(root: View, coroutineContext: CoroutineContext, views: Views? = null) {
    //fun setRootView(root: View) {
    //    //this.coroutineContext = coroutineContext
    //    //if (views != null) this.views = views
    //    //properties.views = views?.views
    //    tree.model = DefaultTreeModel(root.treeNode)
    //    update()
    //}

    fun update() {
        tree.updateUI()
        properties.update()
    }
}
