package com.soywiz.korui.native

import com.soywiz.korev.*
import com.soywiz.korev.KeyEvent
import com.soywiz.korio.lang.*
import java.awt.event.*
import javax.swing.*

open class AwtTextField(factory: AwtUiFactory, val textField: JTextField = JTextField()) : AwtComponent(factory, textField), NativeUiFactory.NativeTextField {
    override var text: String
        get() = textField.text
        set(value) = run { textField.text = value }

    override fun select(range: IntRange?) {
        if (range == null) {
            textField.select(0, 0)
        } else {
            textField.select(range.first, range.last + 1)
        }
    }
    override fun focus() = textField.requestFocus()
    override fun onKeyEvent(block: (KeyEvent) -> Unit): Disposable {
        val event = KeyEvent()

        fun dispatch(e: java.awt.event.KeyEvent, type: KeyEvent.Type) {
            event.type = type
            event.keyCode = e.keyCode
            event.character = e.keyChar
            event.key = awtKeyCodeToKey(e.keyCode)
            event.shift = e.isShiftDown
            event.ctrl = e.isControlDown
            event.alt = e.isAltDown
            event.meta = e.isMetaDown
            block(event)
        }

        val listener = object : KeyAdapter() {
            override fun keyTyped(e: java.awt.event.KeyEvent) = dispatch(e, KeyEvent.Type.TYPE)
            override fun keyPressed(e: java.awt.event.KeyEvent) = dispatch(e, KeyEvent.Type.DOWN)
            override fun keyReleased(e: java.awt.event.KeyEvent) = dispatch(e, KeyEvent.Type.UP)
        }
        textField.addKeyListener(listener)
        return Disposable { textField.removeKeyListener(listener) }
    }
}
