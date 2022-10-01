package com.soywiz.korge.render

import com.soywiz.kds.FastArrayList
import com.soywiz.kds.FastIdentityMap
import com.soywiz.kds.Pool
import com.soywiz.kds.clear
import com.soywiz.kds.getAndRemove
import com.soywiz.kds.getOrPut
import com.soywiz.korag.AG
import com.soywiz.korge.annotations.KorgeExperimental
import com.soywiz.korge.internal.KorgeInternal
import com.soywiz.korim.bitmap.Bitmap
import com.soywiz.korim.bitmap.BitmapCoords
import com.soywiz.korim.bitmap.BmpCoordsWithInstance
import com.soywiz.korim.bitmap.BmpSlice
import com.soywiz.korim.bitmap.MultiBitmap
import com.soywiz.korio.lang.currentThreadId
import com.soywiz.korma.geom.Rectangle

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
// @TODO: Use [com.soywiz.kds.FastIdentityCacheMap]
@OptIn(KorgeInternal::class, KorgeExperimental::class)
class AgBitmapTextureManager(
    val ag: AG
) {
	private val referencedBitmapsSinceGC = AgFastSet<Bitmap>()
	private var referencedBitmaps = FastArrayList<Bitmap>()

    /** Number of frames between each Texture Garbage Collection step */
    var framesBetweenGC = 60
    //var framesBetweenGC = 30 * 60 // 30 seconds
    //var framesBetweenGC = 360

	//var Bitmap._textureBase: TextureBase? by Extra.Property { null }
	//var Bitmap._slices by Extra.Property { LinkedHashSet<BmpCoordsWithBitmap>() }
	//var BmpCoordsWithBitmap._texture: Texture? by Extra.Property { null }

    /** Wrapper of [TextureBase] that contains all the [TextureCoords] slices referenced as [BitmapCoords] in our current cache */
	private class BitmapTextureInfo {
        var textureBase: TextureBase = TextureBase(null, 0, 0)
		val slices = FastIdentityMap<BitmapCoords, TextureCoords>()
        fun reset() {
            textureBase.base = null
            textureBase.version = -1
            textureBase.width = 0
            textureBase.height = 0
            slices.clear()
        }
	}

    private val textureInfoPool = Pool(reset = { it.reset() }) { BitmapTextureInfo() }
	private val bitmapsToTextureBase = FastIdentityMap<Bitmap, BitmapTextureInfo>()

	private var cachedBitmap: Bitmap? = null
	private var cachedBitmapTextureInfo: BitmapTextureInfo? = null

    private var cachedBitmap2: Bitmap? = null
    private var cachedBitmapTextureInfo2: BitmapTextureInfo? = null

    private var cachedBmpSlice: BitmapCoords? = null
	private var cachedBmpSliceTexture: TextureCoords? = null

    private var cachedBmpSlice2: BitmapCoords? = null
    private var cachedBmpSliceTexture2: TextureCoords? = null

    /**
     * Obtains a temporal [BitmapTextureInfo] from a [Bitmap].
     *
     * The [BitmapTextureInfo] is a wrapper of [Bitmap] including a [TextureBase] and information about slices of that [Bitmap]
     * that is just kept temporarily until released.
     *
     * You shouldn't call this method directly. Use [getTexture] or [getTextureBase] instead.
     */
	private fun getTextureInfo(bitmap: Bitmap): BitmapTextureInfo {
		if (cachedBitmap === bitmap && cachedBitmapTextureInfo!!.textureBase.version == bitmap.contentVersion) return cachedBitmapTextureInfo!!
        if (cachedBitmap2 === bitmap && cachedBitmapTextureInfo2!!.textureBase.version == bitmap.contentVersion) return cachedBitmapTextureInfo2!!
        referencedBitmapsSinceGC.add(bitmap)

		val textureInfo = bitmapsToTextureBase.getOrPut(bitmap) {
            textureInfoPool.alloc().also {
                val base = it.textureBase
                base.version = -1
                base.base = ag.createTexture(bitmap.premultiplied, targetKind = when (bitmap) {
                    is MultiBitmap -> AG.TextureTargetKind.TEXTURE_CUBE_MAP
                    else -> AG.TextureTargetKind.TEXTURE_2D
                })
                base.width = bitmap.width
                base.height = bitmap.height
            }
        }

        cachedBitmap2 = cachedBitmap
        cachedBitmapTextureInfo2 = cachedBitmapTextureInfo

        val base = textureInfo.textureBase
		cachedBitmap = bitmap
		cachedBitmapTextureInfo = textureInfo
        if (bitmap.contentVersion != base.version) {
            base.version = bitmap.contentVersion
            // @TODO: Use dirtyRegion to upload only a fragment of the image
            base.update(bitmap, bitmap.mipmaps)
            bitmap.clearDirtyRegion()
        }

		return textureInfo
	}

    /** Obtains a temporal [TextureBase] from [bitmap] [Bitmap]. The texture shouldn't be stored, but used for drawing since it will be destroyed once not used anymore. */
	fun getTextureBase(bitmap: Bitmap): TextureBase = getTextureInfo(bitmap).textureBase!!

    fun getTexture(slice: BmpSlice): Texture = _getTexture(slice) as Texture
    fun getTexture(slice: BitmapCoords): TextureCoords = _getTexture(slice)

    /** Obtains a temporal [Texture] from [slice] [BmpSlice]. The texture shouldn't be stored, but used for drawing since it will be destroyed once not used anymore. */
	private fun _getTexture(slice: BitmapCoords): TextureCoords {
		if (cachedBmpSlice === slice) return cachedBmpSliceTexture!!
        if (cachedBmpSlice2 === slice) return cachedBmpSliceTexture2!!

        val info = getTextureInfo(slice.base)

		val texture = info.slices.getOrPut(slice) {
            if (slice is BmpSlice) {
                Texture(info.textureBase).slice(Rectangle(slice.left, slice.top, slice.width, slice.height))
            } else {
                BmpCoordsWithInstance(info.textureBase, slice)
            }
        }

        cachedBmpSlice2 = cachedBmpSlice
        cachedBmpSliceTexture2 = cachedBmpSliceTexture

		cachedBmpSlice = slice
		cachedBmpSliceTexture = texture

		return texture
	}

	private var fcount = 0

    /**
     * Called automatically by the engine after the render has been executed (each frame). It executes a texture GC every [framesBetweenGC] frames.
     */
    internal fun afterRender() {
        // Prevent leaks when not referenced anymore
        removeCache()

		fcount++
		if (fcount >= framesBetweenGC) {
			fcount = 0
			gc()
		}
	}

    /** Performs a kind of Garbage Collection of textures references since the last GC. This method is automatically executed every [framesBetweenGC] frames. */
	internal fun gc() {
        //println("AgBitmapTextureManager.gc[${referencedBitmaps.size}] - [${referencedBitmapsSinceGC.size}]")
        referencedBitmaps.fastForEach { bmp ->
            if (bmp !in referencedBitmapsSinceGC) {
                removeBitmap(bmp, "GC")
            }
        }
        referencedBitmaps.clear()
        referencedBitmaps.addAll(referencedBitmapsSinceGC.items)
        referencedBitmapsSinceGC.clear()
	}

    @KorgeExperimental
    fun removeBitmap(bmp: Bitmap, reason: String) {
        val info = bitmapsToTextureBase.getAndRemove(bmp) ?: return
        referencedBitmapsSinceGC.remove(bmp)
        if (cachedBitmapTextureInfo === info || cachedBitmapTextureInfo2 === info) removeCache()
        info.textureBase.close()
        textureInfoPool.free(info)
        //println("AgBitmapTextureManager.removeBitmap[$currentThreadId]:${bmp.size}, reason=$reason, textureInfoPool=${textureInfoPool.itemsInPool},${textureInfoPool.totalAllocatedItems}")
    }

    private fun removeCache() {
        cachedBitmap = null
        cachedBitmapTextureInfo = null
        cachedBitmap2 = null
        cachedBitmapTextureInfo2 = null

        cachedBmpSlice = null
        cachedBmpSliceTexture = null
        cachedBmpSlice2 = null
        cachedBmpSliceTexture2 = null
    }
}

