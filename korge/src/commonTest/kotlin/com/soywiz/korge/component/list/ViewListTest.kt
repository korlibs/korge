package com.soywiz.korge.component.list

import com.soywiz.korge.tests.ViewsForTesting
import com.soywiz.korge.view.SolidRect
import com.soywiz.korge.view.xy
import com.soywiz.korim.color.Colors
import com.soywiz.korma.geom.MRectangle
import kotlin.test.Test
import kotlin.test.assertEquals

class ViewListTest : ViewsForTesting() {
	@Test
	fun createList() {
		val item0 = SolidRect(10, 10, Colors.RED).apply { xy(0, 0) }
		val item1 = SolidRect(10, 10, Colors.RED).apply { xy(20, 0) }
		views.stage.addChild(item0)
		views.stage.addChild(item1)
		val itemList = ViewList(item0, item1, 3)
		assertEquals(3, itemList.length)
		assertEquals(MRectangle(40, 0, 10, 10), itemList[2]?.globalBounds)
	}
}
