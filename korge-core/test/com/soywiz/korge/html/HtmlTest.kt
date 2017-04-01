package com.soywiz.korge.html

import com.soywiz.korim.color.Colors
import com.soywiz.korma.geom.Rectangle
import org.junit.Assert
import org.junit.Test

class HtmlTest {
	@Test
	fun name() {
		val doc = Html.parse("""<p align="center"><font face="Times New Roman" size="33" color="#ffffff" letterSpacing="0.00" kerning="1">50%</font></p>""")
		Assert.assertEquals("50%", doc.text)
		doc.doPositioning(Html.MetricsProvider.Identity, Rectangle(0, 0, 100, 100))
		Assert.assertEquals(Rectangle(0, 0, 3, 1), doc.bounds)
		Assert.assertEquals(listOf(Rectangle(0, 0, 3, 1)), doc.allSpans.map { it.bounds })
		val format = doc.firstFormat
		Assert.assertEquals(Html.FontFace.Named("Times New Roman"), format.face)
		Assert.assertEquals(33, format.size)
		Assert.assertEquals(Colors.WHITE, format.color)
		Assert.assertEquals(1, format.kerning)
		Assert.assertEquals(0.0, format.letterSpacing, 0.0001)
		Assert.assertEquals(Html.Alignment.CENTER, format.align)
	}
}
