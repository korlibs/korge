package com.soywiz.korui.factory

import java.awt.*
import javax.swing.*

actual val defaultKoruiFactory: KoruiFactory = AwtKoruiFactory()

val NativeUiComponent.component get() = rawComponent as Component
val NativeUiComponent.container get() = rawComponent as Container

class AwtKoruiFactory : KoruiFactory() {
    override fun createWindow(): NativeUiComponent = NativeUiComponent(JFrame().also {
        it.layout = null
        it.contentPane.layout = null
    })
    override fun createContainer(): NativeUiComponent = NativeUiComponent(JPanel().also {
        it.layout = null
    })
    override fun createButton(): NativeUiComponent = NativeUiComponent(JButton())
    override fun createLabel(): NativeUiComponent = NativeUiComponent(JLabel())

    override fun setParent(c: NativeUiComponent, p: NativeUiComponent?) {
        if (p == null) {
            c.component.parent?.remove(c.component)
        } else {
            p?.container?.add(c.component)
        }
    }

    override fun setChildIndex(c: NativeUiComponent?, index: Int) {
    }

    override fun setBounds(c: NativeUiComponent, x: Int, y: Int, width: Int, height: Int) {
        c.component.setBounds(x, y, width, height)
    }

    override fun getText(c: NativeUiComponent): String? {
        val component = c.component
        return when (component) {
            is AbstractButton -> component.text
            is Label -> component.text
            is Frame -> component.title
            else -> null
        }
    }

    override fun setText(c: NativeUiComponent, text: String) {
        val component = c.component
        when (component) {
            is AbstractButton -> component.text = text
            is Label -> component.text = text
            is Frame -> component.title = text
        }
    }

    override fun getVisible(c: NativeUiComponent): Boolean {
        return c.component.isVisible
    }

    override fun setVisible(c: NativeUiComponent, visible: Boolean) {
        c.component.isVisible = visible
    }
}
