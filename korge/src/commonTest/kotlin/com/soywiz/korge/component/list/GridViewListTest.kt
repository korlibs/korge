package com.soywiz.korge.component.list

/*
import com.soywiz.korge.tests.*
import com.soywiz.korge.view.*
import com.soywiz.korim.color.*
import com.soywiz.korma.geom.*
import kotlin.test.*

class GridViewListTest : ViewsForTesting() {
	@Test
	fun createGrid() {
		val rowTemplate = Container().apply {
			this += SolidRect(10, 10, Colors.RED).apply { xy(0, 0); name = "cell0" }
			this += SolidRect(10, 10, Colors.RED).apply { xy(20, 0); name = "cell1" }
		}
		val row0 = rowTemplate.clone().apply { xy(0, 0); name = "row0" }
		val row1 = rowTemplate.clone().apply { xy(0, 20); name = "row1" }
		views.stage.addChild(row0)
		views.stage.addChild(row1)
		val gridView =
			GridViewList(views.stage["row0"].first, views.stage["row1"].first, { it["cell0"].first to it["cell1"].first }, 3, 3)
		val cell = gridView[2, 2]
		assertEquals(9, gridView.length)
		assertEquals(Rectangle(40, 40, 10, 10), cell?.globalBounds)
	}
}
*/
