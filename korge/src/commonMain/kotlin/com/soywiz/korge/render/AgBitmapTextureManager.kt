package com.soywiz.korge.render

import com.soywiz.kds.*
import com.soywiz.kds.iterators.*
import com.soywiz.korag.*
import com.soywiz.korge.annotations.*
import com.soywiz.korge.internal.*
import com.soywiz.korim.bitmap.*
import com.soywiz.korma.geom.Rectangle

/** Extra property for [Bitmap] objects that specify whether mipmaps should be created for this Bitmap */
var Bitmap.texMipmaps: Boolean by Extra.Property { false }

/** Enable or disable mipmap generation for this [Bitmap] */
fun <T : Bitmap> T.mipmaps(enable: Boolean = true): T = this.apply { this.texMipmaps = enable }

/**
 * Class in charge of automatically handling [AG.Texture] <-> [Bitmap] conversion.
 *
 * To simplify texture storage (which usually require uploading to the GPU, and releasing it once not used),
 * the [AgBitmapTextureManager] allows to get temporal textures that are available while referenced in the coming 60 frames.
 * If it has been 60 frames without being referenced, the textures are collected.
 * This greatly simplifies texture management, but might have an impact in performance.
 * If you want to keep a Bitmap or an atlas in the GPU so there is no impact on uploading when required,
 * you can just call any of the getTexture* methods here each frame, even if not using it in the current frame.
 * You can also manage [Texture] manually, but you should release the textures manually by calling [Texture.close] so the resources are freed.
 */
@OptIn(KorgeInternal::class, KorgeExperimental::class)
class AgBitmapTextureManager(
    val ag: AG
) {
	private val referencedBitmapsSinceGC = LinkedHashSet<Bitmap>()
	private var referencedBitmaps = ArrayList<Bitmap>()

    /** Number of frames between each Texture Garbage Collection step */
    var framesBetweenGC = 60
    //var framesBetweenGC = 360

	//var Bitmap._textureBase: Texture.Base? by Extra.Property { null }
	//var Bitmap._slices by Extra.Property { LinkedHashSet<BmpSlice>() }
	//var BmpSlice._texture: Texture? by Extra.Property { null }

    /** Wrapper of [Texture.Base] that contains all the [Texture] slices referenced as [BmpSlice] in our current cache */
	private class BitmapTextureInfo {
        var textureBase: Texture.Base? = null
		val slices = FastIdentityMap<BmpSlice, Texture>()
        fun reset() {
            textureBase = null
            slices.clear()
        }
	}

    private val textureInfoPool = Pool(reset = { it.reset() }) { BitmapTextureInfo() }
	private val bitmapsToTextureBase = FastIdentityMap<Bitmap, BitmapTextureInfo>()

	private var cachedBitmap: Bitmap? = null
	private var cachedBitmapTextureInfo: BitmapTextureInfo? = null

	private var cachedBmpSlice: BmpSlice? = null
	private var cachedBmpSliceTexture: Texture? = null

    /**
     * Obtains a temporal [BitmapTextureInfo] from a [Bitmap].
     *
     * The [BitmapTextureInfo] is a wrapper of [Bitmap] including a [Texture.Base] and information about slices of that [Bitmap]
     * that is just kept temporarily until released.
     *
     * You shouldn't call this method directly. Use [getTexture] or [getTextureBase] instead.
     */
	private fun getTextureInfo(bitmap: Bitmap): BitmapTextureInfo {
		if (cachedBitmap == bitmap) return cachedBitmapTextureInfo!!
        referencedBitmapsSinceGC += bitmap

		val textureInfo = bitmapsToTextureBase.getOrPut(bitmap) {
            textureInfoPool.alloc().also {
                // @TODO: MAke Texture.Base mutable
                it.textureBase = Texture.Base(ag.createTexture(bitmap, bitmap.texMipmaps, bitmap.premultiplied), bitmap.width, bitmap.height)
            }
        }

		cachedBitmap = bitmap
		cachedBitmapTextureInfo = textureInfo

		return textureInfo
	}

    /** Obtains a temporal [Texture.Base] from [bitmap] [Bitmap]. The texture shouldn't be stored, but used for drawing since it will be destroyed once not used anymore. */
	fun getTextureBase(bitmap: Bitmap): Texture.Base = getTextureInfo(bitmap).textureBase!!

    /** Obtains a temporal [Texture] from [slice] [BmpSlice]. The texture shouldn't be stored, but used for drawing since it will be destroyed once not used anymore. */
	fun getTexture(slice: BmpSlice): Texture {
		if (cachedBmpSlice == slice) return cachedBmpSliceTexture!!
        referencedBitmapsSinceGC += slice.bmp

        val info = getTextureInfo(slice.bmp)

		val texture = info.slices.getOrPut(slice) {
            Texture(info.textureBase!!).slice(Rectangle(slice.left, slice.top, slice.width, slice.height))
        }

		cachedBmpSlice = slice
		cachedBmpSliceTexture = texture

		return texture
	}

	private var fcount = 0

    /**
     * Called automatically by the engine after the render has been executed (each frame). It executes a texture GC every [framesBetweenGC] frames.
     */
    internal fun afterRender() {
        cachedBitmap = null
        cachedBmpSlice = null
		fcount++
		if (fcount >= framesBetweenGC) {
			fcount = 0
			gc()
		}
		// Prevent leaks when not referenced anymore
		removeCache()
	}

    /** Performs a kind of Garbage Collection of textures references since the last GC. This method is automatically executed every [framesBetweenGC] frames. */
	internal fun gc() {
        //println("AgBitmapTextureManager.gc[${referencedBitmaps.size}] - [${referencedBitmapsSinceGC.size}]")
        referencedBitmaps.fastForEach { bmp ->
            if (bmp !in referencedBitmapsSinceGC) {
                removeBitmap(bmp)
            }
        }
        referencedBitmaps.clear()
        referencedBitmaps.addAll(referencedBitmapsSinceGC)
        referencedBitmapsSinceGC.clear()
	}

    @KorgeExperimental
    fun removeBitmap(bmp: Bitmap) {
        //println("removeBitmap:${bmp.size}")
        val info = bitmapsToTextureBase.getAndRemove(bmp) ?: return
        referencedBitmapsSinceGC -= bmp
        if (cachedBitmapTextureInfo == info) removeCache()
        info.textureBase?.close()
        textureInfoPool.free(info)
    }

    private fun removeCache() {
        cachedBitmap = null
        cachedBitmapTextureInfo = null
        cachedBmpSlice = null
        cachedBmpSliceTexture = null
    }
}

private fun <K, V> FastIdentityMap<K, V>.getAndRemove(key: K): V? = get(key).also { remove(key) }
