package com.soywiz.korui

interface UiContainer : UiComponent {
    val numChildren: Int get() = 0
    fun getChild(index: Int): UiComponent = TODO()
}

inline fun UiContainer.forEachChild(block: (UiComponent) -> Unit) {
    for (n in 0 until numChildren) {
        block(getChild(n))
    }
}

val UiContainer.children: List<UiComponent?> get() = (0 until numChildren).map { getChild(it) }

fun UiContainer.container(block: UiContainer.() -> Unit): UiContainer {
    return factory.createContainer().also { it.setParent(this) }.also(block)
}

fun UiContainer.addBlock(block: UiContainer.() -> Unit) {
    block(this)
}
