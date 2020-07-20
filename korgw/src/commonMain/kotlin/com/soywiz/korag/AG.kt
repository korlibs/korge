package com.soywiz.korag

import com.soywiz.kds.*
import com.soywiz.kmem.*
import com.soywiz.korag.shader.*
import com.soywiz.korim.bitmap.*
import com.soywiz.korim.color.*
import com.soywiz.korio.async.*
import com.soywiz.korio.lang.*
import com.soywiz.korma.geom.*
import kotlin.coroutines.*
import kotlinx.coroutines.*

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

interface AGWindow : AGContainer {
}

abstract class AG : Extra by Extra.Mixin() {
	var contextVersion = 0
	abstract val nativeComponent: Any

    fun contextLost() {
        println("AG.contextLost() : $this")
        contextVersion++
    }

	open val maxTextureSize = Size(2048, 2048)

	open val devicePixelRatio: Double = 1.0

	private val _onReadyDeferred = CompletableDeferred<AG>(Job())
	protected fun ready() {
		//println("AG.ready!")
		__ready()
	}

	fun __ready() {
		_onReadyDeferred.complete(this)
	}

	val onReady: Deferred<AG> = _onReadyDeferred
	val onRender = Signal<AG>()

	open fun offscreenRendering(callback: () -> Unit) {
		callback()
	}

	open fun repaint() {
	}

	open fun resized(width: Int, height: Int) {
		//println("ag.resized($width, $height)")
		//printStackTrace()
		mainRenderBuffer.setSize(width, height)
		setViewport(0, 0, width, height)
	}

	open fun dispose() {
	}

	val viewport = intArrayOf(0, 0, 640, 480)

	open val backWidth: Int get() = viewport[2]
	open val backHeight: Int get() = viewport[3]

	protected fun getViewport(out: IntArray): IntArray {
		arraycopy(this.viewport, 0, out, 0, 4)
		return out
	}

	open fun setViewport(x: Int, y: Int, width: Int, height: Int) {
		viewport[0] = x
		viewport[1] = y
		viewport[2] = width
		viewport[3] = height
	}

	protected fun setViewport(v: IntArray) = setViewport(v[0], v[1], v[2], v[3])

	enum class BlendEquation {
		ADD, SUBTRACT, REVERSE_SUBTRACT
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
	}

