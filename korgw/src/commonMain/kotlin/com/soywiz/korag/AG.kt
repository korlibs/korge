package com.soywiz.korag

import com.soywiz.kds.*
import com.soywiz.kgl.*
import com.soywiz.klock.*
import com.soywiz.klogger.*
import com.soywiz.kmem.*
import com.soywiz.korag.annotation.KoragExperimental
import com.soywiz.korag.gl.*
import com.soywiz.korag.shader.*
import com.soywiz.korag.shader.gl.*
import com.soywiz.korim.bitmap.*
import com.soywiz.korim.color.*
import com.soywiz.korio.annotations.*
import com.soywiz.korio.async.*
import com.soywiz.korio.lang.*
import com.soywiz.korio.util.*
import com.soywiz.korma.geom.*
import com.soywiz.korma.math.*
import kotlin.contracts.*
import kotlin.coroutines.*
import kotlin.jvm.*

interface AGFactory {
    val supportsNativeFrame: Boolean
    fun create(nativeControl: Any?, config: AGConfig): AG
    fun createFastWindow(title: String, width: Int, height: Int): AGWindow
    //fun createFastWindow(title: String, width: Int, height: Int, config: AGConfig): AGWindow
}

data class AGConfig(val antialiasHint: Boolean = true)

interface AGContainer {
    val ag: AG
    //data class Resized(var width: Int, var height: Int) {
    //	fun setSize(width: Int, height: Int): Resized = this.apply {
    //		this.width = width
    //		this.height = height
    //	}
    //}

    fun repaint(): Unit
}

@KoragExperimental
enum class AGTarget {
    DISPLAY,
    OFFSCREEN
}

interface AGWindow : AGContainer {
}

interface AGFeatures {
    val graphicExtensions: Set<String> get() = emptySet()
    val isInstancedSupported: Boolean get() = false
    val isStorageMultisampleSupported: Boolean get() = false
    val isFloatTextureSupported: Boolean get() = false
}

@OptIn(KorIncomplete::class)
abstract class AG : AGFeatures, Extra by Extra.Mixin() {
    abstract val nativeComponent: Any

    open fun contextLost() {
        Console.info("AG.contextLost()", this)
        contextVersion++
    }

    open val maxTextureSize = Size(2048, 2048)

    open val devicePixelRatio: Double = 1.0
    open val pixelsPerLogicalInchRatio: Double = 1.0
    open val pixelsPerInch: Double = defaultPixelsPerInch
    // Use this in the debug handler, while allowing people to access raw devicePixelRatio without the noise of window scaling
    // I really dont know if "/" or "*" or right but in my mathematical mind "pixelsPerLogicalInchRatio" must increase and not decrease the scale
    // maybe it is pixelsPerLogicalInchRatio / devicePixelRatio ?
    open val computedPixelRatio: Double get() = devicePixelRatio * pixelsPerLogicalInchRatio

    open fun beforeDoRender() {
    }

    companion object {
        const val defaultPixelsPerInch : Double = 96.0
    }

    inline fun doRender(block: () -> Unit) {
        beforeDoRender()
        mainRenderBuffer.init()
        setRenderBufferTemporally(mainRenderBuffer) {
            block()
        }
    }

    open fun offscreenRendering(callback: () -> Unit) {
        callback()
    }

    open fun repaint() {
    }

    fun resized(width: Int, height: Int) {
        resized(0, 0, width, height, width, height)
    }

    open fun resized(x: Int, y: Int, width: Int, height: Int, fullWidth: Int, fullHeight: Int) {
        mainRenderBuffer.setSize(x, y, width, height, fullWidth, fullHeight)
    }

    open fun dispose() {
    }

    // On MacOS components, this will be the size of the component
    open val backWidth: Int get() = mainRenderBuffer.width
    open val backHeight: Int get() = mainRenderBuffer.height

    // On MacOS components, this will be the full size of the window
    val realBackWidth get() = mainRenderBuffer.fullWidth
    val realBackHeight get() = mainRenderBuffer.fullHeight

    val currentWidth: Int get() = currentRenderBuffer?.width ?: mainRenderBuffer.width
    val currentHeight: Int get() = currentRenderBuffer?.height ?: mainRenderBuffer.height

    //protected fun setViewport(v: IntArray) = setViewport(v[0], v[1], v[2], v[3])

    enum class BlendEquation {
        ADD, SUBTRACT, REVERSE_SUBTRACT;
        companion object {
            val VALUES = values()
        }
    }

    enum class BlendFactor {
        DESTINATION_ALPHA,
        DESTINATION_COLOR,
        ONE,
        ONE_MINUS_DESTINATION_ALPHA,
        ONE_MINUS_DESTINATION_COLOR,
        ONE_MINUS_SOURCE_ALPHA,
        ONE_MINUS_SOURCE_COLOR,
        SOURCE_ALPHA,
        SOURCE_COLOR,
        ZERO;
        companion object {
            val VALUES = values()
        }
    }

    data class Scissor(
        var x: Double = 0.0, var y: Double = 0.0,
        var width: Double = 0.0, var height: Double = 0.0
    ) {
        val rect: Rectangle = Rectangle()
            get() {
                field.setTo(x, y, width, height)
                return field
            }

        val top get() = y
        val left get() = x
        val right get() = x + width
        val bottom get() = y + height

        fun copyFrom(that: Scissor): Scissor = setTo(that.x, that.y, that.width, that.height)

        fun setTo(x: Double, y: Double, width: Double, height: Double): Scissor {
            this.x = x
            this.y = y
            this.width = width
            this.height = height
            return this
        }

        fun setTo(x: Int, y: Int, width: Int, height: Int): Scissor =
            setTo(x.toDouble(), y.toDouble(), width.toDouble(), height.toDouble())

        fun setTo(rect: Rectangle): Scissor = setTo(rect.x, rect.y, rect.width, rect.height)

        fun applyTransform(m: Matrix) {
            val l = m.transformX(left, top)
            val t = m.transformY(left, top)
            val r = m.transformX(right, bottom)
            val b = m.transformY(right, bottom)
            setTo(l, t, r - l, b - t)
        }

        override fun toString(): String = "Scissor(x=${x.niceStr}, y=${y.niceStr}, width=${width.niceStr}, height=${height.niceStr})"

        companion object {
            // null is equivalent to Scissor(-Inf, -Inf, +Inf, +Inf)
            fun combine(prev: Scissor?, next: Scissor?, out: Scissor = Scissor()): Scissor? {
                if (prev === null) return next
                if (next === null) return prev
                return prev.rect.intersection(next.rect, out.rect)?.let { rect -> out.setTo(rect) } ?: out.setTo(0.0, 0.0, 0.0, 0.0)
            }
        }
    }

