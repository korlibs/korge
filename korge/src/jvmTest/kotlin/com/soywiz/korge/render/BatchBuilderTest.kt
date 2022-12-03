package com.soywiz.korge.render

import com.soywiz.korag.*
import com.soywiz.korag.log.*
import com.soywiz.korge.test.*
import kotlin.test.*

class BatchBuilderTest {
	val ag = LogAG(16, 16)
    val nag = NAGLog()
	val bb = BatchBuilder2D(RenderContext(nag))

	@Test
	fun simpleBatch() {
		val tex = Texture(NAGTexture(), 100, 100)
		bb.drawQuad(tex, 0f, 0f, premultiplied = tex.premultiplied, wrap = false)
		bb.flush()

        assertEqualsFileReference("korge/render/BatchBuilderSimpleBatch.log", ag.getLogAsString())
	}

	@Test
	fun batch2() {
		val tex = Texture(NAGTexture(), 100, 100)
		bb.drawQuad(tex, 0f, 0f, premultiplied = tex.premultiplied, wrap = false)
		bb.drawQuad(tex, 100f, 0f, premultiplied = tex.premultiplied, wrap = false)
		bb.flush()

        assertEqualsFileReference("korge/render/BatchBuilderBatch2.log", ag.getLogAsString())
	}
}
