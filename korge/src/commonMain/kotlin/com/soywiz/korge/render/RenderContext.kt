package com.soywiz.korge.render

import com.soywiz.kds.*
import com.soywiz.korag.*
import com.soywiz.korge.stat.*
import com.soywiz.korge.view.*
import com.soywiz.korim.bitmap.*
import com.soywiz.korio.async.*
import kotlin.coroutines.*

class RenderContext(
	val ag: AG,
	val bp: BoundsProvider = BoundsProvider.Dummy,
	val stats: Stats = Stats(),
	val coroutineContext: CoroutineContext = EmptyCoroutineContext
) : Extra by Extra.Mixin(), BoundsProvider by bp {
	val agBitmapTextureManager = AgBitmapTextureManager(ag)
	var frame = 0
	val batch by lazy { BatchBuilder2D(this) }
	val ctx2d by lazy { RenderContext2D(batch, agBitmapTextureManager) }
    val flushers = Signal<Unit>()

	var masksEnabled = true

	fun flush() {
        flushers(Unit)
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
