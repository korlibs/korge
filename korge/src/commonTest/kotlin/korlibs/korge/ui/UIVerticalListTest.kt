package korlibs.korge.ui

import korlibs.time.milliseconds
import korlibs.korge.annotations.KorgeExperimental
import korlibs.korge.internal.DefaultViewport
import korlibs.korge.tests.ViewsForTesting
import korlibs.korge.view.BoundsProvider
import korlibs.korge.view.SolidRect
import korlibs.korge.view.View
import korlibs.korge.view.ViewsLog
import korlibs.korge.view.clipContainer
import korlibs.korge.view.fixedSizeContainer
import korlibs.korge.view.xy
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.test.Test
import kotlin.test.assertEquals

class UIVerticalListTest {
    @KorgeExperimental
    @Test
    fun testFitsChildren() {
        val viewsLog = ViewsLog(EmptyCoroutineContext)
        val views = viewsLog.views
        views.resized(DefaultViewport.WIDTH, (DefaultViewport.HEIGHT * 0.75).toInt())
        val clipContainer = views.stage.clipContainer(256.0, 130.0).xy(0, 100)
        val verticalList = clipContainer.uiVerticalList(object : UIVerticalList.Provider {
            override val numItems: Int get() = 100
            override val fixedHeight: Double get() = 32.0
            override fun getItemHeight(index: Int): Double = fixedHeight
            override fun getItemView(index: Int, vlist: UIVerticalList): View = SolidRect(vlist.width, fixedHeight)
        })
        views.update(0.milliseconds)
        assertEquals(listOf(100.0, 132.0, 164.0, 196.0, 228.0), verticalList.children.map { it.globalPos.yD })
        assertEquals(5, verticalList.numChildren)
    }
}