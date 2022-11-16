package com.soywiz.korui.native

import com.soywiz.kds.*
import com.soywiz.korev.*
import com.soywiz.korev.FocusEvent
import com.soywiz.korim.color.*
import com.soywiz.korio.file.*
import com.soywiz.korio.lang.*
import com.soywiz.korma.geom.*
import com.soywiz.korui.*
import com.soywiz.korui.native.util.*
import java.awt.*
import java.awt.Point
import java.awt.Rectangle
import java.awt.event.*
import java.awt.event.MouseEvent
import java.awt.image.*

internal val awtToWrappersMap = WeakMap<Component, AwtComponent>()

internal fun Component.toAwt(): AwtComponent? = awtToWrappersMap[this]

internal open class AwtComponent(override val factory: BaseAwtUiFactory, val component: Component) : NativeUiFactory.NativeComponent, Extra by Extra.Mixin() {
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

    override var cursor: UiCursor? = null
        set(value) {
            field = value
            component.cursor = value.toAwt()
        }

    //override fun setBounds(x: Int, y: Int, width: Int, height: Int) { component.setBounds(x, y, width, height) }

    override var parent: NativeUiFactory.NativeContainer? = null
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
        set(value) { component.isVisible = value }

    override var focusable: Boolean
        get() = component.isFocusable
        set(value) { component.isFocusable = value }

    override var enabled: Boolean
        get() = component.isEnabled
        set(value) { component.isEnabled = value }

    //var lastMouseEvent: java.awt.event.MouseEvent? = null

    override fun onFocus(handler: (FocusEvent) -> Unit): Disposable {
        val event = com.soywiz.korev.FocusEvent()

        fun dispatch(e: java.awt.event.FocusEvent, type: com.soywiz.korev.FocusEvent.Type) {
            event.type = type
            handler(event)
        }
        val listener = object : FocusAdapter() {
            override fun focusGained(e: java.awt.event.FocusEvent) = dispatch(e, FocusEvent.Type.FOCUS)
            override fun focusLost(e: java.awt.event.FocusEvent) = dispatch(e, FocusEvent.Type.BLUR)
        }
        component.addFocusListener(listener)
        return Disposable {
            component.removeFocusListener(listener)
        }
    }

    companion object {
        val blankCursor = Toolkit.getDefaultToolkit().createCustomCursor(BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB), Point(0, 0), "blank cursor");
    }

    override fun onMouseEvent(handler: (com.soywiz.korev.MouseEvent) -> Unit): Disposable {
        val event = com.soywiz.korev.MouseEvent()

        var lockingX = 0
        var lockingY = 0
        var lockingDeltaX = 0
        var lockingDeltaY = 0
        var locking = false
        var lastAwtComponent: Component? = null
        var lockAwtComponent: Component? = null
        var lastAwtCursor: Cursor? = null

        fun dispatch(e: MouseEvent, type: com.soywiz.korev.MouseEvent.Type) {
            event.component = e.component
            event.type = type
            event.button = MouseButton[e.button]
            event.isShiftDown = e.isShiftDown
            event.isCtrlDown = e.isControlDown
            event.isAltDown = e.isAltDown
            event.isMetaDown = e.isMetaDown
            if (event.typeUp && locking) {
                locking = false
                @Suppress("NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
                component.cursor = lastAwtCursor
            }
            if (locking) {
                val dx = e.xOnScreen - e.x
                val dy = e.yOnScreen - e.y
                lockingDeltaX += MouseInfo.getPointerInfo().location.x - lockingX
                lockingDeltaY += MouseInfo.getPointerInfo().location.y - lockingY
                event.x = lockingDeltaX - dx
                event.y = lockingDeltaY - dy
                Robot().mouseMove(lockingX, lockingY)
            } else {
                event.x = e.x
                event.y = e.y
            }
            handler(event)
        }

        event.requestLock = {
            val component = (event.component as? Component?)
            if (component != null) {
                lastAwtCursor = component.cursor
                component.cursor = blankCursor
            }
            locking = true
            lockingX = MouseInfo.getPointerInfo().location.x
            lockingY = MouseInfo.getPointerInfo().location.y
            lockingDeltaX = lockingX
            lockingDeltaY = lockingY
        }

        val listener = object : MouseAdapter() {
            override fun mouseClicked(e: MouseEvent) = dispatch(e, com.soywiz.korev.MouseEvent.Type.CLICK)
            override fun mousePressed(e: MouseEvent) = dispatch(e, com.soywiz.korev.MouseEvent.Type.DOWN)
            override fun mouseReleased(e: MouseEvent) = dispatch(e, com.soywiz.korev.MouseEvent.Type.UP)
            override fun mouseEntered(e: MouseEvent) = dispatch(e, com.soywiz.korev.MouseEvent.Type.ENTER)
            override fun mouseExited(e: MouseEvent) = dispatch(e, com.soywiz.korev.MouseEvent.Type.EXIT)
            //override fun mouseWheelMoved(e: MouseWheelEvent) = dispatch(e, com.soywiz.korev.MouseEvent.Type.WEE)
            override fun mouseDragged(e: MouseEvent) = dispatch(e, com.soywiz.korev.MouseEvent.Type.DRAG)
            override fun mouseMoved(e: MouseEvent) = dispatch(e, com.soywiz.korev.MouseEvent.Type.MOVE)
        }

        component.addMouseListener(listener)
        component.addMouseMotionListener(listener)
        return Disposable {
            component.removeMouseMotionListener(listener)
            component.removeMouseListener(listener)
        }
    }

    open val componentPane get() = component

    override fun onResize(handler: (ReshapeEvent) -> Unit): Disposable {
        val listener = object : ComponentAdapter() {
            override fun componentResized(e: ComponentEvent) {
                handler(ReshapeEvent(component.x, component.y, componentPane.width, componentPane.height))
            }
        }
        component.addComponentListener(listener)
        return Disposable {
            component.removeComponentListener(listener)
        }
    }

    override fun showPopupMenu(menu: List<UiMenuItem>, x: Int, y: Int) {
        val jmenu = factory.createJPopupMenu()
        //jmenu.border = DropShadowBorder()
        for (it in menu) jmenu.add(it.toMenuItem(factory))
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

    override fun openFileDialog(file: VfsFile?, filter: (VfsFile) -> Boolean): VfsFile? {
        return factory.awtOpenFileDialog(component, file, filter)
    }

    override fun openColorPickerDialog(color: RGBA, listener: ((RGBA) -> Unit)?): RGBA? {
        return factory.awtOpenColorPickerDialog(component, color, listener)
    }

    override fun repaintAll() {
        component.doLayout()
        component.revalidate()
        component.repaint()
    }

    override fun focus(focus: Boolean) {
        if (focus) {
            component.requestFocus()
        } else {
            component.parent.requestFocus()
        }
    }

    override fun updateUI() {
    }
}
