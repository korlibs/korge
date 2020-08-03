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

    override fun getChildAt(childIndex: Int): TreeNode? = container?.get(childIndex)?.treeNode
    override fun getChildCount(): Int = container?.numChildren ?: 0
    override fun getParent(): TreeNode? = view?.parent?.treeNode
    override fun getIndex(node: TreeNode?): Int {
        val viewNode = node as? ViewNode?
        return viewNode?.view?.index ?: -1
    }

    override fun getAllowsChildren(): Boolean = container != null
    override fun isLeaf(): Boolean = container == null
    @OptIn(KorgeInternal::class)
    override fun children(): Enumeration<*> = Vector<Any>(container?.children?.toList() ?: listOf()).elements()
}

class EditPropertiesComponent(view: View?) : JPanel(GridLayout(1, 1)) {
    fun setView(view: View?) {
        removeAll()
        if (view == null) return
        add(PropertyPanel(EditableSection("Properties") {
            if (view is KorgeDebugNode) {
                for ((name, method) in view.getDebugMethods()) {
                    add(EditableButtonProperty(name) {
                        method()
                    })
                }
            }
            add(view::alpha.toEditableProperty(0.0, 1.0))
            add(view::speed.toEditableProperty(0.0, 1.0, supportOutOfRange = true))
            add(view::x.toEditableProperty(supportOutOfRange = true))
            add(view::y.toEditableProperty(supportOutOfRange = true))
            add(view::ratio.toEditableProperty(0.0, 1.0, supportOutOfRange = true))
            add(view::scale.toEditableProperty(0.01, 2.0, supportOutOfRange = true))
            add(view::scaleY.toEditableProperty(0.01, 2.0, supportOutOfRange = true))
            add(view::scaleX.toEditableProperty(0.01, 2.0, supportOutOfRange = true))
            add(view::rotationDegrees.toEditableProperty(-360.0, 360.0, supportOutOfRange = false))
        }))
        revalidate()
        repaint()
    }

    init {
        setView(view)
    }
}

class ViewsDebuggerComponent(rootView: View?) : JPanel(GridLayout(2, 1)) {
    val properties = EditPropertiesComponent(rootView).also { add(it) }
    val tree = JTree(ViewNode(rootView)).apply {
        addTreeSelectionListener {
            val viewNode = it.path.lastPathComponent as ViewNode
            properties.setView(viewNode.view)
        }
    }
    val treeScroll = myComponentFactory.scrollPane(tree).also { add(it) }

    fun setRootView(root: View) {
        tree.model = DefaultTreeModel(root.treeNode)
        update()
    }

    fun update() {
        tree.updateUI()
    }

    fun highlight(view: View?) {
        val treeNode = view?.treeNode ?: return
        val path = TreePath((tree.model as DefaultTreeModel).getPathToRoot(treeNode))
        tree.expandPath(path)
        tree.clearSelection()
        tree.addSelectionPath(path)
    }
}
