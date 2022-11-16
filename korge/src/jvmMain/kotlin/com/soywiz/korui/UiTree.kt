package com.soywiz.korui

import com.soywiz.kds.Extra
import com.soywiz.korui.native.NativeUiFactory

open class UiTree(app: UiApplication, val tree: NativeUiFactory.NativeTree = app.factory.createTree()) : UiComponent(app, tree) {
    var nodeRoot by tree::root
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
