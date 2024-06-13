package korlibs.korge.render

import korlibs.datastructure.*
import korlibs.graphics.*
import korlibs.korge.annotations.KorgeExperimental
import korlibs.korge.internal.KorgeInternal
import korlibs.image.bitmap.*
import korlibs.io.lang.*
import korlibs.math.geom.*
import korlibs.math.geom.slice.*
import korlibs.memory.unit.*

/**
 * Class in charge of automatically handling [AGTexture] <-> [Bitmap] conversion.
 *
 * To simplify texture storage (which usually require uploading to the GPU, and releasing it once not used),
 * the [AgBitmapTextureManager] allows to get temporal textures that are available
 * while referenced in the previous [framesBetweenGC] frames, typically 60.
 *
 * If it has been [framesBetweenGC] frames without being referenced, the textures are collected.
 * This greatly simplifies texture management, but might have an impact in performance.
 * If you want to keep a Bitmap or an atlas in the GPU so there is no impact on uploading when required,
 * you can just call any of the getTexture* methods here each frame, even if not using it in the current frame.
 *
 * You can also manage [Texture] manually, but you should release the textures manually
 * by calling [Texture.close] so the resources are freed.
 *
 * If [maxCachedMemory] is not 0L, that value will be used to keep some textures in the cache
 * even if they were not referenced in the past frames since GC.
 */
