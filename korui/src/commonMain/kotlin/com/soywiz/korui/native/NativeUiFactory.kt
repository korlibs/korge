package com.soywiz.korui.native

import com.soywiz.kds.*
import com.soywiz.korev.*
import com.soywiz.korim.bitmap.*
import com.soywiz.korim.color.*
import com.soywiz.korio.lang.*
import com.soywiz.korma.geom.*
import com.soywiz.korui.*

expect val DEFAULT_UI_FACTORY: NativeUiFactory

object DummyUiFactory : NativeUiFactory
open class DummyBase : NativeUiFactory.NativeComponent, Extra by Extra.Mixin() {
    override val factory: NativeUiFactory get() = DummyUiFactory
}

interface NativeUiFactory {
    fun wrapNative(native: Any?): NativeComponent = object : DummyBase(), NativeComponent {}
    fun wrapNativeContainer(native: Any?): NativeContainer = object : DummyBase(), NativeContainer {}
    fun createWindow(): NativeWindow = object : DummyBase(), NativeWindow {}
    fun createContainer(): NativeContainer = object : DummyBase(), NativeContainer {}
    fun createToolbar(): NativeToolbar = object : DummyBase(), NativeToolbar {}
    fun createScrollPanel(): NativeScrollPanel = object : DummyBase(), NativeScrollPanel {}
    fun createButton(): NativeButton = object : DummyBase(), NativeButton {}
    fun createLabel(): NativeLabel = object : DummyBase(), NativeLabel {}
    fun createCheckBox(): NativeCheckBox = object : DummyBase(), NativeCheckBox {}
    fun createTextField(): NativeTextField = object : DummyBase(), NativeTextField {}
    fun <T> createComboBox(): NativeComboBox<T> = object : DummyBase(), NativeComboBox<T> {}
    fun createTree(): NativeTree = object : DummyBase(), NativeTree {}
    fun createCanvas(): NativeCanvas = object : DummyBase(), NativeCanvas {}

    interface NativeToolbar : NativeContainer {
    }

    interface NativeButton : NativeComponent, NativeWithText {
        var icon: Bitmap?
            get() = null
            set(value) = Unit
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

        fun open(): Unit = Unit
        fun close(): Unit = Unit
        fun onChange(block: () -> Unit) = Disposable { }
    }

    interface NativeComponent : Extra {
        val factory: NativeUiFactory
        var bounds: RectangleInt
            get() = RectangleInt(0, 0, 0, 0)
            set(value) = Unit
        var cursor: UiCursor?
            get() = null
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
        var focusable: Boolean
            get() = true
            set(value) = Unit
        var enabled: Boolean
            get() = true
            set(value) = Unit

        fun onMouseEvent(handler: (MouseEvent) -> Unit): Disposable = Disposable { }
        fun onFocus(handler: (FocusEvent) -> Unit): Disposable = Disposable { }
        fun onResize(handler: (ReshapeEvent) -> Unit): Disposable = Disposable { }

        fun showPopupMenu(menu: List<UiMenuItem>, x: Int = Int.MIN_VALUE, y: Int = Int.MIN_VALUE) = Unit

        fun repaintAll() = Unit
        fun focus(focus: Boolean) = Unit
        fun updateUI() = Unit
    }

    interface NativeContainer : NativeComponent {
        val numChildren: Int get() = 0
        var backgroundColor: RGBA?
            get() = null
            set(value) = Unit
        fun getChildAt(index: Int): NativeComponent = TODO()
        fun insertChildAt(index: Int, child: NativeComponent): Unit = TODO()
        fun removeChild(child: NativeComponent): Unit = TODO()
        fun removeChildAt(index: Int): Unit = TODO()
    }

    interface NativeLabel : NativeComponent, NativeWithText {
        var icon: Bitmap?
            get() = null
            set(value) = Unit
    }

    interface NativeCanvas : NativeComponent {
        var image: Bitmap?
            get() = null
            set(value) = Unit
    }

    interface NativeScrollPanel : NativeContainer {
        var xbar: Boolean?
            get() = null
            set(value) = Unit
        var ybar: Boolean?
            get() = null
            set(value) = Unit
    }

    interface NativeTextField : NativeComponent, NativeWithText {
        fun select(range: IntRange? = 0 until Int.MAX_VALUE): Unit = Unit
        fun focus(): Unit = Unit
        fun onKeyEvent(block: (KeyEvent) -> Unit): Disposable = Disposable { }
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
        var menu: UiMenu?
            get() = null
            set(value) = Unit
        val pixelFactor: Double get() = 1.0
    }

    interface NativeWithText : NativeComponent {
        var text: String
            get() = ""
            set(value) = Unit
    }
}
