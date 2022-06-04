package com.soywiz.korim.vector

import com.soywiz.korim.color.Colors
import com.soywiz.korma.geom.Matrix
import com.soywiz.korma.geom.vector.VectorPath
import com.soywiz.korma.geom.vector.lineTo
import com.soywiz.korma.geom.vector.moveTo
import kotlin.test.Test
import kotlin.test.assertEquals

class ShapeTest {
	@Test
	fun name() {
		val shape = FillShape(
			path = VectorPath().apply {
				moveTo(0, 0)
				lineTo(100, 100)
				lineTo(0, 100)
				close()
			},
			clip = null,
			paint = Colors.GREEN,
			//paint = BitmapPaint(Bitmap32(100, 100, Colors.RED, premultiplied = false), Matrix()),
			transform = Matrix()
		)
		assertEquals(
			//"""<svg width="100px" height="100px" viewBox="0 0 100 100" xmlns="http://www.w3.org/2000/svg" xmlns:xlink="http://www.w3.org/1999/xlink"><defs><pattern id="def0" patternUnits="userSpaceOnUse" width="100" height="100" patternTransform="translate()"><image xlink:href="data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAGQAAABkCAYAAABw4pVUAAAA/UlEQVR4nO3RoQ0AMBDEsNt/6e8YDTAwj5TddnTsdwCGpBkSY0iMITGGxBgSY0iMITGGxBgSY0iMITGGxBgSY0iMITGGxBgSY0iMITGGxBgSY0iMITGGxBgSY0iMITGGxBgSY0iMITGGxBgSY0iMITGGxBgSY0iMITGGxBgSY0iMITGGxBgSY0iMITGGxBgSY0iMITGGxBgSY0iMITGGxBgSY0iMITGGxBgSY0iMITGGxBgSY0iMITGGxBgSY0iMITGGxBgSY0iMITGGxBgSY0iMITGGxBgSY0iMITGGxBgSY0iMITGGxBgSY0iMITGGxBgSY0iMITGGxBgS8wBKb9Zkl+sjewAAAABJRU5ErkJggg==" width="100" height="100"/></pattern></defs><g transform="translate()"><path d="M0 0L100 100L0 100Z" transform="translate()" fill="url(#def0)"/></g></svg>""",
            """<svg width="100px" height="100px" viewBox="0 0 100 100" xmlns="http://www.w3.org/2000/svg" xmlns:xlink="http://www.w3.org/1999/xlink"><defs/><g transform="translate()"><path d="M0 0L100 100L0 100Z" transform="translate()" fill="rgba(0,255,0,1)"/></g></svg>""",
			shape.toSvg().outerXml
		)
	}
}
