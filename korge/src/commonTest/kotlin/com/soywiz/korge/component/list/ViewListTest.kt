package com.soywiz.korge.component.list

import com.soywiz.korge.tests.*
import com.soywiz.korge.view.*
import com.soywiz.korim.color.*
import com.soywiz.korma.geom.*
import kotlin.test.*

class ViewListTest : ViewsForTesting() {
	@Test
	fun createList() {
		val item0 = SolidRect(10, 10, Colors.RED).apply { xy(0, 0) }
		val item1 = SolidRect(10, 10, Colors.RED).apply { xy(20, 0) }
		views.stage.addChild(item0)
		views.stage.addChild(item1)
		val itemList = ViewList(item0, item1, 3)
		assertEquals(3, itemList.length)
		assertEquals(Rectangle(40, 0, 10, 10), itemList[2]?.globalBounds)
	}
}
