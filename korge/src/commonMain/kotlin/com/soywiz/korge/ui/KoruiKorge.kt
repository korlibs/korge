package com.soywiz.korge.ui

import com.soywiz.kds.*
import com.soywiz.korge.view.*
import com.soywiz.korim.color.*
import com.soywiz.korma.geom.*
import com.soywiz.korui.*
import com.soywiz.korui.native.*

var View.koruiComponent by Extra.PropertyThis<View, NativeUiFactory.NativeComponent?> { null }

open class KorgeNativeUiFactory : NativeUiFactory {
    override fun wrapNative(native: Any?) = KorgeComponent(this, native as View)
    override fun wrapNativeContainer(native: Any?) = KorgeContainer(this, native as Container)

    override fun createWindow(): NativeUiFactory.NativeWindow = KorgeWindow(this, FixedSizeContainer())
    override fun createContainer(): NativeUiFactory.NativeContainer = KorgeContainer(this, FixedSizeContainer())
    override fun createButton(): NativeUiFactory.NativeButton = KorgeButton(this, TextButton())
    override fun createCheckBox(): NativeUiFactory.NativeCheckBox = KorgeCheckBox(this, UICheckBox())
    override fun <T> createComboBox(): NativeUiFactory.NativeComboBox<T> = KorgeComboBox(this, UIComboBox<T>())

    open class KorgeComponent(override val factory: KorgeNativeUiFactory, val view: View) : NativeUiFactory.NativeComponent, Extra by Extra.Mixin() {
        init {
            view.koruiComponent = this
        }

        override var bounds: RectangleInt
            get() = RectangleInt(view.x, view.y, view.width, view.height)
            set(value) {
                view.position(value.x, value.y)
                view.size(value.width, value.height)
            }

        override var parent: NativeUiFactory.NativeContainer? = null
            set(p) {
                field = p
                super.parent = p
            }

        override var visible: Boolean
            get() = view.visible
            set(value) = run { view.visible = value }
    }

    open class KorgeButton(override val factory: KorgeNativeUiFactory, val button: TextButton) : KorgeComponent(factory, button), NativeUiFactory.NativeButton {
        override var text: String
            get() = button.text
            set(value) {
                button.text = value
            }
    }

    open class KorgeCheckBox(override val factory: KorgeNativeUiFactory, val button: UICheckBox) : KorgeComponent(factory, button), NativeUiFactory.NativeCheckBox {
        override var text: String
            get() = button.text
            set(value) {
                button.text = value
            }

        override var checked: Boolean
            get() = button.checked
            set(value) {
                button.checked = value
            }
    }

    open class KorgeComboBox<T>(override val factory: KorgeNativeUiFactory, val comboBox: UIComboBox<T>) : KorgeComponent(factory, comboBox), NativeUiFactory.NativeComboBox<T> {
        override var items: List<T>
            get() = comboBox.items
            set(value) {
                comboBox.items = value
            }
        override var selectedItem: T?
            get() = comboBox.selectedItem
            set(value) {
                comboBox.selectedItem = value
            }

        override fun open() {
            comboBox.open()
        }

        override fun close() {
            comboBox.close()
        }
    }

    open class KorgeContainer(override val factory: KorgeNativeUiFactory, val container: Container) : KorgeComponent(factory, container), NativeUiFactory.NativeContainer {
        override val numChildren: Int get() = container.numChildren

        override var backgroundColor: RGBA?
            get() = super.backgroundColor
            set(value) {}

        override fun getChildAt(index: Int): NativeUiFactory.NativeComponent = container.getChildAt(index).koruiComponent!!
        override fun insertChildAt(index: Int, child: NativeUiFactory.NativeComponent) = container.addChildAt((child as KorgeComponent).view, index)
        override fun removeChild(child: NativeUiFactory.NativeComponent) = container.removeChild((child as KorgeComponent).view)
        override fun removeChildAt(index: Int) = container.removeChild(container.getChildAt(index))
    }

    open class KorgeWindow(override val factory: KorgeNativeUiFactory, val window: Container) : KorgeContainer(factory, window), NativeUiFactory.NativeWindow {
    }
}


fun Container.korui(width: Number = this.width, height: Number = this.height, block: UiContainer.() -> Unit): View {
    val app = UiApplication(KorgeNativeUiFactory())
    return (app.window(width.toInt(), height.toInt()) {
        block()
    }.component as KorgeNativeUiFactory.KorgeWindow)
        .also { addChild(it.view) }
        .view
}