    data class Blending(
        val srcRGB: BlendFactor,
        val dstRGB: BlendFactor,
        val srcA: BlendFactor = srcRGB,
        val dstA: BlendFactor = dstRGB,
        val eqRGB: BlendEquation = BlendEquation.ADD,
        val eqA: BlendEquation = eqRGB
    ) {
        constructor(src: BlendFactor, dst: BlendFactor, eq: BlendEquation = BlendEquation.ADD) : this(
            src, dst,
            src, dst,
            eq, eq
        )

        val disabled: Boolean get() = srcRGB == BlendFactor.ONE && dstRGB == BlendFactor.ZERO && srcA == BlendFactor.ONE && dstA == BlendFactor.ZERO
        val enabled: Boolean get() = !disabled

        private val cachedHashCode: Int = hashCode(srcRGB, dstRGB, srcA, dstA, eqRGB, eqA)
        override fun hashCode(): Int = cachedHashCode

        companion object {
            val NONE = Blending(BlendFactor.ONE, BlendFactor.ZERO, BlendFactor.ONE, BlendFactor.ZERO)
            val NORMAL = Blending(
                BlendFactor.SOURCE_ALPHA, BlendFactor.ONE_MINUS_SOURCE_ALPHA,
                BlendFactor.ONE, BlendFactor.ONE_MINUS_SOURCE_ALPHA
            )
            val ADD = Blending(
                BlendFactor.SOURCE_ALPHA, BlendFactor.DESTINATION_ALPHA,
                BlendFactor.ONE, BlendFactor.ONE
            )
        }
    }

    interface BitmapSourceBase {
        val rgba: Boolean
        val width: Int
        val height: Int
    }

    class SyncBitmapSourceList(
        override val rgba: Boolean,
        override val width: Int,
        override val height: Int,
        val gen: () -> List<Bitmap>?
    ) : BitmapSourceBase {
        companion object {
            val NIL = SyncBitmapSourceList(true, 0, 0) { null }
        }

        override fun toString(): String = "SyncBitmapSourceList(rgba=$rgba, width=$width, height=$height)"
    }

    class SyncBitmapSource(
        override val rgba: Boolean,
        override val width: Int,
        override val height: Int,
        val gen: () -> Bitmap?
    ) : BitmapSourceBase {
        companion object {
            val NIL = SyncBitmapSource(true, 0, 0) { null }
        }

        override fun toString(): String = "SyncBitmapSource(rgba=$rgba, width=$width, height=$height)"
    }

    class AsyncBitmapSource(
        val coroutineContext: CoroutineContext,
        override val rgba: Boolean,
        override val width: Int,
        override val height: Int,
        val gen: suspend () -> Bitmap?
    ) : BitmapSourceBase {
        companion object {
            val NIL = AsyncBitmapSource(EmptyCoroutineContext, true, 0, 0) { null }
        }
    }

    var lastTextureId = 0
    var createdTextureCount = 0
    var deletedTextureCount = 0

    enum class TextureKind { RGBA, LUMINANCE }

    //TODO: there are other possible values
    enum class TextureTargetKind { TEXTURE_2D, TEXTURE_3D, TEXTURE_CUBE_MAP }

    //TODO: would it better if this was an interface ?
    open inner class Texture : Closeable {
        var isFbo: Boolean = false
        open val premultiplied: Boolean = true
        var requestMipmaps: Boolean = false
        var mipmaps: Boolean = false; protected set
        var source: BitmapSourceBase = SyncBitmapSource.NIL
        private var uploaded: Boolean = false
        private var generating: Boolean = false
        private var generated: Boolean = false
        private var tempBitmaps: List<Bitmap?>? = null
        var ready: Boolean = false; private set
        val texId: Int = lastTextureId++
        open val nativeTexId: Int get() = texId

        init {
            createdTextureCount++
        }

        protected fun invalidate() {
            uploaded = false
            generating = false
            generated = false
        }

        fun upload(list: List<Bitmap>, width: Int, height: Int): Texture {
            return upload(SyncBitmapSourceList(rgba = true, width = width, height = height) { list })
        }

        fun upload(bmp: Bitmap?, mipmaps: Boolean = false): Texture {
            return upload(
                if (bmp != null) SyncBitmapSource(
                    rgba = bmp.bpp > 8,
                    width = bmp.width,
                    height = bmp.height
                ) { bmp } else SyncBitmapSource.NIL, mipmaps)
        }

        fun upload(bmp: BitmapSlice<Bitmap>?, mipmaps: Boolean = false): Texture {
            // @TODO: Optimize to avoid copying?
            return upload(bmp?.extract(), mipmaps)
        }

        fun upload(source: BitmapSourceBase, mipmaps: Boolean = false): Texture {
            this.source = source
            uploadedSource()
            invalidate()
            this.requestMipmaps = mipmaps
            return this
        }

        fun uploadAndBindEnsuring(bmp: Bitmap?, mipmaps: Boolean = false): Texture =
            upload(bmp, mipmaps).bindEnsuring()
        fun uploadAndBindEnsuring(bmp: BitmapSlice<Bitmap>?, mipmaps: Boolean = false): Texture =
            upload(bmp, mipmaps).bindEnsuring()
        fun uploadAndBindEnsuring(source: BitmapSourceBase, mipmaps: Boolean = false): Texture =
            upload(source, mipmaps).bindEnsuring()

        protected open fun uploadedSource() {
        }

        open fun bind() {
        }

        open fun unbind() {
        }

        fun manualUpload(): Texture {
            uploaded = true
            return this
        }

        fun bindEnsuring(): Texture {
            bind()
            if (isFbo) return this
            val source = this.source
            if (uploaded) return this

            if (!generating) {
                generating = true
                when (source) {
                    is SyncBitmapSourceList -> {
                        tempBitmaps = source.gen()
                        generated = true
                    }
                    is SyncBitmapSource -> {
                        tempBitmaps = listOf(source.gen())
                        generated = true
                    }
                    is AsyncBitmapSource -> {
                        launchImmediately(source.coroutineContext) {
                            tempBitmaps = listOf(source.gen())
                            generated = true
                        }
                    }
                }
            }

            if (generated) {
                uploaded = true
                generating = false
                generated = false
                actualSyncUpload(source, tempBitmaps, requestMipmaps)
                tempBitmaps = null
                ready = true
            }
            return this
        }

        open fun actualSyncUpload(source: BitmapSourceBase, bmps: List<Bitmap?>?, requestMipmaps: Boolean) {
        }

        init {
            //Console.log("CREATED TEXTURE: $texId")
            //printTexStats()
        }

        private var alreadyClosed = false
        override fun close() {
            if (!alreadyClosed) {
                alreadyClosed = true
                source = SyncBitmapSource.NIL
                tempBitmaps = null
                deletedTextureCount++
                //Console.log("CLOSED TEXTURE: $texId")
                //printTexStats()
            }
        }

        private fun printTexStats() {
            //Console.log("create=$createdCount, delete=$deletedCount, alive=${createdCount - deletedCount}")
        }
    }

