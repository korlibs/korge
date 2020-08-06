package com.soywiz.korui

import com.soywiz.kds.*
import com.soywiz.korio.util.*
import com.soywiz.korui.native.*

open class UiTree(app: UiApplication, val tree: NativeUiFactory.NativeTree = app.factory.createTree()) : UiComponent(app, tree) {
    var root by RedirectMutableField(tree::root)
}

interface UiTreeNode : Extra {
    val parent: UiTreeNode? get() = null
    val children: List<UiTreeNode>? get() = null
}

class SimpleUiTreeNode(val text: String, override val children: List<SimpleUiTreeNode>? = null) : UiTreeNode, Extra by Extra.Mixin() {
    override var parent: UiTreeNode? = null

    init {
        if (children != null) {
            for (child in children) {
                child.parent = this
            }
        }
    }

    override fun toString(): String = text
}

inline fun UiContainer.tree(block: UiTree.() -> Unit): UiTree {
    return UiTree(app).also { it.parent = this }.also(block)
}
