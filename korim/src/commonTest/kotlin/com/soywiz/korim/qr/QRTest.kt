package com.soywiz.korim.qr

import com.soywiz.korma.geom.MSize
import kotlin.test.Test
import kotlin.test.assertEquals

class QRTest {
	@Test
	fun name() {
        val img = QR().email("test@test.com")
        assertEquals(MSize(29, 29), img.size)
	}
}