    data class TextureUnit constructor(
        var texture: AG.Texture? = null,
        var linear: Boolean = true,
        var trilinear: Boolean? = null,
    ) {
        fun set(texture: AG.Texture?, linear: Boolean, trilinear: Boolean? = null) {
            this.texture = texture
            this.linear = linear
            this.trilinear = trilinear
        }
    }

    open class Buffer constructor(val kind: AGBufferKind, val list: AGList) {
        enum class Kind { INDEX, VERTEX }

        var dirty = false
        internal var mem: FBuffer? = null
        internal var memOffset: Int = 0
        internal var memLength: Int = 0

        open fun afterSetMem() {
        }

        fun upload(data: ByteArray, offset: Int = 0, length: Int = data.size): Buffer {
            mem = FBuffer(length)
            mem!!.setAlignedArrayInt8(0, data, offset, length)
            memOffset = 0
            memLength = length
            dirty = true
            afterSetMem()
            return this
        }

        fun upload(data: FloatArray, offset: Int = 0, length: Int = data.size): Buffer {
            mem = FBuffer(length * 4)
            mem!!.setAlignedArrayFloat32(0, data, offset, length)
            memOffset = 0
            memLength = length * 4
            dirty = true
            afterSetMem()
            return this
        }

        fun upload(data: IntArray, offset: Int = 0, length: Int = data.size): Buffer {
            mem = FBuffer(length * 4)
            mem!!.setAlignedArrayInt32(0, data, offset, length)
            memOffset = 0
            memLength = length * 4
            dirty = true
            afterSetMem()
            return this
        }

        fun upload(data: ShortArray, offset: Int = 0, length: Int = data.size): Buffer {
            if (mem == null || mem!!.size < length * 2) {
                mem = FBuffer(length * 2)
            }
            mem!!.setAlignedArrayInt16(0, data, offset, length)
            memOffset = 0
            memLength = length * 2
            dirty = true
            afterSetMem()
            return this
        }

        fun upload(data: FBuffer, offset: Int = 0, length: Int = data.size): Buffer {
            mem = data
            memOffset = offset
            memLength = length
            dirty = true
            afterSetMem()
            return this
        }

        internal var agId: Int = list.bufferCreate()

        open fun close(list: AGList) {
            mem = null
            memOffset = 0
            memLength = 0
            dirty = true

            list.bufferDelete(this.agId)
            agId = 0
        }

    }

    enum class DrawType {
        POINTS,
        LINE_STRIP,
        LINE_LOOP,
        LINES,
        TRIANGLES,
        TRIANGLE_STRIP,
        TRIANGLE_FAN;

        companion object {
            val VALUES = values()
        }
    }

    enum class IndexType {
        UBYTE, USHORT,
        // https://developer.mozilla.org/en-US/docs/Web/API/WebGLRenderingContext/drawElements
        @Deprecated("UINT is not always supported on webgl")
        UINT;

        companion object {
            val VALUES = values()
        }
    }

    enum class ReadKind(val size: Int) {
        COLOR(4), DEPTH(4), STENCIL(1);
        companion object { val VALUES = values() }
    }

    val dummyTexture by lazy { createTexture() }

    fun createTexture(): Texture = createTexture(premultiplied = true)
    fun createTexture(bmp: Bitmap, mipmaps: Boolean = false): Texture = createTexture(bmp.premultiplied).upload(bmp, mipmaps)
    fun createTexture(bmp: BitmapSlice<Bitmap>, mipmaps: Boolean = false): Texture =
        createTexture(bmp.premultiplied).upload(bmp, mipmaps)

    fun createTexture(bmp: Bitmap, mipmaps: Boolean = false, premultiplied: Boolean = true): Texture =
        createTexture(premultiplied).upload(bmp, mipmaps)

    open fun createTexture(premultiplied: Boolean, targetKind: TextureTargetKind = TextureTargetKind.TEXTURE_2D): Texture = Texture()

    open fun createBuffer(kind: AGBufferKind): Buffer = commandsNoWait { Buffer(kind, it) }
    fun createIndexBuffer() = createBuffer(AGBufferKind.INDEX)
    fun createVertexBuffer() = createBuffer(AGBufferKind.VERTEX)

    fun createVertexData(vararg attributes: Attribute, layoutSize: Int? = null) = AG.VertexData(createVertexBuffer(), VertexLayout(*attributes, layoutSize = layoutSize))

    fun createIndexBuffer(data: ShortArray, offset: Int = 0, length: Int = data.size - offset) =
        createIndexBuffer().apply {
            upload(data, offset, length)
        }

    fun createIndexBuffer(data: FBuffer, offset: Int = 0, length: Int = data.size - offset) =
        createIndexBuffer().apply {
            upload(data, offset, length)
        }

    fun createVertexBuffer(data: FloatArray, offset: Int = 0, length: Int = data.size - offset) =
        createVertexBuffer().apply {
            upload(data, offset, length)
        }

