package com.soywiz.korge.awt

import com.soywiz.kds.*
import com.soywiz.kds.iterators.*
import com.soywiz.korev.*
import com.soywiz.korev.Event
import com.soywiz.korge.animate.*
import com.soywiz.korge.debug.*
import com.soywiz.korge.internal.*
import com.soywiz.korge.view.*
import com.soywiz.korge.view.Container
import java.awt.*
import java.util.*
import javax.swing.*
import javax.swing.tree.*
import kotlin.coroutines.*

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

    fun childrenList(): List<View> = container?.children?.filter { it !is DummyView } ?: listOf()
    override fun getChildAt(childIndex: Int): TreeNode? = childrenList()?.getOrNull(childIndex)?.treeNode
    override fun getChildCount(): Int = childrenList()?.size ?: 0
    override fun getParent(): TreeNode? = view?.parent?.treeNode
    override fun getIndex(node: TreeNode?): Int = childrenList()?.indexOf((node as? ViewNode?)?.view) ?: -1
    override fun getAllowsChildren(): Boolean = container != null
    override fun isLeaf(): Boolean = container == null
    @OptIn(KorgeInternal::class)
    override fun children(): Enumeration<*> = Vector<Any>(childrenList()).elements()
}

class ViewDebuggerChanged(val view: View?) : Event()

class EditPropertiesComponent(view: View?) : JPanel(GridLayout(1, 1)) {
    var nodeTree: EditableNodeList? = null

    fun setView(view: View?, coroutineContext: CoroutineContext) {
        removeAll()
        this.nodeTree = null
        if (view == null) return
        val nodes = ArrayList<EditableNode>()
        if (view is KorgeDebugNode) {
            nodes.add(view.getDebugProperties())
        }
        nodes.add(EditableSection("View") {
            add(view::alpha.toEditableProperty(0.0, 1.0))
            add(view::speed.toEditableProperty(0.0, 1.0, supportOutOfRange = true))
            add(view::ratio.toEditableProperty(0.0, 1.0, supportOutOfRange = true))
            add(view::x.toEditableProperty(supportOutOfRange = true))
            add(view::y.toEditableProperty(supportOutOfRange = true))
            if (view is RectBase) {
                add(view::anchorX.toEditableProperty(supportOutOfRange = true))
                add(view::anchorY.toEditableProperty(supportOutOfRange = true))
                add(EditableButtonProperty("center") {
                    view.anchorX = -view.width / 2
                    view.anchorY = -view.height / 2
                })
            }
            if (view is AnBaseShape) {
                add(view::dx.toEditableProperty(name = "anchorX", supportOutOfRange = true))
                add(view::dy.toEditableProperty(name = "anchorY", supportOutOfRange = true))
                add(EditableButtonProperty("center") {
                    view.dx = (-view.width / 2).toFloat()
                    view.dy = (-view.height / 2).toFloat()
                })
            }
            add(view::width.toEditableProperty(supportOutOfRange = true))
            add(view::height.toEditableProperty(supportOutOfRange = true))
            add(view::scale.toEditableProperty(0.01, 2.0, supportOutOfRange = true))
            add(view::scaleY.toEditableProperty(0.01, 2.0, supportOutOfRange = true))
            add(view::scaleX.toEditableProperty(0.01, 2.0, supportOutOfRange = true))
            add(view::rotationDegrees.toEditableProperty(-360.0, 360.0, supportOutOfRange = false))
        })
        val nodeTree = EditableNodeList(nodes)
        this.nodeTree = nodeTree
        val propertyList = nodeTree.allBaseEditableProperty
        var updating = false
        propertyList.fastForEach { property ->
            property.onChange {
                if (!updating) {
                    try {
                        updating = true
                        nodeTree.synchronizeProperties()
                        view.stage?.views?.debugSaveView(view)
                    } finally {
                        updating = false
                    }
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

class ViewsDebuggerComponent(rootView: View?, private var coroutineContext: CoroutineContext = EmptyCoroutineContext, var views: Views? = null) : JPanel(GridLayout(2, 1)) {
    val properties = EditPropertiesComponent(rootView).also { add(it) }
    val tree = JTree(ViewNode(rootView)).apply {
        addTreeSelectionListener {
            val viewNode = it.path.lastPathComponent as ViewNode
            properties.setView(viewNode.view, coroutineContext)
            (views ?: rootView?.stage?.views)?.renderContext?.debugAnnotateView = viewNode.view
        }
    }
    val selectedView get() = (tree.selectionPath?.lastPathComponent as? ViewNode)?.view
    val treeScroll = myComponentFactory.scrollPane(tree).also { add(it) }

    fun setRootView(root: View, coroutineContext: CoroutineContext, views: Views? = null) {
        this.coroutineContext = coroutineContext
        if (views != null) {
            this.views = views
        }
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