	data class Scissor(
		var x: Int, var y: Int,
		var width: Int, var height: Int
	) {
		val top get() = y
		val left get() = x
		val right get() = x + width
		val bottom get() = y + height

        fun copyFrom(that: Scissor): Scissor = setTo(that.x, that.y, that.width, that.height)

        fun setTo(x: Int, y: Int, width: Int, height: Int): Scissor = this.apply {
            this.x = x
            this.y = y
            this.width = width
            this.height = height
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

	open inner class Texture : Closeable {
		var isFbo = false
		open val premultiplied = true
		var requestMipmaps = false
		var mipmaps = false; protected set
		var source: BitmapSourceBase = SyncBitmapSource.NIL
		private var uploaded: Boolean = false
		private var generating: Boolean = false
		private var generated: Boolean = false
		private var tempBitmap: Bitmap? = null
		var ready: Boolean = false; private set
		val texId = lastTextureId++

		init {
			createdTextureCount++
		}

		protected fun invalidate() {
			uploaded = false
			generating = false
			generated = false
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

		fun upload(source: BitmapSourceBase, mipmaps: Boolean = false): Texture = this.apply {
			this.source = source
			uploadedSource()
			invalidate()
			this.requestMipmaps = mipmaps
		}

		protected open fun uploadedSource() {
		}

		open fun bind() {
		}

		open fun unbind() {
		}

		fun manualUpload() = this.apply {
			uploaded = true
		}

		fun bindEnsuring() {
			bind()
            if (!isFbo) {
                val source = this.source
                if (!uploaded) {
                    if (!generating) {
                        generating = true
                        when (source) {
                            is SyncBitmapSource -> {
                                tempBitmap = source.gen()
                                generated = true
                            }
                            is AsyncBitmapSource -> {
                                asyncImmediately(source.coroutineContext) {
                                    tempBitmap = source.gen()
                                    generated = true
                                }
                            }
                        }
                    }

                    if (generated) {
                        uploaded = true
                        generating = false
                        generated = false
                        actualSyncUpload(source, tempBitmap, requestMipmaps)
                        tempBitmap = null
                        ready = true
                    }
                }
            }
		}

		open fun actualSyncUpload(source: BitmapSourceBase, bmp: Bitmap?, requestMipmaps: Boolean) {
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
				tempBitmap = null
				deletedTextureCount++
				//Console.log("CLOSED TEXTURE: $texId")
				//printTexStats()
			}
		}

		private fun printTexStats() {
			//Console.log("create=$createdCount, delete=$deletedCount, alive=${createdCount - deletedCount}")
		}
	}

	data class TextureUnit(
		var texture: AG.Texture? = null,
		var linear: Boolean = true
	)

	open class Buffer(val kind: Kind) : Closeable {
		enum class Kind { INDEX, VERTEX }

		var dirty = false
		protected var mem: FBuffer? = null
		protected var memOffset: Int = 0
		protected var memLength: Int = 0

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
			mem = FBuffer(length * 2)
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

		override fun close() {
			mem = null
			memOffset = 0
			memLength = 0
			dirty = true
		}
	}

	enum class DrawType {
		POINTS,
		LINE_STRIP,
		LINE_LOOP,
		LINES,
		TRIANGLES,
		TRIANGLE_STRIP,
		TRIANGLE_FAN,
	}

	val dummyTexture by lazy { createTexture() }

	fun createTexture(): Texture = createTexture(premultiplied = true)
	fun createTexture(bmp: Bitmap, mipmaps: Boolean = false): Texture = createTexture(bmp.premultiplied).upload(bmp, mipmaps)
	fun createTexture(bmp: BitmapSlice<Bitmap>, mipmaps: Boolean = false): Texture =
		createTexture(bmp.premultiplied).upload(bmp, mipmaps)

	fun createTexture(bmp: Bitmap, mipmaps: Boolean = false, premultiplied: Boolean = true): Texture =
		createTexture(premultiplied).upload(bmp, mipmaps)

	open fun createTexture(premultiplied: Boolean): Texture = Texture()
	open fun createBuffer(kind: Buffer.Kind) = Buffer(kind)
	fun createIndexBuffer() = createBuffer(Buffer.Kind.INDEX)
	fun createVertexBuffer() = createBuffer(Buffer.Kind.VERTEX)

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
	}

	enum class TriangleFace {
		FRONT, BACK, FRONT_AND_BACK, NONE;
	}

	enum class CompareMode {
		ALWAYS, EQUAL, GREATER, GREATER_EQUAL, LESS, LESS_EQUAL, NEVER, NOT_EQUAL;
	}

	data class ColorMaskState(
		var red: Boolean = true,
		var green: Boolean = true,
		var blue: Boolean = true,
		var alpha: Boolean = true
	) {
		//val enabled = !red || !green || !blue || !alpha
	}

    enum class FrontFace {
        BOTH, CW, CCW
    }

	data class RenderState(
		var depthFunc: CompareMode = CompareMode.ALWAYS,
		var depthMask: Boolean = true,
		var depthNear: Float = 0f,
		var depthFar: Float = 1f,
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
	)

	private val dummyRenderState = RenderState()
	private val dummyStencilState = StencilState()
	private val dummyColorMaskState = ColorMaskState()

    @Deprecated("")
	fun draw(
		vertices: Buffer,
		program: Program,
		type: DrawType,
		vertexLayout: VertexLayout,
		vertexCount: Int,
		indices: Buffer? = null,
		offset: Int = 0,
		blending: Blending = Blending.NORMAL,
		uniforms: UniformValues = UniformValues.EMPTY,
		stencil: StencilState = dummyStencilState,
		colorMask: ColorMaskState = dummyColorMaskState,
		scissor: Scissor? = null
	) = draw(
        vertices, program, type, vertexLayout, vertexCount, indices, offset, blending,
		uniforms, stencil, colorMask, dummyRenderState, scissor
    )

    fun draw(
        vertices: Buffer,
        program: Program,
        type: DrawType,
        vertexLayout: VertexLayout,
        vertexCount: Int,
        indices: Buffer? = null,
        offset: Int = 0,
        blending: Blending = Blending.NORMAL,
        uniforms: UniformValues = UniformValues.EMPTY,
        stencil: StencilState = dummyStencilState,
        colorMask: ColorMaskState = dummyColorMaskState,
        renderState: RenderState = dummyRenderState,
        scissor: Scissor? = null
    ) = draw(batch.also { batch ->
        batch.vertices = vertices
        batch.program = program
        batch.type = type
        batch.vertexLayout = vertexLayout
        batch.vertexCount = vertexCount
        batch.indices = indices
        batch.offset = offset
        batch.blending = blending
        batch.uniforms = uniforms
        batch.stencil = stencil
        batch.colorMask = colorMask
        batch.renderState = renderState
        batch.scissor = scissor
    })

    class Batch {
        var vertices: Buffer = Buffer(Buffer.Kind.VERTEX)
        var program: Program = DefaultShaders.PROGRAM_DEBUG
        var type: DrawType = DrawType.TRIANGLES
        var vertexLayout: VertexLayout = VertexLayout()
        var vertexCount: Int = 0
        var indices: Buffer? = null
        var offset: Int = 0
        var blending: Blending = Blending.NORMAL
        var uniforms: UniformValues = UniformValues.EMPTY
        var stencil: StencilState = StencilState()
        var colorMask: ColorMaskState = ColorMaskState()
        var renderState: RenderState = RenderState()
        var scissor: Scissor? = null
    }

    private val batch = Batch()

    open fun draw(batch: Batch) {
    }

	protected fun checkBuffers(vertices: AG.Buffer, indices: AG.Buffer?) {
		if (vertices.kind != AG.Buffer.Kind.VERTEX) invalidOp("Not a VertexBuffer")
		if (indices != null && indices.kind != Buffer.Kind.INDEX) invalidOp("Not a IndexBuffer")
	}

	open fun disposeTemporalPerFrameStuff() = Unit

	val frameRenderBuffers = LinkedHashSet<RenderBuffer>()
	val renderBuffers = Pool<RenderBuffer>() { createRenderBuffer() }

	interface BaseRenderBuffer {
		val width: Int
		val height: Int
		fun setSize(width: Int, height: Int)
        fun unset() = Unit
		fun set()
	}

	val mainRenderBuffer = object : BaseRenderBuffer {
		override var width = 128
		override var height = 128

		override fun setSize(width: Int, height: Int) {
			this.width = width
			this.height = height
		}

        override fun unset() {
            unsetBackBuffer(width, height)
        }

        override fun set() {
			setBackBuffer(width, height)
		}
	}

	open inner class RenderBuffer : Closeable, BaseRenderBuffer {
		open val id: Int = -1
		private var cachedTexVersion = -1
		private var _tex: Texture? = null

		val tex: AG.Texture
			get() {
				if (cachedTexVersion != contextVersion) {
					cachedTexVersion = contextVersion
					_tex = this@AG.createTexture(premultiplied = true).manualUpload().apply { isFbo = true }
				}
				return _tex!!
			}

		override var width = 0
		override var height = 0

		protected var dirty = false

		override fun setSize(width: Int, height: Int) {
			this.width = width
			this.height = height
			dirty = true
		}

		override fun set(): Unit = Unit
		fun readBitmap(bmp: Bitmap32) = this@AG.readColor(bmp)
		fun readDepth(width: Int, height: Int, out: FloatArray): Unit = this@AG.readDepth(width, height, out)
		override fun close() = Unit
	}

	open fun createRenderBuffer() = RenderBuffer()

	fun flip() {
		disposeTemporalPerFrameStuff()
		renderBuffers.free(frameRenderBuffers)
		if (frameRenderBuffers.isNotEmpty()) frameRenderBuffers.clear()
		flipInternal()
	}

	protected open fun flipInternal() = Unit

	open fun clear(
		color: RGBA = Colors.TRANSPARENT_BLACK,
		depth: Float = 1f,
		stencil: Int = 0,
		clearColor: Boolean = true,
		clearDepth: Boolean = true,
		clearStencil: Boolean = true
	) = Unit

	@PublishedApi
	internal var currentRenderBuffer: BaseRenderBuffer = mainRenderBuffer

	val renderingToTexture get() = currentRenderBuffer !== mainRenderBuffer

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

    inline
	fun renderToTexture(width: Int, height: Int, render: () -> Unit, use: (tex: Texture) -> Unit = { }) {
		val rb = renderBuffers.alloc()
		frameRenderBuffers += rb
		val oldRenderBuffer = currentRenderBuffer

		rb.setSize(width, height)
		setRenderBuffer(rb)

		try {
			clear(Colors.TRANSPARENT_BLACK) // transparent
			render()
		} finally {
			setRenderBuffer(oldRenderBuffer)
		}

		try {
			use(rb.tex)
		} finally {
			frameRenderBuffers -= rb
			renderBuffers.free(rb)
		}
	}

    inline
    fun renderToBitmap(bmp: Bitmap32, render: () -> Unit) {
        renderToTexture(bmp.width, bmp.height, {
            render()
            readColor(bmp)
        })
    }


    fun setRenderBuffer(renderBuffer: BaseRenderBuffer): BaseRenderBuffer {
		val old = currentRenderBuffer
        currentRenderBuffer.unset()
		currentRenderBuffer = renderBuffer
		renderBuffer.set()
		return old
	}

    open fun unsetBackBuffer(width: Int, height: Int) {
    }

    open fun setBackBuffer(width: Int, height: Int) {
	}

	open fun readColor(bitmap: Bitmap32): Unit = TODO()
	open fun readDepth(width: Int, height: Int, out: FloatArray): Unit = TODO()
	open fun readDepth(out: FloatArray2): Unit = readDepth(out.width, out.height, out.data)
	open fun readColorTexture(texture: Texture, width: Int = backWidth, height: Int = backHeight): Unit = TODO()
	fun readColor() = Bitmap32(backWidth, backHeight).apply { readColor(this) }
	fun readDepth() = FloatArray2(backWidth, backHeight) { 0f }.apply { readDepth(this) }

	inner class TextureDrawer {
		val VERTEX_COUNT = 4
		val vertices = createBuffer(AG.Buffer.Kind.VERTEX)
		val vertexLayout = VertexLayout(DefaultShaders.a_Pos, DefaultShaders.a_Tex)
		val verticesData = FBuffer(VERTEX_COUNT * vertexLayout.totalSize)
		val program = Program(VertexShader {
			DefaultShaders.apply {
				v_Tex setTo a_Tex
				out setTo vec4(a_Pos, 0f.lit, 1f.lit)
			}
		}, FragmentShader {
			DefaultShaders.apply {
				//out setTo vec4(1f, 1f, 0f, 1f)
				out setTo texture2D(u_Tex, v_Tex["xy"])
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
			uniforms[DefaultShaders.u_Tex] = TextureUnit(tex)

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

	val textureDrawer by lazy { TextureDrawer() }
	val flipRenderTexture = true

	fun drawTexture(tex: Texture) {
		textureDrawer.draw(tex, -1f, +1f, +1f, -1f)
	}

	private val drawTempTexture: Texture by lazy { createTexture() }

	fun drawBitmap(bmp: Bitmap) {
		drawTempTexture.upload(bmp, mipmaps = false)
		drawTexture(drawTempTexture)
		drawTempTexture.upload(Bitmaps.transparent)
	}

	class UniformValues() {
		companion object {
			internal val EMPTY = UniformValues()
		}

		private val _uniforms = arrayListOf<Uniform>()
		private val _values = arrayListOf<Any>()
		val uniforms = _uniforms as List<Uniform>

		val keys get() = uniforms
		val values = _values as List<Any>

		val size get() = _uniforms.size

		constructor(vararg pairs: Pair<Uniform, Any>) : this() {
			for (pair in pairs) put(pair.first, pair.second)
		}

		fun clear() {
			_uniforms.clear()
			_values.clear()
		}

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

		fun put(uniform: Uniform, value: Any) {
			for (n in 0 until _uniforms.size) {
				if (_uniforms[n].name == uniform.name) {
					_values[n] = value
					return
				}
			}

			_uniforms.add(uniform)
			_values.add(value)
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

		fun put(uniforms: UniformValues) {
			for (n in 0 until uniforms.size) {
				this.put(uniforms._uniforms[n], uniforms._values[n])
			}
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
