package korlibs.korge.ui

import korlibs.korge.annotations.*
import korlibs.korge.internal.*
import korlibs.korge.view.*
import korlibs.math.geom.*
import korlibs.time.*
import kotlin.coroutines.*
import kotlin.test.*

class UIVerticalListTest {
    @KorgeExperimental
    @Test
    fun testFitsChildren() {
        val viewsLog = ViewsLog(EmptyCoroutineContext)
        val views = viewsLog.views
        views.resized(DefaultViewport.WIDTH, (DefaultViewport.HEIGHT * 0.75).toInt())
        val clipContainer = views.stage.clipContainer(Size(256, 130)).xy(0, 100)
        val verticalList = clipContainer.uiVerticalList(object : UIVerticalList.Provider {
            override val numItems: Int get() = 100
            override val fixedHeight: Float get() = 32f
            override fun getItemHeight(index: Int): Float = fixedHeight
            override fun getItemView(index: Int, vlist: UIVerticalList): View = SolidRect(vlist.width, fixedHeight)
        })
        views.update(0.milliseconds)
        assertEquals(listOf(100.0, 132.0, 164.0, 196.0, 228.0), verticalList.children.map { it.globalPos.yD })
        assertEquals(5, verticalList.numChildren)
    }
}