    fun createVertexBuffer(data: FBuffer, offset: Int = 0, length: Int = data.size - offset) =
        createVertexBuffer().apply {
            upload(data, offset, length)
        }

    enum class StencilOp {
        DECREMENT_SATURATE,
        DECREMENT_WRAP,
        INCREMENT_SATURATE,
        INCREMENT_WRAP,
        INVERT,
        KEEP,
        SET,
        ZERO;
        companion object {
            val VALUES = values()
        }
    }

    enum class TriangleFace {
        FRONT, BACK, FRONT_AND_BACK, NONE;
        companion object {
            val VALUES = values()
        }
    }

    enum class CompareMode {
        ALWAYS, EQUAL, GREATER, GREATER_EQUAL, LESS, LESS_EQUAL, NEVER, NOT_EQUAL;
        companion object {
            val VALUES = values()
        }
    }

    data class ColorMaskState(
        var red: Boolean = true,
        var green: Boolean = true,
        var blue: Boolean = true,
        var alpha: Boolean = true
    ) {
        //val enabled = !red || !green || !blue || !alpha
    }

    enum class CullFace {
        BOTH, FRONT, BACK;
        companion object {
            val VALUES = values()
        }
    }

    // Default: CCW
    enum class FrontFace {
        BOTH, // @TODO: This is incorrect
        CCW, CW;
        companion object {
            val DEFAULT: FrontFace get() = CCW
            val VALUES = values()
        }
    }

    data class RenderState(
        var depthFunc: CompareMode = CompareMode.ALWAYS,
        var depthMask: Boolean = true,
        var depthNear: Float = 0f,
        var depthFar: Float = 1f,
        @Deprecated("This is not used anymore, since it is not available on WebGL")
        var lineWidth: Float = 1f,
        var frontFace: FrontFace = FrontFace.BOTH
    )

    data class StencilState(
        var enabled: Boolean = false,
        var triangleFace: TriangleFace = TriangleFace.FRONT_AND_BACK,
        var compareMode: CompareMode = CompareMode.ALWAYS,
        var actionOnBothPass: StencilOp = StencilOp.KEEP,
        var actionOnDepthFail: StencilOp = StencilOp.KEEP,
        var actionOnDepthPassStencilFail: StencilOp = StencilOp.KEEP,
        var referenceValue: Int = 0,
        var readMask: Int = 0xFF,
        var writeMask: Int = 0xFF
    ) {
        fun copyFrom(other: StencilState) {
            this.enabled = other.enabled
            this.triangleFace = other.triangleFace
            this.compareMode = other.compareMode
            this.actionOnBothPass = other.actionOnBothPass
            this.actionOnDepthFail = other.actionOnDepthFail
            this.actionOnDepthPassStencilFail = other.actionOnDepthPassStencilFail
            this.referenceValue = other.referenceValue
            this.readMask = other.readMask
            this.writeMask = other.writeMask
        }
    }

    private val dummyRenderState = RenderState()
    private val dummyStencilState = StencilState()
    private val dummyColorMaskState = ColorMaskState()

    //open val supportInstancedDrawing: Boolean get() = false

    @Deprecated("Use draw(Batch) or drawV2() instead")
    fun draw(
        vertices: Buffer,
        program: Program,
        type: DrawType,
        vertexLayout: VertexLayout,
        vertexCount: Int,
        indices: Buffer? = null,
        indexType: IndexType = IndexType.USHORT,
        offset: Int = 0,
        blending: Blending = Blending.NORMAL,
        uniforms: UniformValues = UniformValues.EMPTY,
        stencil: StencilState = dummyStencilState,
        colorMask: ColorMaskState = dummyColorMaskState,
        renderState: RenderState = dummyRenderState,
        scissor: Scissor? = null,
        instances: Int = 1
    ) = draw(batch.also { batch ->
        batch.vertices = vertices
        batch.program = program
        batch.type = type
        batch.vertexLayout = vertexLayout
        batch.vertexCount = vertexCount
        batch.indices = indices
        batch.indexType = indexType
        batch.offset = offset
        batch.blending = blending
        batch.uniforms = uniforms
        batch.stencil = stencil
        batch.colorMask = colorMask
        batch.renderState = renderState
        batch.scissor = scissor
        batch.instances = instances
    })

    fun drawV2(
        vertexData: FastArrayList<VertexData>,
        program: Program,
        type: DrawType,
        vertexCount: Int,
        indices: Buffer? = null,
        indexType: IndexType = IndexType.USHORT,
        offset: Int = 0,
        blending: Blending = Blending.NORMAL,
        uniforms: UniformValues = UniformValues.EMPTY,
        stencil: StencilState = dummyStencilState,
        colorMask: ColorMaskState = dummyColorMaskState,
        renderState: RenderState = dummyRenderState,
        scissor: Scissor? = null,
        instances: Int = 1
    ) = draw(batch.also { batch ->
        batch.vertexData = vertexData
        batch.program = program
        batch.type = type
        batch.vertexCount = vertexCount
        batch.indices = indices
        batch.indexType = indexType
        batch.offset = offset
        batch.blending = blending
        batch.uniforms = uniforms
        batch.stencil = stencil
        batch.colorMask = colorMask
        batch.renderState = renderState
        batch.scissor = scissor
        batch.instances = instances
    })

    /** List<VertexData> -> VAO */
    @JvmInline
    value class VertexArrayObject(
        val list: FastArrayList<VertexData>
    )

    data class VertexData constructor(
        var _buffer: Buffer?,
        var layout: VertexLayout = VertexLayout()
    ) {
        val buffer: Buffer get() = _buffer!!
    }

