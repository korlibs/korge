package com.soywiz.korge.render

import com.soywiz.kds.*
import com.soywiz.korag.*
import com.soywiz.korge.stat.*
import com.soywiz.korge.view.*
import com.soywiz.korim.bitmap.*
import com.soywiz.korio.async.*
import kotlin.coroutines.*

/**
 * A context that allows to render objects.
 *
 * The [RenderContext] contains the [ag] [AG] (Accelerated Graphics),
 * that allow to render triangles and other primitives to the current render buffer.
 *
 * When doing 2D, you should usually use the [batch] to buffer vertices,
 * so they can be rendered at once when flushing.
 *
 * If you plan to do a custom drawing using [ag] directly, you should call [flush],
 * so all the pending vertices are rendered.
 *
 * If you want to perform drawing using a context allowing non-precomputed transformations
 * you can use [ctx2d].
 *
 * If you need to get textures from [Bitmap] that are allocated and deallocated as required
 * preventing leaks, you should use [getTex].
 */
class RenderContext(
    /** The Accelerated Graphics object that allows direct rendering */
	val ag: AG,
	val bp: BoundsProvider = BoundsProvider.Dummy,
    /** Object storing all the rendering [Stats] like number of batches, number of vertices etc. */
	val stats: Stats = Stats(),
	val coroutineContext: CoroutineContext = EmptyCoroutineContext
) : Extra by Extra.Mixin(), BoundsProvider by bp {
	val agBitmapTextureManager = AgBitmapTextureManager(ag)

    /** Current frame. */
    @Deprecated("unused")
	var frame = 0

    /** Allows to draw quads, sprites and nine patches using a precomputed global matrix or raw vertices */
	val batch by lazy { BatchBuilder2D(this) }

    /** [RenderContext2D] similar to the one from JS, that keeps an matrix (affine transformation) and allows to draw shapes using the current matrix */
	val ctx2d by lazy { RenderContext2D(batch, agBitmapTextureManager) }

    /** Allows to register handlers when the [flush] method is called */
    val flushers = Signal<Unit>()

    /**
     * Allows to toggle whether stencil-based masks are enabled or not.
     */
	var masksEnabled = true

    /**
     * Flushes all the pending renderings. This is called automatically at the end of the frame.
     * You should call this if you plan to render something else not managed via [batch],
     * so all the pending vertices are drawn.
     */
	fun flush() {
        flushers(Unit)
	}

    /**
     * Temporarily sets the render buffer to a temporal texture of the size [width] and [height] that can be used later in the [use] method.
     * First the texture is created, then [render] method is called once the render buffer is set to the texture,
     * and later the context is restored and the [use] method is called providing as first argument the rendered [Texture].
     * This method is useful for per-frame filters. If you plan to keep the texture data, consider using the [renderToBitmap] method.
     */
	inline fun renderToTexture(width: Int, height: Int, render: () -> Unit, use: (texture: Texture) -> Unit) {
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

    /**
     * Sets the render buffer temporarily to [bmp] [Bitmap32] and calls the [callback] render method that should perform all the renderings inside.
     */
	inline fun renderToBitmap(bmp: Bitmap32, callback: () -> Unit): Bitmap32 {
		flush()
		ag.renderToBitmap(bmp) {
			callback()
			flush()
		}
		return bmp
	}

    /**
     * Finishes the drawing and flips the screen. Called by the KorGe engine at the end of the frame.
     */
	fun finish() {
		ag.flip()
	}

    /**
     * Temporarily allocates a [Texture] with its coords from a [BmpSlice].
     * Textures are managed (allocated and de-allocated) automatically by the engine as required.
     * The texture coords matches the region in the [BmpSlice].
     */
	fun getTex(bmp: BmpSlice): Texture = agBitmapTextureManager.getTexture(bmp)

    /**
     * Allocates a [Texture.Base] from a [Bitmap]. A Texture.Base doesn't have region information.
     * It is just the whole texture/bitmap.
     */
    fun getTex(bmp: Bitmap): Texture.Base = agBitmapTextureManager.getTextureBase(bmp)
}
