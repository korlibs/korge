package com.soywiz.korui

import com.soywiz.kds.*
import com.soywiz.korim.color.*
import com.soywiz.korio.lang.*
import com.soywiz.korio.util.*
import com.soywiz.korma.geom.*
import com.soywiz.korui.layout.*
import com.soywiz.korui.native.*

open class UiContainer(app: UiApplication, val container: NativeUiFactory.NativeContainer = app.factory.createContainer()) : UiComponent(app, container) {
    private val _children = arrayListOf<UiComponent>()
    val numChildren: Int get() = _children.size
    val size: Int get() = numChildren
    var backgroundColor: RGBA? by redirect(container::backgroundColor)

    var layout: UiLayout? = VerticalUiLayout

    fun relayout() {
        layout?.relayout(this)
    }

    override var bounds: RectangleInt
        get() = super.bounds
        set(value) {
            super.bounds = value
            relayout()
        }

    fun getChildIndex(child: UiComponent): Int = _children.indexOf(child)
    fun getChildAt(index: Int): UiComponent = _children[index]
    fun removeChildAt(index: Int): Unit = run {
        _children.removeAt(index)
        container.removeChildAt(index)
    }
    fun removeChild(child: UiComponent) {
        val index = getChildIndex(child)
        if (index >= 0) removeChildAt(index)
        child._parent = null
    }
    fun insertChildAt(index: Int, child: UiComponent) {
        if (child.parent != null) {
            child.parent?.removeChild(child)
        }
        container.insertChildAt(index, child.component)
        val rindex: Int = if (index < 0) numChildren + index else index
        _children.add(rindex.coerceAtLeast(0), child)
        child._parent = this
    }
    fun replaceChildAt(index: Int, newChild: UiComponent): Unit {
        removeChildAt(index)
        insertChildAt(index, newChild)
    }
    operator fun get(index: Int) = getChildAt(index)
    fun removeChildren() {
        val initialNumChildren = numChildren
        while (numChildren > 0) {
            removeChildAt(numChildren - 1)
            if (initialNumChildren == numChildren) invalidOp
        }
    }
    fun addChild(child: UiComponent): Unit = insertChildAt(-1, child)
    inline fun forEachChild(block: (UiComponent) -> Unit) {
        for (n in 0 until numChildren) {
            block(getChildAt(n))
        }
    }
    val children: List<UiComponent?> get() = _children.toList()
}

inline fun UiContainer.container(block: UiContainer.() -> Unit): UiContainer {
    return UiContainer(app)
        .also { it.parent = this }
        .also { it.bounds = this.bounds }
        .also(block)
}

inline fun UiContainer.addBlock(block: UiContainer.() -> Unit) {
    block(this)
}
