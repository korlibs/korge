package com.soywiz.korge.view

import com.soywiz.korge.annotations.KorgeExperimental
import com.soywiz.korge.render.RenderContext
import com.soywiz.korim.bitmap.Bitmap32
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.completeWith

/**
 * Asynchronously renders this [View] (with the provided [views]) to a [Bitmap32] and returns it.
 * The rendering will happen before the next frame.
 */
suspend fun View.renderToBitmap(views: Views): Bitmap32 {
	val done = CompletableDeferred<Bitmap32>()

	views.onBeforeRender.once { ctx ->
        done.completeWith(kotlin.runCatching {
            unsafeRenderToBitmapSync(ctx).also {
                //println("/renderToBitmap")
            }
        })
	}

	return done.await()
}

@KorgeExperimental
fun View.unsafeRenderToBitmapSync(ctx: RenderContext): Bitmap32 {
    val view = this
    val bounds = getLocalBoundsOptimizedAnchored()

    return Bitmap32(bounds.width.toInt(), bounds.height.toInt()).also { bmp ->
        //val ctx = RenderContext(views.ag, coroutineContext = views.coroutineContext)
        //views.ag.renderToBitmap(bmp) {
        ctx.renderToBitmap(bmp, hasStencil = true) {
            ctx.useBatcher { batch ->
                batch.setViewMatrixTemp(view.globalMatrixInv) {
                    view.render(ctx)
                }
            }
        }
    }
}
