package com.soywiz.korge.awt

import com.soywiz.kds.*
import com.soywiz.kds.iterators.*
import com.soywiz.korev.Event
import com.soywiz.korge.animate.*
import com.soywiz.korge.debug.*
import com.soywiz.korge.internal.*
import com.soywiz.korge.view.*
import com.soywiz.korge.view.Container
import com.soywiz.korio.async.*
import com.soywiz.korui.*
import com.soywiz.korui.layout.*
import com.soywiz.korui.native.*
import java.awt.*
import java.awt.event.*
import java.awt.event.KeyEvent
import java.awt.event.MouseEvent
import java.util.*
import javax.swing.*
import javax.swing.tree.*
import kotlin.coroutines.*
import javax.swing.SwingUtilities

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
            add(view::skewXDegrees.toEditableProperty(0.0, 2.0, supportOutOfRange = true))
            add(view::skewYDegrees.toEditableProperty(0.0, 2.0, supportOutOfRange = true))
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

class ViewsDebuggerComponent constructor(
    val views: Views,
    val app: UiApplication,
    rootView: View? = views.stage,
    val coroutineContext: CoroutineContext = views.coroutineContext,
) : JPanel(GridLayout(3, 1)) {
    val actions = ViewsDebuggerActions(views, this)
    val properties = EditPropertiesComponent(rootView, views).also { add(it) }
    val uiProperties = UiEditProperties(app, rootView, views)

    init {
        views.debugHighlighters.add { view ->
            //println("HIGHLIGHTING: $view")
            println("ViewsDebuggerActions.highlight: $views")
            val treeNode = view?.treeNode
            if (treeNode != null) {
                val path = TreePath((tree.model as DefaultTreeModel).getPathToRoot(treeNode))
                println("   - $path")
                tree.expandPath(path)
                //tree.clearSelection()
                tree.selectionPath = path
                tree.scrollPathToVisible(path)
                //tree.repaint()
            } else {
                tree.clearSelection()
                selectView(null)
            }
            update()
        }
    }

    private fun selectView(view: View?) {
        properties.setView(view, coroutineContext)
        uiProperties.setView(view, coroutineContext)
        views.renderContext.debugAnnotateView = view
        (demo.toAwt()?.uiComponent as? UiContainer?)?.relayout()
    }

    val tree: JTree = JTree(ViewNode(rootView)).apply {
        val tree = this
        addTreeSelectionListener {
            println("addTreeSelectionListener: ${it.paths.toList()}")
            if (it.paths.isNotEmpty()) {
                selectView((it.path.lastPathComponent as ViewNode).view)
            } else {
                selectView(null)
            }
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
    val demo = JPanel().also {
        val panel = app.wrapContainer(it)
        panel.layout = VerticalUiLayout
        //panel.button("Hello")
        panel.addChild(uiProperties)
        //it.add(JButton())
        add(it)
        panel.relayout()
    }

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
        uiProperties.update()
    }
}
