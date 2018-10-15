package com.soywiz.korge.render

import com.soywiz.kds.*
import com.soywiz.korag.*
import com.soywiz.korge.stat.*
import com.soywiz.korge.view.*
import com.soywiz.korim.bitmap.*
import kotlin.coroutines.*

class RenderContext(
	val ag: AG,
	val bp: BoundsProvider = BoundsProvider.Dummy,
	val stats: Stats = Stats(),
	val coroutineContext: CoroutineContext = EmptyCoroutineContext
) : Extra by Extra.Mixin(), BoundsProvider by bp {
	val agBitmapTextureManager = AgBitmapTextureManager(ag)
	var frame = 0
	val batch = BatchBuilder2D(ag)
	val ctx2d = RenderContext2D(batch)

	var masksEnabled = true

	fun flush() {
		batch.flush()
	}

	fun renderToTexture(width: Int, height: Int, render: () -> Unit, use: (Texture) -> Unit) {
		flush()
		ag.renderToTexture(width, height, render = {
			val oldScissors = batch.scissor
			batch.scissor = null
			try {
				render()
				flush()
			} finally {
				batch.scissor = oldScissors
			}
		}, use = {
			use(Texture(it, width, height))
			flush()
		})
	}

	fun renderToBitmap(bmp: Bitmap32, callback: () -> Unit): Bitmap32 {
		flush()
		ag.renderToBitmap(bmp) {
			callback()
			flush()
		}
		return bmp
	}

	fun finish() {
		ag.flip()
	}

	fun getTex(bmp: BmpSlice): Texture = agBitmapTextureManager.getTexture(bmp)
	fun getTex(bmp: Bitmap): Texture.Base = agBitmapTextureManager.getTextureBase(bmp)
}
