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

    open fun afterSetMem() {
        estimatedMemoryUsage = ByteUnits.fromBytes(mem?.sizeInBytes ?: 0)
    }

    fun upload(data: ByteArray, offset: Int = 0, length: Int = data.size - offset): AGBuffer = upload(Int8Buffer(data, offset, length).buffer)
    fun upload(data: FloatArray, offset: Int = 0, length: Int = data.size - offset): AGBuffer = upload(Float32Buffer(data, offset, length).buffer)
    fun upload(data: IntArray, offset: Int = 0, length: Int = data.size - offset): AGBuffer = upload(Int32Buffer(data, offset, length).buffer)
    fun upload(data: ShortArray, offset: Int = 0, length: Int = data.size - offset): AGBuffer = upload(Int16Buffer(data, offset, length).buffer)
    fun upload(data: Buffer, offset: Int, length: Int = data.size - offset): AGBuffer = upload(data.sliceWithSize(offset, length))
    fun upload(data: Buffer): AGBuffer {
        mem = data
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
    var mipmaps: Boolean = false; internal set
    var source: AGBitmapSourceBase = AGSyncBitmapSource.NIL
    internal var uploaded: Boolean = false
    internal var generating: Boolean = false
    internal var generated: Boolean = false
    internal var tempBitmaps: List<Bitmap?>? = null
    var ready: Boolean = false; internal set

    var cachedVersion = ag.contextVersion
    var texId = ag.commandsNoWait { it.createTexture() }

    var forcedTexId: ForcedTexId? = null
    val implForcedTexId: Int get() = forcedTexId?.forcedTexId ?: -1
    val implForcedTexTarget: AGTextureTargetKind get() = forcedTexId?.forcedTexTarget?.let { AGTextureTargetKind.fromGl(it) } ?: targetKind

    init {
        ag.createdTextureCount++
        ag.textures += this
    }

    internal fun invalidate() {
        uploaded = false
        generating = false
        generated = false
    }

    private fun checkBitmaps(bmp: Bitmap) {
        if (!bmp.premultiplied) {
            Console.error("Trying to upload a non-premultiplied bitmap: $bmp. This will cause rendering artifacts")
        }
    }

    fun upload(list: List<Bitmap>, width: Int, height: Int): AGTexture {
        list.fastForEach { checkBitmaps(it) }
        return upload(AGSyncBitmapSourceList(rgba = true, width = width, height = height, depth = list.size) { list })
    }

    fun upload(bmp: Bitmap?, mipmaps: Boolean = false): AGTexture {
        bmp?.let { checkBitmaps(it) }
        this.forcedTexId = (bmp as? ForcedTexId?)
        return upload(
            if (bmp != null) AGSyncBitmapSource(
                rgba = bmp.bpp > 8,
                width = bmp.width,
                height = bmp.height
            ) { bmp } else AGSyncBitmapSource.NIL, mipmaps)
    }

    fun upload(bmp: BitmapSlice<Bitmap>?, mipmaps: Boolean = false): AGTexture {
        // @TODO: Optimize to avoid copying?
        return upload(bmp?.extract(), mipmaps)
    }

    var estimatedMemoryUsage: ByteUnits = ByteUnits.fromBytes(0L)

    fun upload(source: AGBitmapSourceBase, mipmaps: Boolean = false): AGTexture {
        this.source = source
        estimatedMemoryUsage = ByteUnits.fromBytes(source.width * source.height * source.depth * 4)
        uploadedSource()
        invalidate()
        this.requestMipmaps = mipmaps
        return this
    }

    protected open fun uploadedSource() {
    }

    fun uploadAndBindEnsuring(bmp: Bitmap?, mipmaps: Boolean = false): AGTexture = upload(bmp, mipmaps).bindEnsuring()
    fun uploadAndBindEnsuring(bmp: BitmapSlice<Bitmap>?, mipmaps: Boolean = false): AGTexture = upload(bmp, mipmaps).bindEnsuring()
    fun uploadAndBindEnsuring(source: AGBitmapSourceBase, mipmaps: Boolean = false): AGTexture = upload(source, mipmaps).bindEnsuring()

    fun doMipmaps(source: AGBitmapSourceBase, requestMipmaps: Boolean): Boolean {
        return requestMipmaps && source.width.isPowerOfTwo && source.height.isPowerOfTwo
    }

    open fun bind(): Unit = ag.commandsNoWait { it.bindTexture(texId, implForcedTexTarget, implForcedTexId) }
    open fun unbind(): Unit = ag.commandsNoWait { it.bindTexture(0, implForcedTexTarget) }

    private var closed = false
    override fun close() {
        if (!alreadyClosed) {
            alreadyClosed = true
            source = AGSyncBitmapSource.NIL
            tempBitmaps = null
            ag.deletedTextureCount++
            ag.textures -= this
            //Console.log("CLOSED TEXTURE: $texId")
            //printTexStats()
        }

        if (!closed) {
            closed = true
            if (cachedVersion == ag.contextVersion) {
                if (texId != 0) {
                    ag.commandsNoWait { it.deleteTexture(texId) }
                    texId = 0
                }
            } else {
                //println("YAY! NO DELETE texture because in new context and would remove the wrong texture: $texId")
            }
        } else {
            //println("ALREADY CLOSED TEXTURE: $texId")
        }
    }

    override fun toString(): String = "AGOpengl.GlTexture($texId, pre=$premultiplied)"
    fun manualUpload(): AGTexture {
        uploaded = true
        return this
    }

    fun bindEnsuring(): AGTexture {
        ag.commandsNoWait { it.bindTextureEnsuring(this) }
        return this
    }

    open fun actualSyncUpload(source: AGBitmapSourceBase, bmps: List<Bitmap?>?, requestMipmaps: Boolean) {
        //this.bind() // Already bound
        this.mipmaps = doMipmaps(source, requestMipmaps)
    }

    private var alreadyClosed = false

    private fun printTexStats() {
        //Console.log("create=$createdCount, delete=$deletedCount, alive=${createdCount - deletedCount}")
    }
}

interface AGBitmapSourceBase {
    val rgba: Boolean
    val width: Int
    val height: Int
    val depth: Int get() = 1
}

class AGSyncBitmapSourceList(
    override val rgba: Boolean,
    override val width: Int,
    override val height: Int,
    override val depth: Int,
    val gen: () -> List<Bitmap>?
) : AGBitmapSourceBase {
    companion object {
        val NIL = AGSyncBitmapSourceList(true, 0, 0, 0) { null }
    }

    override fun toString(): String = "SyncBitmapSourceList(rgba=$rgba, width=$width, height=$height)"
}

class AGSyncBitmapSource(
    override val rgba: Boolean,
    override val width: Int,
    override val height: Int,
    val gen: () -> Bitmap?
) : AGBitmapSourceBase {
    companion object {
        val NIL = AGSyncBitmapSource(true, 0, 0) { null }
    }

    override fun toString(): String = "SyncBitmapSource(rgba=$rgba, width=$width, height=$height)"
}

class AGAsyncBitmapSource(
    val coroutineContext: CoroutineContext,
    override val rgba: Boolean,
    override val width: Int,
    override val height: Int,
    val gen: suspend () -> Bitmap?
) : AGBitmapSourceBase {
    companion object {
        val NIL = AGAsyncBitmapSource(EmptyCoroutineContext, true, 0, 0) { null }
    }
}
