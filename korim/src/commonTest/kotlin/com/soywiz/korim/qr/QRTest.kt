package com.soywiz.korim.qr

import com.soywiz.korma.geom.*
import kotlin.test.*

class QRTest {
	@Test
	fun name() {
        val img = QR().email("test@test.com")
        assertEquals(Size(29, 29), img.size)
	}
}
