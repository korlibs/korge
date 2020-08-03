package com.soywiz.korge.awt

import com.soywiz.kds.*
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

class EditPropertiesComponent(view: View?) : JPanel(GridLayout(1, 1)) {
    fun setView(view: View?, coroutineContext: CoroutineContext) {
        removeAll()
        if (view == null) return
        val nodes = ArrayList<EditableNode>()
        if (view is KorgeDebugNode) {
            nodes.add(view.getDebugProperties())
        }
        nodes.add(EditableSection("View") {
            add(view::alpha.toEditableProperty(0.0, 1.0))
            add(view::speed.toEditableProperty(0.0, 1.0, supportOutOfRange = true))
            add(view::x.toEditableProperty(supportOutOfRange = true))
            add(view::y.toEditableProperty(supportOutOfRange = true))
            add(view::ratio.toEditableProperty(0.0, 1.0, supportOutOfRange = true))
            add(view::scale.toEditableProperty(0.01, 2.0, supportOutOfRange = true))
            add(view::scaleY.toEditableProperty(0.01, 2.0, supportOutOfRange = true))
            add(view::scaleX.toEditableProperty(0.01, 2.0, supportOutOfRange = true))
            add(view::rotationDegrees.toEditableProperty(-360.0, 360.0, supportOutOfRange = false))
        })
        add(PropertyPanel(EditableNodeList(nodes), coroutineContext))
        revalidate()
        repaint()
    }

    init {
        setView(view, EmptyCoroutineContext)
    }
}

class ViewsDebuggerComponent(rootView: View?, private var coroutineContext: CoroutineContext = EmptyCoroutineContext) : JPanel(GridLayout(2, 1)) {
    val properties = EditPropertiesComponent(rootView).also { add(it) }
    val tree = JTree(ViewNode(rootView)).apply {
        addTreeSelectionListener {
            val viewNode = it.path.lastPathComponent as ViewNode
            properties.setView(viewNode.view, coroutineContext)
        }
    }
    val treeScroll = myComponentFactory.scrollPane(tree).also { add(it) }

    fun setRootView(root: View, coroutineContext: CoroutineContext) {
        this.coroutineContext = coroutineContext
        tree.model = DefaultTreeModel(root.treeNode)
        update()
    }

    fun update() {
        tree.updateUI()
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
