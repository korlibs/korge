package com.soywiz.korui

import com.soywiz.korio.lang.*

interface UiContainer : UiComponent {
    val numChildren: Int get() = 0
    fun getChildAt(index: Int): UiComponent = TODO()
    fun insertChildAt(index: Int, child: UiComponent): Unit = TODO()
    fun removeChild(child: UiComponent): Unit = TODO()
    fun removeChildAt(index: Int): Unit = TODO()
}

val UiContainer.size get() = numChildren
operator fun UiContainer.get(index: Int) = getChildAt(index)

fun UiContainer.replaceChildAt(index: Int, newChild: UiComponent): Unit {
    removeChildAt(index)
    insertChildAt(index, newChild)
}

fun UiContainer.removeChildren() {
    val initialNumChildren = numChildren
    while (numChildren > 0) {
        removeChildAt(numChildren - 1)
        if (initialNumChildren == numChildren) invalidOp
    }
}

fun UiContainer.addChild(child: UiComponent): Unit = insertChildAt(-1, child)

inline fun UiContainer.forEachChild(block: (UiComponent) -> Unit) {
    for (n in 0 until numChildren) {
        block(getChildAt(n))
    }
}

val UiContainer.children: List<UiComponent?> get() = (0 until numChildren).map { getChildAt(it) }

inline fun UiContainer.container(block: UiContainer.() -> Unit): UiContainer {
    return factory.createContainer()
        .also { it.parent = this }
        .also { it.bounds = this.bounds }
        .also(block)
}

inline fun UiContainer.addBlock(block: UiContainer.() -> Unit) {
    block(this)
}
