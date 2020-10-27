package com.soywiz.korge.ui

import com.soywiz.kds.*
import com.soywiz.korev.*
import com.soywiz.korge.component.*
import com.soywiz.korge.input.*
import com.soywiz.korge.view.*
import com.soywiz.korim.color.*
import com.soywiz.korim.text.*
import com.soywiz.korio.lang.*
import com.soywiz.korma.geom.*
import com.soywiz.korui.*
import com.soywiz.korui.native.*

var View.koruiComponent by Extra.PropertyThis<View, NativeUiFactory.NativeComponent?> { null }

open class KorgeNativeUiFactory : NativeUiFactory {
    override fun wrapNative(native: Any?) = KorgeComponent(this, native as View)
    override fun wrapNativeContainer(native: Any?) = KorgeContainer(this, native as Container)

    override fun createWindow() = KorgeWindow(this, FixedSizeContainer())
    override fun createContainer() = KorgeContainer(this, FixedSizeContainer())
    override fun createButton() = KorgeButton(this, UITextButton())
    override fun createCheckBox() = KorgeCheckBox(this, UICheckBox())
    override fun <T> createComboBox() = KorgeComboBox(this, UIComboBox<T>())
    override fun createLabel() = KorgeLabel(this, UIText("").also { it.textAlignment = TextAlignment.MIDDLE_LEFT })
    override fun createTextField() = KorgeTextField(this, UIText(""))

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

        override fun onMouseEvent(handler: (MouseEvent) -> Unit): Disposable {
            var startedHere = false
            view.mouse {
                click {
                    //println("CLICK")
                    handler(MouseEvent(MouseEvent.Type.CLICK))
                }
                down {
                    handler(MouseEvent(MouseEvent.Type.DOWN))
                    startedHere = true
                }
                up {
                    handler(MouseEvent(MouseEvent.Type.UP))
                }
                upAnywhere {
                    startedHere = false
                }
                moveAnywhere {
                    if (startedHere && it.pressing) {
                        val x = view.localMouseX(views).toInt()
                        val y = view.localMouseY(views).toInt()
                        //println("dragging")
                        handler(MouseEvent(MouseEvent.Type.DRAG, x = x, y = y))
                    }
                }
            }
            return Disposable {
                view.mouse.removeFromView()
            }
        }
    }

    open class KorgeButton(override val factory: KorgeNativeUiFactory, val button: UITextButton) : KorgeComponent(factory, button), NativeUiFactory.NativeButton {
        override var text: String
            get() = button.text
            set(value) {
                button.text = value
            }
    }

    open class KorgeLabel(override val factory: KorgeNativeUiFactory, val uiText: UIText) : KorgeComponent(factory, uiText), NativeUiFactory.NativeLabel {
        override var text: String
            get() = uiText.text
            set(value) {
                uiText.text = value
            }
    }

    open class KorgeTextField(override val factory: KorgeNativeUiFactory, val uiText: UIText) : KorgeComponent(factory, uiText), NativeUiFactory.NativeTextField {
        override var text: String
            get() = uiText.text
            set(value) {
                uiText.text = value
            }
    }

    open class KorgeCheckBox(override val factory: KorgeNativeUiFactory, val checkBox: UICheckBox) : KorgeComponent(factory, checkBox), NativeUiFactory.NativeCheckBox {
        override var text: String
            get() = checkBox.text
            set(value) {
                checkBox.text = value
            }

        override var checked: Boolean
            get() = checkBox.checked
            set(value) {
                checkBox.checked = value
            }

        override fun onChange(block: () -> Unit): Disposable {
            return checkBox.onChange { block() }.disposable()
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
