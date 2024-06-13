@file:OptIn(ExperimentalUnsignedTypes::class)

package korlibs.graphics

import korlibs.graphics.gl.*
import korlibs.graphics.shader.*
import korlibs.image.bitmap.*
import korlibs.io.lang.*
import korlibs.logger.*
import korlibs.math.*
import korlibs.math.geom.*
import korlibs.memory.*
import korlibs.memory.unit.*

internal interface AGNativeObject {
    fun markToDelete()
}

open class AGObject : AutoCloseable {
    internal var _native: AGNativeObject? = null
    internal var _cachedContextVersion: Int = -1
    internal var _cachedVersion: Int = -2
    internal var _version: Int = -1

    internal fun _resetVersion() {
        //_version = RESET_VERSION
        markAsDirty()
        _cachedVersion = _version - 1
    }

    protected fun markAsDirty() {
        _version++
    }

    override fun close() {
        _native?.markToDelete()
        _native = null
    }
}

internal inline fun <T : AGObject, R: AGNativeObject> T.createOnce(ag: AG, block: (T) -> R): R {
    if (this._native == null || this._cachedContextVersion != ag.contextVersion) {
        this._cachedContextVersion = ag.contextVersion
        this._resetVersion()
        this._native = block(this)
    }
    return this._native as R
}

class AGBuffer : AGObject() {
    var mem: Buffer? = null
        private set

    // @TODO: Allow upload range in addition to the full buffer.
    // @TODO: This will allow to upload chunks of uniform buffers for example.
    // glBufferData & glBufferSubData
    fun upload(data: ByteArray, offset: Int = 0, length: Int = data.size - offset): AGBuffer = upload(Int8Buffer(data, offset, length).buffer)
    fun upload(data: UByteArray): AGBuffer = upload(Uint8Buffer(data).buffer)
    fun upload(data: FloatArray, offset: Int = 0, length: Int = data.size - offset): AGBuffer = upload(Float32Buffer(data, offset, length).buffer)
    fun upload(data: IntArray, offset: Int = 0, length: Int = data.size - offset): AGBuffer = upload(Int32Buffer(data, offset, length).buffer)
    fun upload(data: ShortArray, offset: Int = 0, length: Int = data.size - offset): AGBuffer = upload(Int16Buffer(data, offset, length).buffer)
    fun upload(data: Buffer, offset: Int, length: Int = data.size - offset): AGBuffer = upload(data.sliceWithSize(offset, length))
    fun upload(data: Buffer): AGBuffer {
        //println(data.sizeInBytes)
        // Only check small buffers
        if (data.sizeInBytes < 1024) {
            if (this.mem != null && this.mem!!.sizeInBytes == data.sizeInBytes && arrayequal(this.mem!!, 0, data, 0, data.sizeInBytes)) return this
        }
        //println("New Data!")
        mem = data.clone()
        markAsDirty()
        return this
    }

    val sizeInBytes: Int
        get() = mem?.sizeInBytes ?: 0

    //private val id = LAST_ID.incrementAndGet()
    //companion object { private val LAST_ID = KorAtomicInt(0) }
    // init { printStackTrace() }

    override fun toString(): String = "AGBuffer(${mem?.sizeInBytes ?: 0})"

    //override fun toString(): String = "AGBuffer[$id](${mem?.sizeInBytes ?: 0})"
}

data class AGTextureUnits(val textures: Array<AGTexture?>, val infos: AGTextureUnitInfoArray) {
    companion object {
        val MAX_TEXTURE_UNITS = 8 // iOS only support 8 texture units on OpenGL
        val EMPTY get() = AGTextureUnits()
    }
    constructor(size: Int = MAX_TEXTURE_UNITS) : this(arrayOfNulls(size), AGTextureUnitInfoArray(size))
    val size: Int get() = textures.size

    fun copyFrom(other: AGTextureUnits, sampler: Sampler) = copyFrom(other, sampler.index)
    fun copyFrom(other: AGTextureUnits, index: Int) = set(index, other.textures[index], other.infos[index])

    fun set(index: Int, texture: AGTexture?, info: AGTextureUnitInfo = AGTextureUnitInfo.DEFAULT) {
        textures[index] = texture
        infos[index] = info
    }
    fun set(sampler: Sampler, texture: AGTexture?, info: AGTextureUnitInfo = AGTextureUnitInfo.DEFAULT) {
        set(sampler.index, texture, info)
    }
    fun clear() {
        for (n in 0 until size) set(n, null, AGTextureUnitInfo.DEFAULT)
    }
    fun clone(): AGTextureUnits = AGTextureUnits(textures.copyOf(), infos.copyOf())

