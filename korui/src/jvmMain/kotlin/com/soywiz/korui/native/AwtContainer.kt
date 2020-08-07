package com.soywiz.korui.native

import com.soywiz.korim.color.*
import com.soywiz.korma.geom.*
import java.awt.*
import java.awt.Rectangle
import javax.swing.*

open class AwtContainer(
    factory: BaseAwtUiFactory,
    val container: Container = factory.createJPanel(),
    val childContainer: Container = container
) : AwtComponent(factory, container), NativeUiFactory.NativeContainer {
    init {
        //container.layout = null
        childContainer.layout = null
    }

    override var backgroundColor: RGBA?
        get() = container.background?.toRgba()
        set(value) {
            //container.isOpaque
            //container.isOpaque = value != null
            container.background = value?.toAwt()
        }

    /*
    override var bounds: RectangleInt
        get() {
            val b = childContainer.bounds
            return RectangleInt(b.x, b.y, b.width, b.height)
        }
        set(value) {
            container.bounds = Rectangle(value.x, value.y, value.width, value.height)
        }
    */

    override val numChildren: Int get() = childContainer.componentCount
    override fun getChildAt(index: Int): NativeUiFactory.NativeComponent = awtToWrappersMap[childContainer.getComponent(index)] ?: error("Can't find component")
    override fun insertChildAt(index: Int, child: NativeUiFactory.NativeComponent) {
        childContainer.add((child as AwtComponent).component, index)
    }
    override fun removeChild(child: NativeUiFactory.NativeComponent): Unit {
        childContainer.remove((child as AwtComponent).component)
    }
    override fun removeChildAt(index: Int): Unit {
        childContainer.remove(index)
    }
}