    data class Batch constructor(
        var vertexData: FastArrayList<VertexData> = fastArrayListOf(VertexData(null)),
        var program: Program = DefaultShaders.PROGRAM_DEBUG,
        var type: DrawType = DrawType.TRIANGLES,
        var vertexCount: Int = 0,
        var indices: Buffer? = null,
        var indexType: IndexType = IndexType.USHORT,
        var offset: Int = 0,
        var blending: Blending = Blending.NORMAL,
        var uniforms: UniformValues = UniformValues.EMPTY,
        var stencil: StencilState = StencilState(),
        var colorMask: ColorMaskState = ColorMaskState(),
        var renderState: RenderState = RenderState(),
        var scissor: Scissor? = null,
        var instances: Int = 1
    ) {
        private val singleVertexData = FastArrayList<VertexData>()

        private fun ensureSingleVertexData() {
            if (singleVertexData.isEmpty()) singleVertexData.add(VertexData(null))
            vertexData = singleVertexData
        }

        @Deprecated("Use vertexData instead")
        var vertices: Buffer
            get() = (singleVertexData.firstOrNull() ?: vertexData.first()).buffer
            set(value) {
                ensureSingleVertexData()
                singleVertexData[0]._buffer = value
            }
        @Deprecated("Use vertexData instead")
        var vertexLayout: VertexLayout
            get() = (singleVertexData.firstOrNull() ?: vertexData.first()).layout
            set(value) {
                ensureSingleVertexData()
                singleVertexData[0].layout = value
            }
    }

    private val batch = Batch()

    open fun draw(batch: Batch) {
        val instances = batch.instances
        val program = batch.program
        val type = batch.type
        val vertexCount = batch.vertexCount
        val indices = batch.indices
        val indexType = batch.indexType
        val offset = batch.offset
        val blending = batch.blending
        val uniforms = batch.uniforms
        val stencil = batch.stencil
        val colorMask = batch.colorMask
        val renderState = batch.renderState
        val scissor = batch.scissor

        //println("SCISSOR: $scissor")

        //finalScissor.setTo(0, 0, backWidth, backHeight)
        if (indices != null && indices.kind != AGBufferKind.INDEX) invalidOp("Not a IndexBuffer")

        commandsNoWait { list ->
            applyScissorState(list, scissor)

            getProgram(program, config = when {
                uniforms.useExternalSampler() -> ProgramConfig.EXTERNAL_TEXTURE_SAMPLER
                else -> ProgramConfig.DEFAULT
            }).use(list)

            val vaoId = list.vaoCreate()
            list.vaoSet(vaoId, VertexArrayObject(batch.vertexData))
            list.vaoUse(vaoId)

            val uboId = list.uboCreate()
            list.uboSet(uboId, uniforms)
            list.uboUse(uboId)

            list.enableDisable(AGEnable.BLEND, blending.enabled) {
                list.blendEquation(blending.eqRGB, blending.eqA)
                list.blendFunction(blending.srcRGB, blending.dstRGB, blending.srcA, blending.dstA)
            }

            list.enableDisable(AGEnable.CULL_FACE, renderState.frontFace != FrontFace.BOTH) {
                list.frontFace(renderState.frontFace)
            }

            list.depthMask(renderState.depthMask)
            list.depthRange(renderState.depthNear, renderState.depthFar)

            list.enableDisable(AGEnable.DEPTH, renderState.depthFunc != CompareMode.ALWAYS) {
                list.depthFunction(renderState.depthFunc)
            }

            list.colorMask(colorMask.red, colorMask.green, colorMask.blue, colorMask.alpha)

            if (stencil.enabled) {
                list.enable(AGEnable.STENCIL)
                list.stencilFunction(stencil.compareMode, stencil.referenceValue, stencil.readMask)
                list.stencilOperation(
                    stencil.actionOnDepthFail,
                    stencil.actionOnDepthPassStencilFail,
                    stencil.actionOnBothPass
                )
                list.stencilMask(stencil.writeMask)
            } else {
                list.disable(AGEnable.STENCIL)
                list.stencilMask(0)
            }

            //val viewport = FBuffer(4 * 4)
            //gl.getIntegerv(KmlGl.VIEWPORT, viewport)
            //println("viewport=${viewport.getAlignedInt32(0)},${viewport.getAlignedInt32(1)},${viewport.getAlignedInt32(2)},${viewport.getAlignedInt32(3)}")

            list.draw(type, vertexCount, offset, instances, if (indices != null) indexType else null, indices)

            //list.uboUse(0)
            list.uboDelete(uboId)

            list.vaoUse(0)
            list.vaoDelete(vaoId)
        }
    }

    fun UniformValues.useExternalSampler(): Boolean {
        var useExternalSampler = false
        this.fastForEach { uniform, value ->
            val uniformType = uniform.type
            when (uniformType) {
                VarType.Sampler2D -> {
                    val unit = value.fastCastTo<TextureUnit>()
                    val tex = (unit.texture.fastCastTo<AGOpengl.GlTexture?>())
                    if (tex != null) {
                        if (tex.forcedTexTarget != KmlGl.TEXTURE_2D && tex.forcedTexTarget != -1) {
                            useExternalSampler = true
                        }
                    }
                }
            }
        }
        return useExternalSampler
    }

    open fun disposeTemporalPerFrameStuff() = Unit

    val frameRenderBuffers = LinkedHashSet<RenderBuffer>()
    val renderBuffers = Pool<RenderBuffer>() { createRenderBuffer() }

    interface BaseRenderBuffer {
        val x: Int
        val y: Int
        val width: Int
        val height: Int
        val fullWidth: Int
        val fullHeight: Int
        val scissor: RectangleInt?
        fun setSize(x: Int, y: Int, width: Int, height: Int, fullWidth: Int = width, fullHeight: Int = height)
        fun init() = Unit
        fun set() = Unit
        fun unset() = Unit
        fun scissor(scissor: RectangleInt?)
    }

    object RenderBufferConsts {
        const val DEFAULT_INITIAL_WIDTH = 128
        const val DEFAULT_INITIAL_HEIGHT = 128
    }

    open class BaseRenderBufferImpl : BaseRenderBuffer {
        override var x = 0
        override var y = 0
        override var width = RenderBufferConsts.DEFAULT_INITIAL_WIDTH
        override var height = RenderBufferConsts.DEFAULT_INITIAL_HEIGHT
        override var fullWidth = RenderBufferConsts.DEFAULT_INITIAL_WIDTH
        override var fullHeight = RenderBufferConsts.DEFAULT_INITIAL_HEIGHT
        private val _scissor = RectangleInt()
        override var scissor: RectangleInt? = null

        override fun setSize(x: Int, y: Int, width: Int, height: Int, fullWidth: Int, fullHeight: Int) {
            this.x = x
            this.y = y
            this.width = width
            this.height = height
            this.fullWidth = fullWidth
            this.fullHeight = fullHeight
        }

