package korlibs.korge.ui

import korlibs.korge.view.*
import korlibs.math.geom.*
import kotlin.test.*

class UIMaterialLayerTest {
    @Test
    fun testBounds() {
        val container = Container()
        val layer = container.uiMaterialLayer(Size(100, 100))
        assertEquals(Rectangle(0, 0, 100, 100).toInt(), layer.getBounds(container).toInt())
        layer.anchor = Anchor.CENTER
        assertEquals(Rectangle(-50, -50, 100, 100).toInt(), layer.getBounds(container).toInt())
    }
}
