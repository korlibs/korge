package com.soywiz.korge.render

import com.soywiz.korag.AG
import com.soywiz.korio.util.Extra

class RenderContext(
	val ag: AG
) : Extra by Extra.Mixin() {
	var frame = 0
	val batch = BatchBuilder2D(ag)
	val ctx2d = RenderContext2D(batch)

	fun flush() {
		batch.flush()
	}

	fun renderToTexture(width: Int, height: Int, callback: () -> Unit): Texture {
		flush()
		return Texture(ag.renderToTexture(width, height) {
			callback()
			flush()
		})
	}
}