        override fun scissor(scissor: RectangleInt?) {
            this.scissor = scissor?.let { _scissor.setTo(it) }
        }
    }

    @KoragExperimental
    var agTarget = AGTarget.DISPLAY

    val mainRenderBuffer: BaseRenderBuffer by lazy {
        when (agTarget) {
            AGTarget.DISPLAY -> createMainRenderBuffer()
            AGTarget.OFFSCREEN -> createRenderBuffer()
        }
    }

    open fun createMainRenderBuffer(): BaseRenderBuffer = BaseRenderBufferImpl()

    open inner class RenderBuffer : Closeable, BaseRenderBufferImpl() {
        open val id: Int = -1
        private var cachedTexVersion = -1
        private var _tex: Texture? = null
        protected var nsamples: Int = 1
        protected var hasDepth: Boolean = true
        protected var hasStencil: Boolean = true

        val tex: AG.Texture
            get() {
                if (cachedTexVersion != contextVersion) {
                    cachedTexVersion = contextVersion
                    _tex = this@AG.createTexture(premultiplied = true).manualUpload().apply { isFbo = true }
                }
                return _tex!!
            }

        protected var dirty = true

        override fun setSize(x: Int, y: Int, width: Int, height: Int, fullWidth: Int, fullHeight: Int) {
            if (
                this.x != x ||this.y != y ||
                this.width != width || this.height != height ||
                this.fullWidth != fullWidth || this.fullHeight != fullHeight
            ) {
                super.setSize(x, y, width, height, fullWidth, fullHeight)
                dirty = true
            }
        }

        fun setSamples(samples: Int) {
            if (this.nsamples != samples) {
                nsamples = samples
                dirty = true
            }
        }

        fun setExtra(hasDepth: Boolean = true, hasStencil: Boolean = true) {
            if (this.hasDepth != hasDepth || this.hasStencil != hasStencil) {
                this.hasDepth = hasDepth
                this.hasStencil = hasStencil
                dirty = true
            }
        }

        override fun set(): Unit = Unit
        fun readBitmap(bmp: Bitmap32) = this@AG.readColor(bmp)
        fun readDepth(width: Int, height: Int, out: FloatArray): Unit = this@AG.readDepth(width, height, out)
        override fun close(): Unit {
            cachedTexVersion = -1
            _tex?.close()
            _tex = null
        }
    }

    open fun createRenderBuffer() = RenderBuffer()

    fun flip() {
        disposeTemporalPerFrameStuff()
        renderBuffers.free(frameRenderBuffers)
        if (frameRenderBuffers.isNotEmpty()) frameRenderBuffers.clear()
        flipInternal()
    }

    open fun flipInternal() = Unit

    open fun startFrame() {
    }

    open fun clear(
        color: RGBA = Colors.TRANSPARENT_BLACK,
        depth: Float = 1f,
        stencil: Int = 0,
        clearColor: Boolean = true,
        clearDepth: Boolean = true,
        clearStencil: Boolean = true,
        scissor: Scissor? = null
    ) {
        commandsNoWait { list ->
            //println("CLEAR: $color, $depth")
            applyScissorState(list, scissor)
            //gl.disable(KmlGl.SCISSOR_TEST)
            if (clearColor) {
                list.colorMask(true, true, true, true)
                list.clearColor(color.rf, color.gf, color.bf, color.af)
            }
            if (clearDepth) {
                list.depthMask(true)
                list.clearDepth(depth)
            }
            if (clearStencil) {
                list.stencilMask(-1)
                list.clearStencil(stencil)
            }
            list.clear(clearColor, clearDepth, clearStencil)
        }
    }


    private val finalScissorBL = Rectangle()
    private val tempRect = Rectangle()

    protected fun applyScissorState(list: AGList, scissor: Scissor? = null) {
        //println("applyScissorState")
        if (this.currentRenderBuffer == null) {
            //println("this.currentRenderBuffer == null")
        }
        val currentRenderBuffer = this.currentRenderBuffer ?: return
        if (currentRenderBuffer === mainRenderBuffer) {
            var realScissors: Rectangle? = finalScissorBL
            realScissors?.setTo(0.0, 0.0, realBackWidth.toDouble(), realBackHeight.toDouble())
            if (scissor != null) {
                tempRect.setTo(
                    currentRenderBuffer.x + scissor.x,
                    ((currentRenderBuffer.y + currentRenderBuffer.height) - (scissor.y + scissor.height)),
                    (scissor.width),
                    scissor.height
                )
                realScissors = realScissors?.intersection(tempRect, realScissors)
            }

            //println("currentRenderBuffer: $currentRenderBuffer")

            val renderBufferScissor = currentRenderBuffer.scissor
            if (renderBufferScissor != null) {
                realScissors = realScissors?.intersection(renderBufferScissor.rect, realScissors)
            }

            //println("[MAIN_BUFFER] realScissors: $realScissors")

            list.enable(AGEnable.SCISSOR)
            if (realScissors != null) {
                list.scissor(realScissors.x.toInt(), realScissors.y.toInt(), realScissors.width.toInt(), realScissors.height.toInt())
            } else {
                list.scissor(0, 0, 0, 0)
            }
        } else {
            //println("[RENDER_TARGET] scissor: $scissor")

            list.enableDisable(AGEnable.SCISSOR, scissor != null) {
                list.scissor(scissor!!.x.toIntRound(), scissor.y.toIntRound(), scissor.width.toIntRound(), scissor.height.toIntRound())
            }
        }
    }


    fun clearStencil(stencil: Int = 0, scissor: Scissor? = null) = clear(clearColor = false, clearDepth = false, clearStencil = true, stencil = stencil, scissor = scissor)
    fun clearDepth(depth: Float = 1f, scissor: Scissor? = null) = clear(clearColor = false, clearDepth = true, clearStencil = false, depth = depth, scissor = scissor)
    fun clearColor(color: RGBA = Colors.TRANSPARENT_BLACK, scissor: Scissor? = null) = clear(clearColor = true, clearDepth = false, clearStencil = false, color = color, scissor = scissor)

    //@PublishedApi
    @KoragExperimental
    var currentRenderBuffer: BaseRenderBuffer? = null
        private set

