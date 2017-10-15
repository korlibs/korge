package com.soywiz.korge.component.list

import com.soywiz.korge.tests.ViewsForTesting
import com.soywiz.korge.view.get
import com.soywiz.korim.color.Colors
import com.soywiz.korma.geom.Rectangle
import org.junit.Test
import kotlin.test.assertEquals

class GridViewListTest : ViewsForTesting() {
	@Test
	fun createGrid() {
		val rowTemplate = views.container().apply {
			this += views.solidRect(10, 10, Colors.RED).apply { setXY(0, 0); name = "cell0" }
			this += views.solidRect(10, 10, Colors.RED).apply { setXY(20, 0); name = "cell1" }
		}
		val row0 = rowTemplate.clone().apply { setXY(0, 0); name = "row0" }
		val row1 = rowTemplate.clone().apply { setXY(0, 20); name = "row1" }
		views.stage.addChild(row0)
		views.stage.addChild(row1)
		val gridView = GridViewList(views.stage["row0"]!!, views.stage["row1"]!!, { it["cell0"]!! to it["cell1"]!! }, 3, 3)
		val cell = gridView[2, 2]
		assertEquals(9, gridView.length)
		assertEquals(Rectangle(40, 40, 10, 10), cell?.globalBounds)
	}
}
