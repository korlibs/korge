package com.soywiz.korge.ui

import com.soywiz.klock.milliseconds
import com.soywiz.korge.annotations.KorgeExperimental
import com.soywiz.korge.internal.DefaultViewport
import com.soywiz.korge.tests.ViewsForTesting
import com.soywiz.korge.view.BoundsProvider
import com.soywiz.korge.view.SolidRect
import com.soywiz.korge.view.View
import com.soywiz.korge.view.ViewsLog
import com.soywiz.korge.view.clipContainer
import com.soywiz.korge.view.fixedSizeContainer
import com.soywiz.korge.view.xy
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
        assertEquals(listOf(100.0, 132.0, 164.0, 196.0, 228.0), verticalList.children.map { it.globalY })
        assertEquals(5, verticalList.numChildren)
    }
}
