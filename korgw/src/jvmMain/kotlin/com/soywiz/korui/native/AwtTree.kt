package com.soywiz.korui.native

import com.soywiz.kds.*
import com.soywiz.korui.*
import java.util.*
import javax.swing.*
import javax.swing.tree.*

val UiTreeNode.awt by Extra.PropertyThis<UiTreeNode, AwtTreeNode>() { AwtTreeNode(this) }

data class AwtTreeNode(val node: UiTreeNode) : TreeNode {
    override fun getChildAt(childIndex: Int): TreeNode? = node.children?.get(childIndex)?.awt
    override fun getChildCount(): Int = node.children?.size ?: 0
    override fun getParent(): TreeNode? = node.parent?.let { it.awt }
    override fun getIndex(node: TreeNode?): Int = this.node.children?.indexOf(node as UiTreeNode?) ?: -1
    override fun getAllowsChildren(): Boolean = node.children != null
    override fun isLeaf(): Boolean = node.children == null
    override fun children(): Enumeration<out TreeNode> = Vector(node.children ?: listOf()).elements() as Enumeration<out TreeNode>
    override fun toString(): String = node.toString()
}

open class AwtTree(factory: BaseAwtUiFactory, val tree: JTree = JTree()) : AwtComponent(factory, tree), NativeUiFactory.NativeTree {
    val model get() = tree.model as DefaultTreeModel
    override var root: UiTreeNode?
        get() = (model.root as? AwtTreeNode?)?.node
        set(value) {
            tree.model = DefaultTreeModel(value?.awt)
        }

    override fun select(node: UiTreeNode?) {
        val path = TreePath(model.getPathToRoot(node?.awt))
        tree.clearSelection()
        tree.selectionPath = path
        tree.expandPath(path)
    }

    override fun onSelect(block: (nodes: List<UiTreeNode>) -> Unit) {
        tree.addTreeSelectionListener {
            block(tree.selectionPaths.map { (it.lastPathComponent as AwtTreeNode).node })
        }
    }
}
