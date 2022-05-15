package com.soywiz.korim.qr

import com.soywiz.korma.geom.Size
import kotlin.test.Test
import kotlin.test.assertEquals

class QRTest {
	@Test
	fun name() {
        val img = QR().email("test@test.com")
        assertEquals(Size(29, 29), img.size)
	}
}
