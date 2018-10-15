package com.soywiz.korge.component.list

import com.soywiz.korge.tests.ViewsForTesting
import com.soywiz.korim.color.Colors
import com.soywiz.korma.geom.Rectangle
import org.junit.Test
import kotlin.test.assertEquals

class ViewListTest : ViewsForTesting() {
	@Test
	fun createList() {
		val item0 = views.solidRect(10, 10, Colors.RED).apply { setXY(0, 0) }
		val item1 = views.solidRect(10, 10, Colors.RED).apply { setXY(20, 0) }
		views.stage.addChild(item0)
		views.stage.addChild(item1)
		val itemList = ViewList(item0, item1, 3)
		assertEquals(3, itemList.length)
		assertEquals(Rectangle(40, 0, 10, 10), itemList[2]?.globalBounds)
	}
}