    val currentRenderBufferOrMain: BaseRenderBuffer get() = currentRenderBuffer ?: mainRenderBuffer

    val renderingToTexture get() = currentRenderBuffer !== mainRenderBuffer && currentRenderBuffer !== null

    inline fun backupTexture(tex: Texture?, callback: () -> Unit) {
        if (tex != null) {
            readColorTexture(tex, backWidth, backHeight)
        }
        try {
            callback()
        } finally {
            if (tex != null) drawTexture(tex)
        }
    }

    inline fun setRenderBufferTemporally(rb: BaseRenderBuffer, callback: () -> Unit) {
        val old = setRenderBuffer(rb)
        try {
            callback()
        } finally {
            setRenderBuffer(old)
        }
    }

    open fun fixWidthForRenderToTexture(width: Int): Int = width.nextMultipleOf(64)
    open fun fixHeightForRenderToTexture(height: Int): Int = height.nextMultipleOf(64)

    //open fun fixWidthForRenderToTexture(width: Int): Int = width
    //open fun fixHeightForRenderToTexture(height: Int): Int = height

    @KoragExperimental
    fun unsafeAllocateFrameRenderBuffer(width: Int, height: Int, hasDepth: Boolean = false, hasStencil: Boolean = false, msamples: Int = 1): RenderBuffer {
        val realWidth = fixWidthForRenderToTexture(width)
        val realHeight = fixHeightForRenderToTexture(height)
        val rb = renderBuffers.alloc()
        frameRenderBuffers += rb
        rb.setSize(0, 0, realWidth, realHeight, realWidth, realHeight)
        rb.setExtra(hasDepth = hasDepth, hasStencil = hasStencil)
        rb.setSamples(msamples)
        //println("unsafeAllocateFrameRenderBuffer($width, $height), real($realWidth, $realHeight), $rb")
        return rb
    }

    @KoragExperimental
    fun unsafeFreeFrameRenderBuffer(rb: RenderBuffer) {
        frameRenderBuffers -= rb
        renderBuffers.free(rb)
    }

    @OptIn(KoragExperimental::class)
    inline fun renderToTexture(
        width: Int, height: Int,
        render: (rb: RenderBuffer) -> Unit,
        hasDepth: Boolean = false, hasStencil: Boolean = false, msamples: Int = 1,
        use: (tex: Texture, texWidth: Int, texHeight: Int) -> Unit
    ) {
        val rb = unsafeAllocateFrameRenderBuffer(width, height, hasDepth, hasStencil, msamples)
        try {
            setRenderBufferTemporally(rb) {
                clear(Colors.TRANSPARENT_BLACK) // transparent
                render(rb)
            }
            use(rb.tex, rb.width, rb.height)
        } finally {
            unsafeFreeFrameRenderBuffer(rb)
        }
    }

    inline fun renderToBitmap(
        bmp: Bitmap32,
        hasDepth: Boolean = false, hasStencil: Boolean = false, msamples: Int = 1,
        render: () -> Unit
    ) {
        renderToTexture(bmp.width, bmp.height, render = {
            render()
            //println("renderToBitmap.readColor: $currentRenderBuffer")
            readColor(bmp)
        }, hasDepth = hasDepth, hasStencil = hasStencil, msamples = msamples, use = { _, _, _ -> })
    }

    fun setRenderBuffer(renderBuffer: BaseRenderBuffer?): BaseRenderBuffer? {
        val old = currentRenderBuffer
        currentRenderBuffer?.unset()
        currentRenderBuffer = renderBuffer
        renderBuffer?.set()
        return old
    }

    open fun readColor(bitmap: Bitmap32): Unit {
        commandsSync { it.readPixels(0, 0, bitmap.width, bitmap.height, bitmap.data.ints, ReadKind.COLOR) }
    }
    open fun readDepth(width: Int, height: Int, out: FloatArray): Unit {
        commandsSync { it.readPixels(0, 0, width, height, out, ReadKind.DEPTH) }
    }
    open fun readStencil(bitmap: Bitmap8): Unit {
        commandsSync { it.readPixels(0, 0, bitmap.width, bitmap.height, bitmap.data, ReadKind.STENCIL) }
    }
    fun readDepth(out: FloatArray2): Unit = readDepth(out.width, out.height, out.data)
    open fun readColorTexture(texture: Texture, width: Int = backWidth, height: Int = backHeight): Unit = TODO()
    fun readColor() = Bitmap32(backWidth, backHeight).apply { readColor(this) }
    fun readDepth() = FloatArray2(backWidth, backHeight) { 0f }.apply { readDepth(this) }

    inner class TextureDrawer {
        val VERTEX_COUNT = 4
        val vertices = createBuffer(AGBufferKind.VERTEX)
        val vertexLayout = VertexLayout(DefaultShaders.a_Pos, DefaultShaders.a_Tex)
        val verticesData = FBuffer(VERTEX_COUNT * vertexLayout.totalSize)
        val program = Program(VertexShader {
            DefaultShaders {
                SET(v_Tex, a_Tex)
                SET(out, vec4(a_Pos, 0f.lit, 1f.lit))
            }
        }, FragmentShader {
            DefaultShaders {
                //out setTo vec4(1f, 1f, 0f, 1f)
                SET(out, texture2D(u_Tex, v_Tex["xy"]))
            }
        })
        val uniforms = UniformValues()

        fun setVertex(n: Int, px: Float, py: Float, tx: Float, ty: Float) {
            val offset = n * 4
            verticesData.setAlignedFloat32(offset + 0, px)
            verticesData.setAlignedFloat32(offset + 1, py)
            verticesData.setAlignedFloat32(offset + 2, tx)
            verticesData.setAlignedFloat32(offset + 3, ty)
        }

        fun draw(tex: Texture, left: Float, top: Float, right: Float, bottom: Float) {
            //tex.upload(Bitmap32(32, 32) { x, y -> Colors.RED })
            //uniforms[DefaultShaders.u_Tex] = TextureUnit(tex)

            val texLeft = -1f
            val texRight = +1f
            val texTop = -1f
            val texBottom = +1f

            setVertex(0, left, top, texLeft, texTop)
            setVertex(1, right, top, texRight, texTop)
            setVertex(2, left, bottom, texLeft, texBottom)
            setVertex(3, right, bottom, texRight, texBottom)

            vertices.upload(verticesData)
            draw(
                vertices = vertices,
                program = program,
                type = AG.DrawType.TRIANGLE_STRIP,
                vertexLayout = vertexLayout,
                vertexCount = 4,
                uniforms = uniforms,
                blending = AG.Blending.NONE
            )
        }
    }

