package com.soywiz.korag

import com.soywiz.kds.iterators.*
import com.soywiz.klock.*
import com.soywiz.klogger.*
import com.soywiz.kmem.*
import com.soywiz.kmem.unit.*
import com.soywiz.korag.gl.*
import com.soywiz.korag.shader.*
import com.soywiz.korag.shader.gl.*
import com.soywiz.korim.bitmap.*
import com.soywiz.korio.lang.*
import kotlin.coroutines.*

internal interface AGNativeObject {
    fun markToDelete()
}

open class AGObject : Closeable {
    internal var _native: AGNativeObject? = null
    internal var _cachedContextVersion: Int = -1
    internal var _cachedVersion: Int = -2
    internal var _version: Int = -1

    protected fun markAsDirty() {
        _version++
    }

    override fun close() {
        _native?.markToDelete()
        _native = null
    }
}

open class AGBuffer constructor(val ag: AG, val list: AGList) : AGObject() {
    var estimatedMemoryUsage: ByteUnits = ByteUnits.fromBytes(0)
    var dirty: Boolean = true
    internal var mem: Buffer? = null

    init {
        ag.buffers += this
    }

    override fun close() {
        super.close()
        ag.buffers -= this
    }

    open fun afterSetMem() {
        estimatedMemoryUsage = ByteUnits.fromBytes(mem?.sizeInBytes ?: 0)
    }

    fun upload(data: ByteArray, offset: Int = 0, length: Int = data.size - offset): AGBuffer = upload(Int8Buffer(data, offset, length).buffer)
    fun upload(data: FloatArray, offset: Int = 0, length: Int = data.size - offset): AGBuffer = upload(Float32Buffer(data, offset, length).buffer)
    fun upload(data: IntArray, offset: Int = 0, length: Int = data.size - offset): AGBuffer = upload(Int32Buffer(data, offset, length).buffer)
    fun upload(data: ShortArray, offset: Int = 0, length: Int = data.size - offset): AGBuffer = upload(Int16Buffer(data, offset, length).buffer)
    fun upload(data: Buffer, offset: Int, length: Int = data.size - offset): AGBuffer = upload(data.sliceWithSize(offset, length))
    fun upload(data: Buffer): AGBuffer {
        mem = data.clone()
        afterSetMem()
        markAsDirty()
        return this
    }
}

class AGProgram(val ag: AG, val program: Program, val programConfig: ProgramConfig) : AGObject() {
    var cachedVersion = -1
    var programId = 0

    fun ensure(list: AGList) {
        if (cachedVersion != ag.contextVersion) {
            val time = measureTime {
                ag.programCount++
                programId = list.createProgram(program, programConfig)
                cachedVersion = ag.contextVersion
            }
            if (GlslGenerator.DEBUG_GLSL) {
                Console.info("AG: Created program ${program.name} with id ${programId} in time=$time")
            }
        }
    }

    fun use(list: AGList) {
        ensure(list)
        list.useProgram(programId)
    }

    fun unuse(list: AGList) {
        ensure(list)
        list.useProgram(0)
    }

    fun close(list: AGList) {
        if (programId != 0) {
            ag.programCount--
            list.deleteProgram(programId)
        }
        programId = 0
    }
}

data class AGTextureUnit constructor(
    val index: Int,
    var texture: AGTexture? = null,
    var linear: Boolean = true,
    var trilinear: Boolean? = null,
) {
    fun set(texture: AGTexture?, linear: Boolean, trilinear: Boolean? = null) {
        this.texture = texture
        this.linear = linear
        this.trilinear = trilinear
    }
}

// @TODO: Move most of this to AGQueueProcessorOpenGL, avoid cyclic dependency and simplify
open class AGTexture constructor(
    val ag: AG,
    open val premultiplied: Boolean,
    val targetKind: AGTextureTargetKind = AGTextureTargetKind.TEXTURE_2D
) : AGObject(), Closeable {
    var isFbo: Boolean = false
    var requestMipmaps: Boolean = false

    /** [MultiBitmap] for multiple bitmaps (ie. cube map) */
    var bitmap: Bitmap? = null
    var mipmaps: Boolean = false; internal set
    var cachedVersion = ag.contextVersion
    var forcedTexId: ForcedTexId? = null
    val implForcedTexId: Int get() = forcedTexId?.forcedTexId ?: -1
    val implForcedTexTarget: AGTextureTargetKind get() = forcedTexId?.forcedTexTarget?.let { AGTextureTargetKind.fromGl(it) } ?: targetKind
    var estimatedMemoryUsage: ByteUnits = ByteUnits.fromBytes(0L)
    val width: Int get() = bitmap?.width ?: 0
    val height: Int get() = bitmap?.height ?: 0
    val depth: Int get() = (bitmap as? MultiBitmap?)?.bitmaps?.size ?: 1

    init {
        ag.createdTextureCount++
        ag.textures += this
    }

    override fun close() {
        super.close()
        ag.textures -= this
    }

    private fun checkBitmaps(bmp: Bitmap) {
        if (!bmp.premultiplied) {
            Console.error("Trying to upload a non-premultiplied bitmap: $bmp. This will cause rendering artifacts")
        }
    }

    fun upload(list: List<Bitmap>, width: Int, height: Int): AGTexture {
        list.fastForEach { checkBitmaps(it) }
        return upload(MultiBitmap(width, height, list))
    }

    fun upload(bmp: Bitmap?, mipmaps: Boolean = false): AGTexture {
        bmp?.let { checkBitmaps(it) }
        this.forcedTexId = (bmp as? ForcedTexId?)
        this.bitmap = bmp
        estimatedMemoryUsage = ByteUnits.fromBytes(width * height * depth * 4)
        uploadedSource()
        markAsDirty()
        this.requestMipmaps = mipmaps
        return this
    }

    fun upload(bmp: BitmapSlice<Bitmap>?, mipmaps: Boolean = false): AGTexture {
        // @TODO: Optimize to avoid copying?
        return upload(bmp?.extract(), mipmaps)
    }

    protected open fun uploadedSource() {
    }

    fun doMipmaps(bitmap: Bitmap?, requestMipmaps: Boolean): Boolean {
        val width = bitmap?.width ?: 0
        val height = bitmap?.height ?: 0
        return requestMipmaps && width.isPowerOfTwo && height.isPowerOfTwo
    }

    override fun toString(): String = "AGTexture(pre=$premultiplied)"
}
