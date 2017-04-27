package com.soywiz.korge.html

import com.soywiz.korim.color.Colors
import com.soywiz.korma.geom.Rectangle
import org.junit.Assert
import org.junit.Ignore
import org.junit.Test

class HtmlTest {
	@Test
	@Ignore("Not working right in this version")
	fun name() {
		val doc = Html.parse("""<p align="center"><font face="Times New Roman" size="33" color="#ffffff" letterSpacing="0.00" kerning="1">50%</font></p>""")
		Assert.assertEquals("50%", doc.text)
		doc.doPositioning(Html.MetricsProvider.Identity, Rectangle(0, 0, 100, 100))
		Assert.assertEquals(Rectangle(48.5, 0, 3, 1), doc.bounds)
		Assert.assertEquals(listOf(Rectangle(48.5, 0, 3, 1)), doc.allSpans.map { it.bounds })
		val format = doc.firstFormat
		Assert.assertEquals(Html.FontFace.Named("Times New Roman"), format.computedFace)
		Assert.assertEquals(33, format.computedSize)
		Assert.assertEquals(Colors.WHITE, format.computedColor)
		Assert.assertEquals(1, format.computedKerning)
		Assert.assertEquals(0.0, format.computedLetterSpacing, 0.0001)
		Assert.assertEquals(Html.Alignment.CENTER, format.computedAlign)
	}
}
