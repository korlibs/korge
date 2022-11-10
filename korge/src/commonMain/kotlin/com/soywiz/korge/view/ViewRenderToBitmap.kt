package com.soywiz.korge.view

import com.soywiz.korge.annotations.KorgeExperimental
import com.soywiz.korge.render.RenderContext
import com.soywiz.korim.bitmap.Bitmap32
import com.soywiz.korma.geom.Point
import com.soywiz.korma.geom.Rectangle
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.completeWith

/**
 * Asynchronously renders this [View] (with the provided [views]) to a [Bitmap32] and returns it.
 * The rendering will happen before the next frame.
 */
suspend fun View.renderToBitmap(views: Views, region: Rectangle? = null, scale: Double = 1.0, outPoint: Point = Point(), includeBackground: Boolean = false): Bitmap32 {
	val done = CompletableDeferred<Bitmap32>()

    // This will help to trigger a re-rendering in the case nothing else changed
    views.stage.invalidateRender()
    views.onBeforeRender.once { ctx ->
        done.completeWith(kotlin.runCatching {
            if (includeBackground) ctx.ag.clear(views.clearColor)
            unsafeRenderToBitmapSync(ctx, region, scale, outPoint).also {
                //println("/renderToBitmap")
            }
        })
    }

    return done.await()
}

@KorgeExperimental
fun View.unsafeRenderToBitmapSync(ctx: RenderContext, region: Rectangle? = null, scale: Double = 1.0, outPoint: Point = Point()): Bitmap32 {
    val view = this
    val bounds = getLocalBoundsOptimizedAnchored()

    //println("bounds=$bounds")

    return Bitmap32(
        (region?.width ?: bounds.width).toInt(),
        (region?.height ?: bounds.height).toInt()
    ).also { bmp ->
        //val ctx = RenderContext(views.ag, coroutineContext = views.coroutineContext)
        //views.ag.renderToBitmap(bmp) {
        ctx.renderToBitmap(bmp, hasStencil = true) {
            ctx.useBatcher { batch ->
                val matrix = view.globalMatrixInv.clone()
                if (region != null) {
                    matrix.prescale(scale)
                    matrix.pretranslate(-region.x, -region.y)
                }
                matrix.translate(-bounds.x, -bounds.y)
                outPoint.setTo(bounds.x, bounds.y)
                batch.setViewMatrixTemp(matrix) {
                    view.render(ctx)
                }
            }
        }
    }
}
