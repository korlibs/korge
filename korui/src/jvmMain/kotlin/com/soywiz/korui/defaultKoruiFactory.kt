package com.soywiz.korui

import com.soywiz.kds.*
import com.soywiz.korev.*
import com.soywiz.korev.MouseEvent
import com.soywiz.korio.lang.*
import com.soywiz.korui.*
import java.awt.*
import java.awt.event.*
import javax.swing.*

actual val defaultKoruiFactory: KoruiFactory = AwtKoruiFactory()

open class AwtKoruiFactory : KoruiFactory {
    override fun createWindow() = AwtWindow(this)
    override fun createContainer() = AwtContainer(this)
    override fun createButton() = AwtButton(this)
    override fun createLabel() = AwtLabel(this)
    override fun createTextField() = AwtTextField(this)
    override fun <T> createComboBox() = AwtComboBox<T>(this)
}

open class AwtComponent(override val factory: AwtKoruiFactory, val component: Component) : UiComponent, Extra by Extra.Mixin() {
    override fun setBounds(x: Int, y: Int, width: Int, height: Int) {
        component.setBounds(x, y, width, height)
    }

    override fun setParent(p: UiContainer?) {
        if (p == null) {
            component.parent?.remove(component)
        } else {
            (p as AwtContainer).container.add(component)
        }
    }

    override var index: Int
        get() = super.index
        set(value) {}

    override var visible: Boolean
        get() = component.isVisible
        set(value) = run { component.isVisible = value }

    override fun addMouseEventListener(handler: (MouseEvent) -> Unit): Disposable {
        val event = MouseEvent()

        fun dispatch(e: java.awt.event.MouseEvent, type: MouseEvent.Type) {
            event.button = MouseButton[e.button]
            event.x = e.x
            event.y = e.y
            event.type = type
            handler(event)
        }

        val listener = object : MouseAdapter() {
            override fun mouseClicked(e: java.awt.event.MouseEvent) = dispatch(e, MouseEvent.Type.CLICK)
        }

        component.addMouseListener(listener)
        return Disposable {
            component.removeMouseListener(listener)
        }
    }
}

open class AwtContainer(factory: AwtKoruiFactory, val container: Container = JPanel()) : AwtComponent(factory, container), UiContainer {
    init {
        container.layout = null
    }
}

open class AwtWindow(factory: AwtKoruiFactory, val window: JFrame = JFrame()) : AwtContainer(factory, window), UiWindow {
    init {
        window.contentPane.layout = null
        window.defaultCloseOperation = WindowConstants.EXIT_ON_CLOSE
    }
}

open class AwtButton(factory: AwtKoruiFactory, val button: JButton = JButton()) : AwtComponent(factory, button), UiButton {
    override var text: String
        get() = button.text
        set(value) = run { button.text = value }
}

open class AwtLabel(factory: AwtKoruiFactory, val label: JLabel = JLabel()) : AwtComponent(factory, label), UiLabel {
    override var text: String
        get() = label.text
        set(value) = run { label.text = value }
}

open class AwtTextField(factory: AwtKoruiFactory, val textField: JTextField = JTextField()) : AwtComponent(factory, textField), UiTextField {
    override var text: String
        get() = textField.text
        set(value) = run { textField.text = value }
}

open class AwtComboBox<T>(factory: AwtKoruiFactory, val comboBox: JComboBox<T> = JComboBox<T>()) : AwtComponent(factory, comboBox), UiComboBox<T> {
    override var items: List<T>
        get() {
            val model = comboBox.model
            return (0 until model.size).map { model.getElementAt(it) }
        }
        set(value) {
            comboBox.model = DefaultComboBoxModel((value as List<Any>).toTypedArray()) as DefaultComboBoxModel<T>
        }

    override var selectedItem: T?
        get() = comboBox.selectedItem as T?
        set(value) = run { comboBox.selectedItem = value }

}
