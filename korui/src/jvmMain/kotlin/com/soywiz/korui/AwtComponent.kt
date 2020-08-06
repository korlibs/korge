package com.soywiz.korui

import com.soywiz.kds.*
import com.soywiz.korev.*
import com.soywiz.korio.lang.*
import java.awt.*
import java.awt.event.*
import java.awt.event.MouseEvent
import javax.swing.*


internal val awtToWrappersMap = WeakMap<Component, AwtComponent>()

open class AwtComponent(override val factory: AwtUiFactory, val component: Component) : UiComponent, Extra by Extra.Mixin() {
    init {
        awtToWrappersMap[component] = this
    }

    override fun setBounds(x: Int, y: Int, width: Int, height: Int) {
        component.setBounds(x, y, width, height)
    }

    override var parent: UiContainer? = null
        //get() {
        //    println(component.parent.parent.parent)
        //    return awtToWrappersMap[component.parent] as? UiContainer?
        //}
        set(p) {
            field = p
            if (p == null) {
                component.parent?.remove(component)
                //field = null
            } else {
                //(p as AwtContainer).childContainer.add(component)
                (p as AwtContainer).container.add(component)
            }
        }

    override var index: Int
        get() = super.index
        set(value) {}

    override var visible: Boolean
        get() = component.isVisible
        set(value) = run { component.isVisible = value }

    override var enabled: Boolean
        get() = component.isEnabled
        set(value) = run { component.isEnabled = value }

    //var lastMouseEvent: java.awt.event.MouseEvent? = null

    override fun onMouseEvent(handler: (com.soywiz.korev.MouseEvent) -> Unit): Disposable {
        val event = com.soywiz.korev.MouseEvent()

        fun dispatch(e: MouseEvent, type: com.soywiz.korev.MouseEvent.Type) {
            event.button = MouseButton[e.button]
            event.x = e.x
            event.y = e.y
            event.type = type
            handler(event)
        }

        val listener = object : MouseAdapter() {
            override fun mouseClicked(e: MouseEvent) = dispatch(e, com.soywiz.korev.MouseEvent.Type.CLICK)
        }

        component.addMouseListener(listener)
        return Disposable {
            component.removeMouseListener(listener)
        }
    }

    override fun showPopupMenu(menu: List<UiMenuItem>, x: Int, y: Int) {
        val jmenu = JPopupMenu()
        for (it in menu) jmenu.add(it.toMenuItem())
        //var x = if (x >= 0) x
        try {
            jmenu.show(
                component,
                if (x == Int.MIN_VALUE) component.mousePosition?.x ?: 0 else x,
                if (y == Int.MIN_VALUE) component.mousePosition?.y ?: 0 else y
            )
        } catch (e: Throwable) {
            e.printStackTrace()
        }
    }
}