// @TODO: Use [korlibs.datastructure.FastIdentityCacheMap]
@OptIn(KorgeInternal::class, KorgeExperimental::class)
class AgBitmapTextureManager(
    val ag: AG
) : AutoCloseable {
    var maxCachedMemory = 0L

    /** Bitmaps to keep for some time even if not referenced in [framesBetweenGC] as long as the [maxCachedMemory] allows it */
    private val cachedBitmaps = FastSmallSet<Bitmap>()
	private val referencedBitmapsSinceGC = FastSmallSet<Bitmap>()
	private var referencedBitmaps = FastArrayList<Bitmap>()

    val numCachedBitmaps: Int get() = cachedBitmaps.size
    val numReferencedBitmapsSinceGC: Int get() = referencedBitmapsSinceGC.size
    val numReferencedBitmaps: Int get() = referencedBitmaps.size

    override fun toString(): String = toStringStats()

    fun toStringStats(): String =
        "AgBitmapTextureManager(numCachedBitmaps=$numCachedBitmaps, numReferencedBitmapsSinceGC=$numReferencedBitmapsSinceGC, numReferencedBitmaps=$numReferencedBitmaps, maxCachedMemory=${ByteUnits.fromBytes(maxCachedMemory)}, managedTextureMemory=${ByteUnits.fromBytes(managedTextureMemory)})"

    /** Number of frames between each Texture Garbage Collection step */
    var framesBetweenGC: Int = 60
    var managedTextureMemory: Long = 0L
        private set (value) {
            //if (field >= 0L && value < 0L) printStackTrace("OLD managedTextureMemory=$field -> $value")
            field = value
        }
    //var framesBetweenGC = 30 * 60 // 30 seconds
    //var framesBetweenGC = 360

	//var Bitmap._textureBase: TextureBase? by Extra.Property { null }
	//var Bitmap._slices by Extra.Property { LinkedHashSet<BmpCoordsWithBitmap>() }
	//var BmpCoordsWithBitmap._texture: Texture? by Extra.Property { null }

    /** Wrapper of [TextureBase] that contains all the [TextureCoords] slices referenced as [BitmapCoords] in our current cache */
	internal class BitmapTextureInfo {
        var usedMemory: Int = 0
        var textureBase: TextureBase = TextureBase(null, 0, 0)
		val slices = FastIdentityMap<BitmapCoords, TextureCoords>()
        fun reset() {
            usedMemory = 0
            textureBase.base = null
            textureBase.version = -1
            textureBase.width = 0
            textureBase.height = 0
            slices.clear()
        }

        override fun toString(): String = "BitmapTextureInfo(textureBase=$textureBase, usedMemory=$usedMemory, slices=${slices.size})"
    }

    private val textureInfoPool = Pool(reset = { it.reset() }) { BitmapTextureInfo() }
	private val bitmapsToTextureBase = FastIdentityMap<Bitmap, BitmapTextureInfo>()

    internal fun getBitmapsWithTextureInfoCopy(): Map<Bitmap, BitmapTextureInfo> = bitmapsToTextureBase.toMap()

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
                //println("ALLOC TEXTURE_INFO $it")
                val base = it.textureBase
                base.version = -1
                base.base = AGTexture(targetKind = when (bitmap) {
                    is MultiBitmap -> AGTextureTargetKind.TEXTURE_CUBE_MAP
                    else -> AGTextureTargetKind.TEXTURE_2D
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
            //println("OLD VERSION: textureInfo.usedMemory=${textureInfo.usedMemory}")
            managedTextureMemory -= textureInfo.usedMemory
            try {
                base.update(bitmap, bitmap.mipmaps, bitmap.baseMipmapLevel, bitmap.maxMipmapLevel)
            } finally {
                textureInfo.usedMemory = bitmap.estimateSizeInBytes
                managedTextureMemory += textureInfo.usedMemory
            }
            bitmap.clearDirtyRegion()
        }

		return textureInfo
	}

    private val Bitmap.estimateSizeInBytes: Int get() = height * (width * bpp / 8)

    /** Obtains a temporal [TextureBase] from [bitmap] [Bitmap]. The texture shouldn't be stored, but used for drawing since it will be destroyed once not used anymore. */
	fun getTextureBase(bitmap: Bitmap): TextureBase = getTextureInfo(bitmap).textureBase!!

    fun getTexture(slice: BmpSlice): Texture = _getTexture(slice) as Texture
    fun getTexture(slice: BitmapCoords): TextureCoords = _getTexture(slice)

    /** Obtains a temporal [Texture] from [slice] [BmpSlice]. The texture shouldn't be stored, but used for drawing since it will be destroyed once not used anymore. */
	private fun _getTexture(slice: BitmapCoords): TextureCoords {
		if (cachedBmpSlice === slice) return cachedBmpSliceTexture!!
        if (cachedBmpSlice2 === slice) return cachedBmpSliceTexture2!!

        val info = getTextureInfo(slice.base)

		val texture: TextureCoords = info.slices.getOrPut(slice) {
            if (slice is BmpSlice) {
                Texture(info.textureBase).slice(RectangleInt(slice.left, slice.top, slice.width, slice.height))
            } else {
                TextureCoords(info.textureBase, slice)
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
        clearFastCacheAccess()

		fcount++
		if (fcount >= framesBetweenGC) {
			fcount = 0
			gc()
		}
	}

    /**
     * Performs a kind of Garbage Collection of textures references since the last GC.
     * This method is automatically executed every [framesBetweenGC] frames.
     **/
	internal fun gc() {
        //println("AgBitmapTextureManager.gc[${referencedBitmaps.size}] - [${referencedBitmapsSinceGC.size}]")
        referencedBitmaps.fastForEach { bmp ->
            when {
                bmp in referencedBitmapsSinceGC -> Unit // Keep normally
                maxCachedMemory < this.managedTextureMemory || managedTextureMemory < 0L -> removeBitmap(bmp, "GC")
                else -> cachedBitmaps.add(bmp)
            }
        }
        referencedBitmaps.clear()
        //println("GC: $referencedBitmapsSinceGC")
        referencedBitmapsSinceGC.fastForEach { referencedBitmaps.add(it) }
        cachedBitmaps.fastForEach { referencedBitmaps.add(it) }
        referencedBitmapsSinceGC.clear()
	}

    @KorgeExperimental
    fun removeBitmap(bmp: Bitmap, reason: String) {
        val info = bitmapsToTextureBase.getAndRemove(bmp) ?: return
        managedTextureMemory -= info.usedMemory
        referencedBitmapsSinceGC.remove(bmp)
        if (cachedBitmapTextureInfo === info || cachedBitmapTextureInfo2 === info) clearFastCacheAccess()
        info.textureBase.close()
        textureInfoPool.free(info)
        cachedBitmaps.remove(bmp)
        //println("AgBitmapTextureManager.removeBitmap[$currentThreadId]:${bmp.size}, reason=$reason, textureInfoPool=${textureInfoPool.itemsInPool},${textureInfoPool.totalAllocatedItems}")
    }

    private fun clearFastCacheAccess() {
        cachedBitmap = null
        cachedBitmapTextureInfo = null
        cachedBitmap2 = null
        cachedBitmapTextureInfo2 = null

        cachedBmpSlice = null
        cachedBmpSliceTexture = null
        cachedBmpSlice2 = null
        cachedBmpSliceTexture2 = null
    }
    
    override fun close() {
        clearFastCacheAccess()
        for (bmp in bitmapsToTextureBase.keys.toList()) removeBitmap(bmp, "close")
        bitmapsToTextureBase.clear()
        referencedBitmaps.clear()
        referencedBitmapsSinceGC.clear()
        cachedBitmaps.clear()
        textureInfoPool.clear()
    }
}
