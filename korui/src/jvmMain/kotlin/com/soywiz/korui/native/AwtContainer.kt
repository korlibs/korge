package com.soywiz.korui.native

import com.soywiz.korim.color.*
import java.awt.*
import javax.swing.*

open class AwtContainer(factory: AwtUiFactory, val container: Container = JPanel(), val childContainer: Container = container) : AwtComponent(factory, container), NativeUiFactory.NativeContainer {
    init {
        container.layout = null
        childContainer.layout = null
    }

    override var backgroundColor: RGBA?
        get() = container.background?.toRgba()
        set(value) {
            //container.isOpaque
            //container.isOpaque = value != null
            container.background = value?.toAwt()
        }


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
