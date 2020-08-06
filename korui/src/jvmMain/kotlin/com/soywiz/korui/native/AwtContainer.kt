package com.soywiz.korui.native

import java.awt.*
import javax.swing.*

open class AwtContainer(factory: AwtUiFactory, val container: Container = JPanel(), val childContainer: Container = container) : AwtComponent(factory, container), NativeUiFactory.NativeContainer {
    init {
        container.layout = null
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
