package com.soywiz.korge.awt

import com.soywiz.kds.*
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
        if (view != null) {
            add(PropertyPanel(EditableSection("Properties", listOf(
                view::alpha.toEditableProperty(0.0, 1.0),
                view::x.toEditableProperty(supportOutOfRange = true),
                view::y.toEditableProperty(supportOutOfRange = true),
                view::scale.toEditableProperty(0.01, 2.0, supportOutOfRange = true),
                view::scaleY.toEditableProperty(0.01, 2.0, supportOutOfRange = true),
                view::scaleX.toEditableProperty(0.01, 2.0, supportOutOfRange = true),
                view::rotationDegrees.toEditableProperty(-360.0, 360.0, supportOutOfRange = false),
            ))))
        }
    }

    init {
        setView(view)
    }
}

class ViewsDebuggerComponent(rootView: View?) : JPanel(GridLayout(2, 1)) {
    val properties = EditPropertiesComponent(rootView).also { add(it) }
    val tree = JTree(ViewNode(rootView)).also { add(it) }.apply {
        addTreeSelectionListener {
            val viewNode = it.path.lastPathComponent as ViewNode
            properties.setView(viewNode.view)
        }
    }

    fun setRootView(root: View) {
        tree.model = DefaultTreeModel(root.treeNode)
        tree.updateUI()
    }

    fun update() {
        tree.updateUI()
    }
}