    inline fun fastForEach(block: (index: Int, tex: AGTexture?, info: AGTextureUnitInfo) -> Unit) {
        for (n in 0 until size) {
            block(n, textures[n], infos[n])
        }
    }
}

inline class AGTextureUnitInfoArray(val data: IntArray) {
    constructor(size: Int) : this(IntArray(size) { AGTextureUnitInfo.DEFAULT.data })
    operator fun set(index: Int, value: AGTextureUnitInfo) { data[index] = value.data }
    operator fun get(index: Int): AGTextureUnitInfo = AGTextureUnitInfo.fromRaw(data[index])
    fun copyOf() = AGTextureUnitInfoArray(data.copyOf())
}
inline class AGTextureUnitInfo private constructor(val data: Int) {

    companion object {
        val INVALID = AGTextureUnitInfo(-1)
        val DEFAULT = AGTextureUnitInfo(0)
            .withLinearTrilinear(true, true)
            .withWrap(AGWrapMode.CLAMP_TO_EDGE)
            .withKind(AGTextureTargetKind.TEXTURE_2D)

        fun fromRaw(data: Int): AGTextureUnitInfo = AGTextureUnitInfo(data)

        operator fun invoke(
            wrap: AGWrapMode = AGWrapMode.CLAMP_TO_EDGE,
            linear: Boolean = true,
            trilinear: Boolean = linear
        ): AGTextureUnitInfo = AGTextureUnitInfo(0).withWrap(wrap).withLinearTrilinear(linear, trilinear)
    }
    val wrap: AGWrapMode get() = AGWrapMode(data.extract2(0))
    val linear: Boolean get() = data.extract(2)
    val trilinear: Boolean get() = data.extract(3)
    val kind: AGTextureTargetKind get() = AGTextureTargetKind(data.extract(8, 5))

    fun withKind(kind: AGTextureTargetKind): AGTextureUnitInfo = AGTextureUnitInfo(data.insert5(kind.ordinal, 8))
    fun withWrap(wrap: AGWrapMode): AGTextureUnitInfo = AGTextureUnitInfo(data.insert2(wrap.ordinal, 0))
    fun withLinear(linear: Boolean): AGTextureUnitInfo = AGTextureUnitInfo(data.insert(linear, 2))
    fun withTrilinear(trilinear: Boolean): AGTextureUnitInfo = AGTextureUnitInfo(data.insert(trilinear, 3))
    fun withLinearTrilinear(linear: Boolean, trilinear: Boolean): AGTextureUnitInfo = withLinear(linear).withTrilinear(trilinear)

    override fun toString(): String = "AGTextureUnitInfo(wrap=$wrap, linear=$linear, trilinear=$trilinear, kind=$kind)"
}

class AGTexture(
    val targetKind: AGTextureTargetKind = AGTextureTargetKind.TEXTURE_2D
) : AGObject(), AutoCloseable {
    private val logger = Logger("AGTexture")
    var isFbo: Boolean = false
    var requestMipmaps: Boolean = false
    var baseMipmapLevel: Int? = null
    var maxMipmapLevel: Int? = null

    /** [MultiBitmap] for multiple bitmaps (ie. cube map) */
    var bitmap: Bitmap? = null
    var mipmaps: Boolean = false; internal set
    var forcedTexId: ForcedTexId? = null
    val implForcedTexId: Int get() = forcedTexId?.forcedTexId ?: -1
    val implForcedTexTarget: AGTextureTargetKind get() = forcedTexId?.forcedTexTarget?.let { AGTextureTargetKind.fromGl(it) } ?: targetKind
    var estimatedMemoryUsage: ByteUnits = ByteUnits.fromBytes(0L)
    val width: Int get() = bitmap?.width ?: 0
    val height: Int get() = bitmap?.height ?: 0
    val depth: Int get() = (bitmap as? MultiBitmap?)?.bitmaps?.size ?: 1

    private fun checkBitmaps(bmp: Bitmap) {
        if (!bmp.premultiplied) {
            logger.error { "Trying to upload a non-premultiplied bitmap: $bmp. This will cause rendering artifacts" }
        }
    }

    fun upload(bmp: Bitmap?, mipmaps: Boolean = false, baseMipmapLevel: Int? = null, maxMipmapLevel: Int? = null): AGTexture {
        bmp?.let { checkBitmaps(it) }
        this.forcedTexId = (bmp as? ForcedTexId?)
        this.bitmap = bmp
        estimatedMemoryUsage = ByteUnits.fromBytes(width * height * depth * 4)
        markAsDirty()
        this.requestMipmaps = mipmaps
        this.baseMipmapLevel = baseMipmapLevel
        this.maxMipmapLevel = maxMipmapLevel
        return this
    }

    fun upload(bmp: BmpSlice?, mipmaps: Boolean = false, baseMipmapLevel: Int? = null, maxMipmapLevel: Int? = null): AGTexture {
        // @TODO: Optimize to avoid copying?
        return upload(bmp?.extract(), mipmaps, baseMipmapLevel, maxMipmapLevel)
    }

    fun doMipmaps(bitmap: Bitmap?, requestMipmaps: Boolean): Boolean {
        val width = bitmap?.width ?: 0
        val height = bitmap?.height ?: 0
        return requestMipmaps && width.isPowerOfTwo && height.isPowerOfTwo
    }

    override fun toString(): String = "AGTexture(size=$width,$height)"
}

