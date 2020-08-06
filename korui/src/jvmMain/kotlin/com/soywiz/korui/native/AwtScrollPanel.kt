package com.soywiz.korui.native

import com.soywiz.korma.geom.*
import java.awt.*
import javax.swing.*

open class AwtScrollPanel(
    factory: BaseAwtUiFactory,
    val view: JFixedSizeContainer = AwtContainer(factory, JFixedSizeContainer()).container as JFixedSizeContainer,
    //val view: JPanel = JPanel(),
    val scrollPanel: JScrollPane = JScrollPane(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED)
) : AwtContainer(factory, scrollPanel, view), NativeUiFactory.NativeScrollPanel {
    override var bounds: RectangleInt
        get() = super<AwtContainer>.bounds
        set(value) {
            super<AwtContainer>.bounds = value
            //scrollPanel.setViewportView()
            //view.setBounds(0, 0, value.width, value.height)
        }

    override var xbar: Boolean? = null
        set(value) {
            field = value
            scrollPanel.horizontalScrollBarPolicy = when (value) {
                null -> ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED
                true -> ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS
                false -> ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER
            }
        }
    override var ybar: Boolean? = null
        set(value) {
            field = value
            scrollPanel.verticalScrollBarPolicy = when (value) {
                null -> ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED
                true -> ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS
                false -> ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER
            }
        }

    init {
        //scrollPanel.layout = ScrollPaneLayout()
        //view.background = Color.GREEN
        //view.layout = BoxLayout(view, BoxLayout.Y_AXIS)
        //view.setBounds(0, 0, 1000, 1000)
        //view.minimumSize = Dimension(300, 300)

        scrollPanel.verticalScrollBar.unitIncrement = 16
        scrollPanel.horizontalScrollBar.unitIncrement = 16
        scrollPanel.setViewportView(view)
        //view.preferredSize = Dimension(2000, 2000)
        //view.size = Dimension(2000, 2000)
        //scrollPanel.viewport.extentSize = Dimension(2000, 2000)
        //scrollPanel.viewport.viewSize = Dimension(2000, 2000)
    }

    override fun updateUI() {
        super<AwtContainer>.updateUI()
        view.cachedBounds = null
    }
}

open class JFixedSizeContainer : JPanel() {
    init {
        this.layout = null
    }
    internal var cachedBounds: Dimension? = null
    override fun isPreferredSizeSet(): Boolean = true
    override fun preferredSize(): Dimension {
        //cachedBounds = null
        if (cachedBounds == null) {
            val bb = BoundsBuilder()
            for (n in 0 until componentCount) {
                val b = this.getComponent(n).bounds
                bb.add(b.x, b.y)
                bb.add(b.x + b.width, b.y + b.height)
            }
            cachedBounds = Dimension(bb.xmax.toInt(), bb.ymax.toInt())
        }
        return cachedBounds!!
    }
}
