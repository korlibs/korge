package com.soywiz.korui.native

import com.soywiz.kds.*
import com.soywiz.korev.*
import com.soywiz.korim.bitmap.*
import com.soywiz.korim.color.*
import com.soywiz.korio.file.*
import com.soywiz.korio.lang.*
import com.soywiz.korma.geom.*
import com.soywiz.korui.*

expect val DEFAULT_UI_FACTORY: NativeUiFactory

object DummyUiFactory : NativeUiFactory {
    open class DummyComponent(val native: Any?) : NativeUiFactory.NativeComponent, Extra by Extra.Mixin() {
        override val factory: NativeUiFactory get() = DummyUiFactory
        override var bounds: RectangleInt = RectangleInt(0, 0, 0, 0)
        override var cursor: UiCursor? = null
        override var visible: Boolean = true
        override var focusable: Boolean = true
        override var enabled: Boolean = true
    }

    open class DummyContainer(native: Any?) : DummyComponent(native), NativeUiFactory.NativeContainer, NativeUiFactory.NativeChildren by NativeUiFactory.NativeChildren.Mixin() {
        override val factory: NativeUiFactory get() = DummyUiFactory
    }

    open class DummyWindow(native: Any?) : DummyContainer(native), NativeUiFactory.NativeWindow {
    }

    open class DummyButton(native: Any?) : DummyComponent(native), NativeUiFactory.NativeButton {
    }

    override fun wrapNative(native: Any?) = DummyComponent(native)
    override fun wrapNativeContainer(native: Any?) = DummyContainer(native)
    override fun createWindow() = DummyWindow(null)
    override fun createContainer() = DummyContainer(null)
    override fun createButton() = DummyButton(null)
}

interface NativeUiFactory {
    fun wrapNative(native: Any?): NativeComponent = TODO()
    fun wrapNativeContainer(native: Any?): NativeContainer = TODO()
    fun createWindow(): NativeWindow = TODO()
    fun createContainer(): NativeContainer = TODO()
    fun createToolbar(): NativeToolbar = TODO()
    fun createScrollPanel(): NativeScrollPanel = TODO()
    fun createButton(): NativeButton = TODO()
    fun createLabel(): NativeLabel = TODO()
    fun createCheckBox(): NativeCheckBox = TODO()
    fun createTextField(): NativeTextField = TODO()
    fun <T> createComboBox(): NativeComboBox<T> = TODO()
    fun createTree(): NativeTree = TODO()
    fun createCanvas(): NativeCanvas = TODO()

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
        //fun setBounds(x: Int, y: Int, width: Int, height: Int) = Unit
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

        fun repaintAll() = Unit
        fun focus(focus: Boolean) = Unit
        fun updateUI() = Unit

        fun showPopupMenu(menu: List<UiMenuItem>, x: Int = Int.MIN_VALUE, y: Int = Int.MIN_VALUE) = Unit
        fun openFileDialog(file: VfsFile?, filter: (VfsFile) -> Boolean): VfsFile? {
            TODO()
            return null
        }

        fun openColorPickerDialog(color: RGBA, listener: ((RGBA) -> Unit)?): RGBA? {
            return color
        }
    }

    interface NativeChildren {
        val numChildren: Int get() = 0
        fun getChildAt(index: Int): NativeComponent = TODO()
        fun insertChildAt(index: Int, child: NativeComponent): Unit = TODO()
        fun removeChild(child: NativeComponent): Unit = TODO()
        fun removeChildAt(index: Int): Unit = TODO()

        class Mixin : NativeChildren {
            val children = arrayListOf<NativeComponent>()

            override val numChildren: Int get() = children.size
            override fun getChildAt(index: Int): NativeComponent = children[index]
            override fun insertChildAt(index: Int, child: NativeComponent): Unit {
                if (index < 0) {
                    children.add(child)
                } else {
                    children.add(index, child)
                }
            }
            override fun removeChild(child: NativeComponent): Unit {
                children.remove(child)
            }
            override fun removeChildAt(index: Int): Unit {
                children.removeAt(index)
            }
        }
    }

    interface NativeContainer : NativeComponent, NativeChildren {
        var backgroundColor: RGBA?
            get() = null
            set(value) = Unit
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