open class AGFrameBufferBase(val isMain: Boolean) : AGObject() {
    val isTexture: Boolean get() = !isMain
    val tex: AGTexture = AGTexture().also { it.isFbo = true }
    var estimatedMemoryUsage: ByteUnits = ByteUnits.fromBytes(0)

    override fun close() {
        tex.close()
        //ag.frameRenderBuffers -= this
    }

    override fun toString(): String = "AGFrameBufferBase(isMain=$isMain)"
}

open class AGFrameBuffer(val base: AGFrameBufferBase, val id: Int = -1) : AutoCloseable {
    constructor(isMain: Boolean = false, id: Int = -1) : this(AGFrameBufferBase(isMain), id)
    val isTexture: Boolean get() = base.isTexture
    val isMain: Boolean get() = base.isMain
    val tex: AGTexture get() = base.tex
    val info: AGFrameBufferInfo get() = AGFrameBufferInfo(0)
        .withBounds(x, y, width, height).withSamples(nsamples).withHasDepth(hasDepth).withHasStencil(hasStencil)
    companion object {
        const val DEFAULT_INITIAL_WIDTH = 128
        const val DEFAULT_INITIAL_HEIGHT = 128
    }

    var nsamples: Int = 1; protected set
    val hasStencilAndDepth: Boolean get() = hasDepth && hasStencil
    var hasStencil: Boolean = true; protected set
    var hasDepth: Boolean = true; protected set

    var x = 0
    var y = 0
    var width = DEFAULT_INITIAL_WIDTH
    var height = DEFAULT_INITIAL_HEIGHT
    var fullWidth = DEFAULT_INITIAL_WIDTH
    var fullHeight = DEFAULT_INITIAL_HEIGHT
    private var _scissor = RectangleInt()
    var scissor: RectangleInt? = null

    open fun setSize(width: Int, height: Int): Boolean {
        return setSize(0, 0, width, height)
    }

    open fun setSize(x: Int, y: Int, width: Int, height: Int, fullWidth: Int = width, fullHeight: Int = height): Boolean {
        if (this.x == x && this.y == y && this.width == width && this.height == height && this.fullWidth == fullWidth && this.fullHeight == fullHeight) return true
        tex.upload(NullBitmap(width, height))

        base.estimatedMemoryUsage = ByteUnits.fromBytes(fullWidth * fullHeight * (4 + 4))

        this.x = x
        this.y = y
        this.width = width
        this.height = height
        this.fullWidth = fullWidth
        this.fullHeight = fullHeight
        markAsDirty()

        return false
    }

    fun scissor(scissor: RectangleInt?) {
        this.scissor = scissor
    }

    override fun close() {
        base.close()
        //ag.frameRenderBuffers -= this
    }


    fun setSamples(samples: Int) {
        if (this.nsamples == samples) return
        nsamples = samples
        markAsDirty()
    }

    fun setExtra(hasDepth: Boolean = true, hasStencil: Boolean = true) {
        if (this.hasDepth == hasDepth && this.hasStencil == hasStencil) return
        this.hasDepth = hasDepth
        this.hasStencil = hasStencil
        markAsDirty()
    }

    private fun markAsDirty() {
        //base.markAsDirty()
    }

    override fun toString(): String = "AGFrameBuffer(${if (isMain) "main" else "$id"}, $width, $height)"
}
