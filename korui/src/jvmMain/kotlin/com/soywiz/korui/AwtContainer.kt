package com.soywiz.korui

import java.awt.*
import javax.swing.*

open class AwtContainer(factory: AwtUiFactory, val container: Container = JPanel(), val childContainer: Container = container) : AwtComponent(factory, container), UiContainer {
    init {
        container.layout = null
    }

    override val numChildren: Int get() = childContainer.componentCount
    override fun getChild(index: Int): UiComponent = awtToWrappersMap[childContainer.getComponent(index)] ?: error("Can't find component")
}
