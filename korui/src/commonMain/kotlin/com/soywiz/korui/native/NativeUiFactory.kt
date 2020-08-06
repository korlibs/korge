package com.soywiz.korui.native

import com.soywiz.kds.*
import com.soywiz.korev.*
import com.soywiz.korio.lang.*
import com.soywiz.korma.geom.*
import com.soywiz.korui.*

expect val DEFAULT_UI_FACTORY: NativeUiFactory

object DummyUiFactory : NativeUiFactory

interface NativeUiFactory {
    fun createWindow(): NativeWindow = object : NativeWindow, Extra by Extra.Mixin() { override val factory = this@NativeUiFactory }
    fun createContainer(): NativeContainer = object : NativeContainer, Extra by Extra.Mixin() { override val factory = this@NativeUiFactory }
    fun createScrollPanel(): NativeScrollPanel = object : NativeScrollPanel, Extra by Extra.Mixin() { override val factory = this@NativeUiFactory }
    fun createButton(): NativeButton = object : NativeButton, Extra by Extra.Mixin() { override val factory = this@NativeUiFactory }
    fun createLabel(): NativeLabel = object : NativeLabel, Extra by Extra.Mixin() { override val factory = this@NativeUiFactory }
    fun createCheckBox(): NativeCheckBox = object : NativeCheckBox, Extra by Extra.Mixin() { override val factory = this@NativeUiFactory }
    fun createTextField(): NativeTextField = object : NativeTextField, Extra by Extra.Mixin() { override val factory = this@NativeUiFactory }
    fun <T> createComboBox(): NativeComboBox<T> = object : NativeComboBox<T>, Extra by Extra.Mixin() { override val factory = this@NativeUiFactory }
    fun createTree(): NativeTree = object : NativeTree, Extra by Extra.Mixin() { override val factory = this@NativeUiFactory }

    interface NativeButton : NativeComponent, NativeWithText {
    }

    interface NativeCheckBox : NativeComponent, NativeWithText {
        var checked: Boolean
            get() = false
            set(value) = Unit
    }

    interface NativeComboBox<T> : NativeComponent {
        var items: List<T>
            get() = listOf()
            set(value) = Unit

        var selectedItem: T?
            get() = null
            set(value) = Unit
    }

    interface NativeComponent : Extra {
        val factory: NativeUiFactory
        var bounds: RectangleInt
            get() = RectangleInt(0, 0, 0, 0)
            set(value) = Unit
        fun setBounds(x: Int, y: Int, width: Int, height: Int) = Unit
        var parent: NativeContainer?
            get() = null
            set(value) {
                parent?.removeChild(this)
                value?.insertChildAt(-1, this)
            }
        var index: Int
            get() = -1
            set(value) = Unit
        var visible: Boolean
            get() = true
            set(value) = Unit
        var enabled: Boolean
            get() = true
            set(value) = Unit
        fun onMouseEvent(handler: (MouseEvent) -> Unit): Disposable = Disposable { }

        fun showPopupMenu(menu: List<UiMenuItem>, x: Int = Int.MIN_VALUE, y: Int = Int.MIN_VALUE) = Unit

        fun repaintAll() = Unit
    }

    interface NativeContainer : NativeComponent {
        val numChildren: Int get() = 0
        fun getChildAt(index: Int): NativeComponent = TODO()
        fun insertChildAt(index: Int, child: NativeComponent): Unit = TODO()
        fun removeChild(child: NativeComponent): Unit = TODO()
        fun removeChildAt(index: Int): Unit = TODO()
    }

    interface NativeLabel : NativeComponent, NativeWithText {
    }

    interface NativeScrollPanel : NativeContainer {
    }

    interface NativeTextField : NativeComponent, NativeWithText {
    }

    interface NativeTree : NativeComponent {
        var root: UiTreeNode?
            get() = null
            set(value) = Unit
        fun select(node: UiTreeNode?) = Unit

        fun onSelect(block: (nodes: List<UiTreeNode>) -> Unit) = Unit
    }

    interface NativeWindow : NativeContainer {
        var title: String
            get() = ""
            set(value) = Unit
        fun onResize(handler: (ReshapeEvent) -> Unit): Disposable = Disposable { }
        var menu: UiMenu?
            get() = null
            set(value) = Unit
    }

    interface NativeWithText : NativeComponent {
        var text: String
            get() = ""
            set(value) = Unit
    }
}
