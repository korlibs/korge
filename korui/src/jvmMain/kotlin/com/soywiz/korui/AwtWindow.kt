package com.soywiz.korui

import com.soywiz.korev.*
import com.soywiz.korio.lang.*
import java.awt.event.*
import javax.swing.*

open class AwtWindow(factory: AwtUiFactory, val frame: JFrame = JFrame()) : AwtContainer(factory, frame, frame.contentPane), UiWindow {
    init {
        frame.contentPane.layout = null
        frame.defaultCloseOperation = WindowConstants.EXIT_ON_CLOSE
        frame.setLocationRelativeTo(null)
    }

    override var visible: Boolean
        get() = super<AwtContainer>.visible
        set(value) {
            super<AwtContainer>.visible = value
            frame.setLocationRelativeTo(null)
        }
    override var title: String
        get() = frame.title
        set(value) {
            frame.title = value
        }

    override var menu: UiMenu? = null
        set(value) {
            field = value
            frame.jMenuBar = value?.toJMenuBar()
        }

    override fun onResize(handler: (ReshapeEvent) -> Unit): Disposable {
        val listener = object : ComponentAdapter() {
            override fun componentResized(e: ComponentEvent) {
                handler(ReshapeEvent(frame.x, frame.y, frame.contentPane.width, frame.contentPane.height))
            }
        }
        frame.addComponentListener(listener)
        return Disposable {
            frame.removeComponentListener(listener)
        }
    }
}
