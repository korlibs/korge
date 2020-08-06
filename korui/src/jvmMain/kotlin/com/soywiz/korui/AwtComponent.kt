package com.soywiz.korui

import com.soywiz.kds.*
import com.soywiz.korev.*
import com.soywiz.korio.lang.*
import com.soywiz.korma.geom.*
import java.awt.*
import java.awt.Rectangle
import java.awt.event.*
import java.awt.event.MouseEvent
import javax.swing.*

internal val awtToWrappersMap = WeakMap<Component, AwtComponent>()

fun Component.toAwt(): AwtComponent? = awtToWrappersMap[this]

open class AwtComponent(override val factory: AwtUiFactory, val component: Component) : UiComponent, Extra by Extra.Mixin() {
    init {
        awtToWrappersMap[component] = this
    }

    override var bounds: RectangleInt
        get() {
            val b = component.bounds
            return RectangleInt(b.x, b.y, b.width, b.height)
        }
        set(value) {
            component.bounds = Rectangle(value.x, value.y, value.width, value.height)
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
            super.parent = p
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

    override fun repaintAll() {
        component.doLayout()
        component.revalidate()
        component.repaint()
    }

    override fun copyFrom(nchild: UiComponent) {
        this.bounds = nchild.bounds
        this.visible = nchild.visible
        this.enabled = nchild.enabled
    }
}