    //////////////


    private val programs = FastIdentityMap<Program, FastIdentityMap<ProgramConfig, AgProgram>>()

    @JvmOverloads
    fun getProgram(program: Program, config: ProgramConfig = ProgramConfig.DEFAULT): AgProgram {
        return programs.getOrPut(program) { FastIdentityMap() }.getOrPut(config) { AgProgram(program, config) }
    }

    inner class AgProgram(val program: Program, val programConfig: ProgramConfig) {
        var cachedVersion = -1
        var programId = 0

        fun ensure(list: AGList) {
            if (cachedVersion != contextVersion) {
                val time = measureTime {
                    programId = list.createProgram(program, programConfig)
                    cachedVersion = contextVersion
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
            if (programId != 0) list.deleteProgram(programId)
            programId = 0
        }
    }

    //////////////

    val textureDrawer by lazy { TextureDrawer() }
    val flipRenderTexture = true

    fun drawTexture(tex: Texture) {
        textureDrawer.draw(tex, -1f, +1f, +1f, -1f)
    }

    private val drawTempTexture: Texture by lazy { createTexture() }

    protected val _globalState = AGGlobalState()
    var contextVersion: Int by _globalState::contextVersion
    @PublishedApi internal val _list = _globalState.createList()

    val multithreadedRendering: Boolean get() = false

    @OptIn(ExperimentalContracts::class)
    @KoragExperimental
    @Deprecated("Use commandsNoWait instead")
    inline fun <T> commands(block: (AGList) -> T): T {
        contract { callsInPlace(block, InvocationKind.EXACTLY_ONCE) }
        return commandsNoWait(block)
    }

    /**
     * Queues commands, and wait for them to be executed synchronously
     */
    @OptIn(ExperimentalContracts::class)
    @KoragExperimental
    inline fun <T> commandsSync(block: (AGList) -> T): T {
        contract { callsInPlace(block, InvocationKind.EXACTLY_ONCE) }
        val result = block(_list)
        if (multithreadedRendering) {
            runBlockingNoJs { _list.sync() }
        } else {
            _executeList(_list)
        }
        return result
    }

    /**
     * Queues commands without waiting
     */
    @OptIn(ExperimentalContracts::class)
    @KoragExperimental
    inline fun <T> commandsNoWait(block: (AGList) -> T): T {
        contract { callsInPlace(block, InvocationKind.EXACTLY_ONCE) }
        val result = block(_list)
        if (!multithreadedRendering) _executeList(_list)
        return result
    }

    /**
     * Queues commands and suspend until they are executed
     */
    @OptIn(ExperimentalContracts::class)
    @KoragExperimental
    suspend inline fun <T> commandsSuspend(block: (AGList) -> T): T {
        contract { callsInPlace(block, InvocationKind.EXACTLY_ONCE) }
        val result = block(_list)
        if (!multithreadedRendering) {
            _executeList(_list)
        } else {
            _list.sync()
        }
        return result
    }

    @PublishedApi
    internal fun _executeList(list: AGList) = executeList(list)

    protected open fun executeList(list: AGList) {
    }

    fun drawBitmap(bmp: Bitmap) {
        drawTempTexture.upload(bmp, mipmaps = false)
        drawTexture(drawTempTexture)
        drawTempTexture.upload(Bitmaps.transparent)
    }

    class UniformValues() {
        companion object {
            internal val EMPTY = UniformValues()
        }

        private val _uniforms = FastArrayList<Uniform>()
        private val _values = FastArrayList<Any>()
        val uniforms = _uniforms as List<Uniform>

        val keys get() = uniforms
        val values = _values as List<Any>

        val size get() = _uniforms.size

        fun isEmpty(): Boolean = size == 0
        fun isNotEmpty(): Boolean = size != 0

        constructor(vararg pairs: Pair<Uniform, Any>) : this() {
            for (pair in pairs) put(pair.first, pair.second)
        }

        inline fun fastForEach(block: (uniform: Uniform, value: Any) -> Unit) {
            for (n in 0 until size) {
                block(uniforms[n], values[n])
            }
        }

        operator fun plus(other: UniformValues): UniformValues {
            return UniformValues().put(this).put(other)
        }

        fun clear() {
            _uniforms.clear()
            _values.clear()
        }

        operator fun contains(uniform: Uniform): Boolean = _uniforms.contains(uniform)

        operator fun get(uniform: Uniform): Any? {
            for (n in 0 until _uniforms.size) {
                if (_uniforms[n].name == uniform.name) return _values[n]
            }
            return null
        }

        operator fun set(uniform: Uniform, value: Any) = put(uniform, value)

        fun putOrRemove(uniform: Uniform, value: Any?) {
            if (value == null) {
                remove(uniform)
            } else {
                put(uniform, value)
            }
        }

        fun put(uniform: Uniform, value: Any): UniformValues {
            for (n in 0 until _uniforms.size) {
                if (_uniforms[n].name == uniform.name) {
                    _values[n] = value
                    return this
                }
            }

            _uniforms.add(uniform)
            _values.add(value)
            return this
        }

        fun remove(uniform: Uniform) {
            for (n in 0 until _uniforms.size) {
                if (_uniforms[n].name == uniform.name) {
                    _uniforms.removeAt(n)
                    _values.removeAt(n)
                    return
                }
            }
        }

        fun put(uniforms: UniformValues): UniformValues {
            for (n in 0 until uniforms.size) {
                this.put(uniforms._uniforms[n], uniforms._values[n])
            }
            return this
        }

        fun setTo(uniforms: UniformValues) {
            clear()
            put(uniforms)
        }

        override fun toString() = "{" + keys.zip(values).map { "${it.first}=${it.second}" }.joinToString(", ") + "}"
    }
}


fun AG.Blending.toRenderFboIntoBack() = this
fun AG.Blending.toRenderImageIntoFbo() = this
