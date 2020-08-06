package com.soywiz.korui

import com.soywiz.kds.*

interface UiTree : UiComponent {
    var root: UiTreeNode?
        get() = null
        set(value) = Unit
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

fun UiContainer.tree(block: UiTree.() -> Unit): UiTree {
    return factory.createTree().also { it.parent = this }.also(block)
}
