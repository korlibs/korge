package korlibs.korge.view

import korlibs.datastructure.*
import korlibs.image.bitmap.*
import korlibs.image.color.*
import korlibs.korge.annotations.*
import korlibs.korge.render.*
import korlibs.logger.*
import korlibs.math.geom.*
import kotlinx.coroutines.*

private val logger = Logger("RenderToBitmap")

/**
 * Asynchronously renders this [View] (with the provided [views]) to a [Bitmap32] and returns it.
 * The rendering will happen before the next frame.
 */
suspend fun View.renderToBitmap(views: Views? = this.stage?.views, region: Rectangle? = null, scale: Double = 1.0, outPoint: Ref<Point>? = null, includeBackground: Boolean = false): Bitmap32 {
    if (views == null) {
        logger.warn { "View.renderToBitmap Views not specified" }
        return Bitmap32(1, 1, Colors.TRANSPARENT.premultiplied)
    }
	val done = CompletableDeferred<Bitmap32>()

    // This will help to trigger a re-rendering in the case nothing else changed
    views.stage.invalidateRender()
    views.onBeforeRender.once { ctx ->
        done.completeWith(kotlin.runCatching {
            unsafeRenderToBitmapSync(ctx, region, scale, outPoint, bgcolor = if (includeBackground) views.clearColor else Colors.TRANSPARENT).also {
                //println("/renderToBitmap")
            }
        })
    }

    return done.await()
}

@KorgeExperimental
fun View.unsafeRenderToBitmapSync(
    ctx: RenderContext,
    region: Rectangle? = null,
    scale: Double = 1.0,
    outPoint: Ref<Point>? = null,
    useTexture: Boolean = true,
    bgcolor: RGBA = Colors.TRANSPARENT
): Bitmap32 {
    val view = this
    val bounds = getLocalBounds(includeFilters = true)

    //println("bounds=$bounds")

    return Bitmap32(
        (region?.width ?: bounds.width).toInt(),
        (region?.height ?: bounds.height).toInt(),
        premultiplied = true
    ).also { bmp ->
        //val ctx = RenderContext(views.ag, coroutineContext = views.coroutineContext)
        //views.ag.renderToBitmap(bmp) {
        ctx.renderToBitmap(bmp, hasStencil = true, useTexture = useTexture) {
            if (bgcolor != Colors.TRANSPARENT) {
                ctx.clear(bgcolor)
            }
            ctx.useBatcher { batch ->
                val matrix = view.globalMatrixInv.clone().mutable
                if (region != null) {
                    matrix.prescale(scale)
                    matrix.pretranslate(-region.x, -region.y)
                }
                matrix.translate(-bounds.x, -bounds.y)
                outPoint?.value = Point(bounds.x, bounds.y)
                batch.setViewMatrixTemp(matrix.immutable) {
                    view.render(ctx)
                }
            }
        }
    }
}
