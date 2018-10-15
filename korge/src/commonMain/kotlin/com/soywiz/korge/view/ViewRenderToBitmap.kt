package com.soywiz.korge.view

import com.soywiz.korge.render.*
import com.soywiz.korim.bitmap.*
import kotlinx.coroutines.*

suspend fun View.renderToBitmap(views: Views): Bitmap32 {
	val view = this
	val bounds = getLocalBounds()
	val done = CompletableDeferred<Bitmap32>()

	views.onBeforeRender.once {
		done.complete(Bitmap32(bounds.width.toInt(), bounds.height.toInt()).also { bmp ->
			val ctx = RenderContext(views.ag, coroutineContext = views.coroutineContext)
			views.ag.renderToBitmap(bmp) {
				ctx.batch.setViewMatrixTemp(view.globalMatrixInv) {
					view.render(ctx)
				}
			}
		})
	}

	return done.await()
}
