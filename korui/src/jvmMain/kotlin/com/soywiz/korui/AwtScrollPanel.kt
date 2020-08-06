package com.soywiz.korui

import java.awt.*
import javax.swing.*

open class JFixedSizeContainer : JPanel() {
    init {
        this.layout = null
    }
    var myPreferredSize = Dimension(2000, 2000)
    override fun isPreferredSizeSet(): Boolean = true
    override fun preferredSize(): Dimension = myPreferredSize
}

open class AwtScrollPanel(
    factory: AwtUiFactory,
    val view: JFixedSizeContainer = AwtContainer(factory, JFixedSizeContainer()).container as JFixedSizeContainer,
    val scrollPanel: JScrollPane = JScrollPane(view, ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS)
) : AwtContainer(factory, scrollPanel, view), UiScrollPanel {
    init {
        //view.preferredSize = Dimension(2000, 2000)
        //view.size = Dimension(2000, 2000)
        //scrollPanel.viewport.extentSize = Dimension(2000, 2000)
        //scrollPanel.viewport.viewSize = Dimension(2000, 2000)
    }
}
