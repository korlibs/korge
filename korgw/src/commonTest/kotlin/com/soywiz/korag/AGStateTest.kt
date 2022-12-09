package com.soywiz.korag

import com.soywiz.korag.log.*
import kotlin.test.*

class AGStateTest {
    @Test
    fun test() {
        val fb = AGLog().createFrameBuffer()
        fb.setSize(0, 0, 200, 600)
        fb.setExtra(hasDepth = false, hasStencil = true)
        fb.setSamples(4)
        assertEquals("AGFrameBufferInfo(width=200, height=600, hasDepth=false, hasStencil=true, samples=4)", fb.info.toString())
    }
}
