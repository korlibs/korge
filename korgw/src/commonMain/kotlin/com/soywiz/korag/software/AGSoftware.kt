package com.soywiz.korag.software

import com.soywiz.klock.measureTime
import com.soywiz.kmem.FBuffer
import com.soywiz.kmem.clamp
import com.soywiz.korag.AG
import com.soywiz.korag.AGConfig
import com.soywiz.korag.AGFactory
import com.soywiz.korag.AGList
import com.soywiz.korag.AGWindow
import com.soywiz.korag.shader.Attribute
import com.soywiz.korag.shader.Program
import com.soywiz.korag.shader.VarKind
import com.soywiz.korim.bitmap.Bitmap
import com.soywiz.korim.bitmap.Bitmap32
import com.soywiz.korim.bitmap.Bitmaps
import com.soywiz.korim.color.Colors
import com.soywiz.korim.color.RGBA
import com.soywiz.korma.geom.Point
import com.soywiz.korma.geom.PointInt
import com.soywiz.korma.geom.vector.Edge
import com.soywiz.korma.interpolation.interpolate
import kotlin.math.max
import kotlin.math.min

open class AGFactorySoftware() : AGFactory {
	override val supportsNativeFrame: Boolean = false
	override fun create(nativeControl: Any?, config: AGConfig): AG = AGSoftware(nativeControl as? Bitmap32 ?: Bitmap32(640, 480))
	override fun createFastWindow(title: String, width: Int, height: Int): AGWindow {
		TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
	}
}

open class AGSoftware(val bitmap: Bitmap32) : AG() {
    constructor(width: Int, height: Int) : this(Bitmap32(width, height))

	override val nativeComponent: Any = bitmap
    var renderBuffer: SoftwareRenderBuffer? = null
    var currentTexture: SoftwareTexture? = null

    inner class SoftwareTexture(premultiplied: Boolean) : AG.Texture(premultiplied) {
        var bitmap: Bitmap = Bitmaps.transparent.bmp

        override fun uploadedSource() {
        }

        override fun bind() {
            currentTexture = this
        }

        override fun unbind() {
            currentTexture = null
        }

        override fun actualSyncUpload(source: BitmapSourceBase, bmps: List<Bitmap?>?, requestMipmaps: Boolean) {
            bitmap = bmps?.firstOrNull() ?: bitmap
        }

        override fun close() {
        }
    }

    inner class SoftwareRenderBuffer : RenderBuffer() {
        var bitmap = Bitmap32(1, 1)

        override fun setSize(x: Int, y: Int, width: Int, height: Int, fullWidth: Int, fullHeight: Int) {
            bitmap = Bitmap32(width, height)
        }

        override fun set() {
            renderBuffer = this
        }
    }

    inner class SoftwareBuffer(list: AGList) : Buffer( list) {
        val memory: FBuffer? get() = mem
        override fun afterSetMem() {
        }
    }

    override fun createBuffer(): Buffer = commandsNoWait { SoftwareBuffer(it) }
    override fun createMainRenderBuffer(): BaseRenderBuffer = SoftwareRenderBuffer()
    override fun createRenderBuffer(): RenderBuffer = SoftwareRenderBuffer()

    override fun createTexture(premultiplied: Boolean, targetKind: TextureTargetKind): Texture = SoftwareTexture(premultiplied)

    fun readIndices(batch: Batch): IntArray {
        val indices = batch.indices as? SoftwareBuffer ?: return IntArray(batch.vertexCount) { it }
        val memory = indices.memory ?: TODO()
        return when (batch.indexType) {
            IndexType.USHORT -> IntArray(batch.vertexCount) { memory.getAlignedUInt16(it) }
            else -> TODO("${batch.indexType}")
        }
    }

    override fun draw(batch: Batch) {
        val time = measureTime {
            drawInternal(batch)
        }
        println("DRAWN IN time=$time")
    }

    data class CompiledProgram(val program: Program) {
        val programAllocator = ShaderAllocator().also {
            it.allocateVaryingUniform(program)
        }
        val vertexShader = program.vertex.toSGVM(programAllocator)
        val fragmentShader = program.fragment.toSGVM(programAllocator)
    }

    private val compiledPrograms = LinkedHashMap<Program, CompiledProgram>()

    fun drawInternal(batch: Batch) {
        // Read indices
        val indices = readIndices(batch)

        val program = compiledPrograms.getOrPut(batch.program) { CompiledProgram(batch.program) }
        val vertexShader = program.vertexShader
        val fragmentShader = program.fragmentShader

        fragmentShader.tex2d = { sampler: Int, x: Float, y: Float, out: FloatArray, outIndex: Int ->
            val textures = fragmentShader.textures
            //val tex = textures[sampler]
            val tex = textures[47]
            val bmp = tex?.bitmap
            val bmpWidth = bmp?.width ?: 0
            val bmpHeight = bmp?.height ?: 0
            val tx = (x * (bmpWidth - 1))
            val ty = (y * (bmpHeight - 1))
            val colorA = if (bmp != null) bmp.getRgbaSampled(tx.toFloat(), ty.toFloat()) else Colors.FUCHSIA
            //val colorA = if (bmp != null) bmp.getRgbaClampedBorder(tx.toInt(), ty.toInt()) else Colors.FUCHSIA
            val color = RGBA.mixRgba(colorA, Colors.FUCHSIA, 0.4)
            //val color = colorA
            out[outIndex + 0] = color.rf
            out[outIndex + 1] = color.gf
            out[outIndex + 2] = color.bf
            out[outIndex + 3] = color.af
            //println("tex2d: sampler=$sampler, x=$x, y=$y, outIndex=$outIndex")
        }

        data class CachedAttribute(val attribute: Attribute, val offset: Int, val program: SGVM) {
            val bytesSize get() = attribute.type.bytesSize
            val type get() = attribute.type
            val kind get() = attribute.type.kind
            val arrayCount get() = attribute.arrayCount
            val elementCount get() = attribute.elementCount
            val programOffset = vertexShader.getAllocation(attribute)?.index ?: error("Can't find attribute $attribute")
            val normalized = attribute.normalized
            val normalizedRatio = when (kind) {
                VarKind.TUNSIGNED_BYTE -> 0xFF.toFloat()
                else -> 1f
            }
        }
        data class CachedVertexData(val data: VertexData, val program: SGVM) {
            val buffer = data.buffer as SoftwareBuffer
            val memory = buffer.memory!!
            val layout = data.layout
            val totalSize = layout.totalSize
            val attributes = layout.attributes.zip(layout.attributePositions).map { CachedAttribute(it.first, it.second, vertexShader) }
        }

        val vertexData = batch.vertexData.map { CachedVertexData(it, vertexShader) }

        println("------------------------------------------------------------------------------------------------------------")

        val vshaders = Array(3) { vertexShader.clone()}

        // Set uniforms
        run {
            batch.uniforms.fastForEach { uniform, value ->
                val position = vertexShader.getAllocation(uniform)
                if (position != null) {
                    fragmentShader.setUniform(position.index, uniform, value)
                    for (shader in vshaders) {
                        shader.setUniform(position.index, uniform, value)
                    }
                }
            }
        }

        //println(batch.program.vertex.stm)
        //println("varyings=${programAllocator.varyings}")
        //println("output=${vertexShader.allocator.output}")

        if (batch.type != AG.DrawType.TRIANGLES) TODO("Unsupported ${batch.type}")

        var n = 0
        var triangleCount = 0
        val colors = arrayOf(Colors.RED, Colors.BLUE)

        val fragmentOutputIndex = fragmentShader.allocator.output.allocation.index

        // Execute vertex programs
        for (vertexId in 0 until batch.vertexCount) {
            val index = indices[vertexId]
            val shader = vshaders[n++]
            for (vertex in vertexData) {
                val mem = vertex.memory
                val totalSize = vertex.totalSize
                val startVertex = index * totalSize
                for (attrib in vertex.attributes) {
                    val startAttribute = startVertex + attrib.offset
                    val elementBytesSize = attrib.kind.bytesSize
                    val normalized = attrib.normalized
                    val elementCount = attrib.elementCount
                    if (attrib.arrayCount != 1) TODO("attrib.arrayCount=${attrib.arrayCount}")
                    for (n in 0 until elementCount) {
                        val noffset = startAttribute + elementBytesSize * n
                        val value = when (attrib.type.kind) {
                            VarKind.TUNSIGNED_BYTE -> mem.getUnalignedUInt8(noffset).toFloat()
                            VarKind.TFLOAT -> mem.getUnalignedFloat32(noffset)
                            else -> TODO("${attrib.type}")
                        }
                        val attribPos = attrib.programOffset + n
                        shader.setF(attribPos, if (normalized) value / attrib.normalizedRatio else value)
                    }
                    //println("attrib=$attrib, elementCount=$elementCount, startAttribute=$startAttribute, bytesSize=$elementBytesSize, totalSize=$totalSize, arrayCount=${attrib.arrayCount}, values=${(0 until elementCount).map { shader.freg[attrib.programOffset + it] }}")

                }
                //println("vertex=$vertex")
            }
            shader.execute()
            //return
            if (n == 3) {
                n = 0
                println("EMIT!")
                //println(programAllocator.varyings)
                //for (varying in programAllocator.varyingIndices) println("varying=$varying, data=${(0 until 3).map { vshaders[it].freg[varying] }}")
                val outputIndex = program.programAllocator.output.allocation.index
                val p0 = IndexedPoint(0, coordToRenderPixels(vshaders[0].freg[outputIndex], vshaders[0].freg[outputIndex + 1]))
                val p1 = IndexedPoint(1, coordToRenderPixels(vshaders[1].freg[outputIndex], vshaders[1].freg[outputIndex + 1]))
                val p2 = IndexedPoint(2, coordToRenderPixels(vshaders[2].freg[outputIndex], vshaders[2].freg[outputIndex + 1]))

                println(program.programAllocator.varyings)
                for (varying in program.programAllocator.varyingIndices) {
                    println("varying=$varying, data=${(0 until 3).map {  vshaders[it].freg[varying] }}")
                }

                //val color = colors.getCyclic(triangleCount)
                drawScanlines(p0, p1, p2) { y, a, b, a0, a1, a2, b0, b1, b2 ->
                    val total = (b - a) + 1
                    for (x in clipX(a)..clipY(b)) {
                        val n = x - a
                        val ratio = (n.toFloat() / total).toDouble()
                        val r0 = ratio.interpolate(a0, b0)
                        val r1 = ratio.interpolate(a1, b1)
                        val r2 = ratio.interpolate(a2, b2)
                        for (varying in program.programAllocator.varyingIndices) {
                            val v0 = vshaders[0].freg[varying]
                            val v1 = vshaders[1].freg[varying]
                            val v2 = vshaders[2].freg[varying]
                            fragmentShader.freg[varying] = (v0 * r0) + (v1 * r1) + (v2 * r2)
                        }
                        //println("[$x, $y] ${fragmentShader.freg[program.programAllocator.varyingIndices[0]]},${fragmentShader.freg[program.programAllocator.varyingIndices[1]]}")
                        //println("FRAGMENT!")
                        fragmentShader.execute()
                        if (!fragmentShader.discard) {
                            val rf = fragmentShader.freg[fragmentOutputIndex + 0]
                            val gf = fragmentShader.freg[fragmentOutputIndex + 1]
                            val bf = fragmentShader.freg[fragmentOutputIndex + 2]
                            val af = fragmentShader.freg[fragmentOutputIndex + 3]
                            val color = RGBA.float(rf, gf, bf, af)

                            // @TODO: BLENDING
                            // Copy Output to the bitmap (missing blending)
                            bitmap[a + n, bitmap.height - y - 1] = color
                            //bitmap[a + n, y] = color
                        }
                    }
                }
                println("SCREEN[${bitmap.width}, ${bitmap.height}] = TRIANGLE[$p0, $p1, $p2]")
                triangleCount++
                //break
            }
        }
        println("WARNNING. AGSoftware.draw not implemented")
    }

    // @TODO: Use scissor testing
    private fun clipX(x: Int): Int = x.clamp(0, bitmap.width - 1)
    private fun clipY(y: Int): Int = y.clamp(0, bitmap.height - 1)

    @PublishedApi
    internal val tempPoints = Array(3) { IndexedPoint(0, PointInt()) }

    data class IndexedPoint(val index: Int, val pi: PointInt) {
        val p get() = pi.p
        val x get() = pi.x
        val y get() = pi.y
    }

    @Suppress("EXPERIMENTAL_API_USAGE")
    private inline fun drawScanlines(
        p0: IndexedPoint,
        p1: IndexedPoint,
        p2: IndexedPoint,
        block: (
            y: Int, a: Int, b: Int,
            a0: Float, a1: Float, a2: Float,
            b0: Float, b1: Float, b2: Float,
        ) -> Unit
    ) {
        //assert(p0.y <= p1.y)
        //assert(p1.y <= p2.y)

        tempPoints[0] = p0
        tempPoints[1] = p1
        tempPoints[2] = p2
        tempPoints.sortWith { a, b ->
            val dy = a.y - b.y
            if (dy == 0) a.x - b.x else dy
        }

        val tp = Point()
        val edge0 = Edge(tempPoints[0].pi, tempPoints[1].pi)
        val edge1 = Edge(tempPoints[0].pi, tempPoints[2].pi)
        val edge2 = Edge(tempPoints[1].pi, tempPoints[2].pi)
        for (n in 0 until 2) {
            // top triangle

            val y0 = if (n == 0) tempPoints[0].y else tempPoints[1].y
            val y1 = if (n == 0) tempPoints[1].y else tempPoints[2].y

            val e0 = if (n == 0) edge0 else edge1
            val e1 = if (n == 0) edge1 else edge2

            for (y in clipY(y0)..clipY(y1)) {
                val x0 = e0.intersectX(y)
                val x1 = e1.intersectX(y)

                tp.setTo(x0, y)
                val a0 = tp.distanceTo(p0.p).toFloat()
                val a1 = tp.distanceTo(p1.p).toFloat()
                val a2 = tp.distanceTo(p2.p).toFloat()
                val aSum = a0 + a1 + a2
                val aScale = 1f / aSum

                tp.setTo(x1, y)
                val b0 = tp.distanceTo(p0.p).toFloat()
                val b1 = tp.distanceTo(p1.p).toFloat()
                val b2 = tp.distanceTo(p2.p).toFloat()
                val bSum = b0 + b1 + b2
                val bScale = 1f / bSum

                val aa0 = 1f - a0 * aScale
                val aa1 = 1f - a1 * aScale
                val aa2 = 1f - a2 * aScale
                val aaSum = aa0 + aa1 + aa2
                val aaScale = 1f / aaSum

                val bb0 = 1f - b0 * bScale
                val bb1 = 1f - b1 * bScale
                val bb2 = 1f - b2 * bScale
                val bbSum = bb0 + bb1 + bb2
                val bbScale = 1f / bbSum

                block(
                    y,
                    min(x0, x1), max(x0, x1),
                    aa0 * aaScale, aa1 * aaScale, aa2 * aaScale,
                    bb0 * bbScale, bb1 * bbScale, bb2 * bbScale
                )
            }
        }
    }

    fun coordToRenderPixels(x: Float, y: Float, out: PointInt = PointInt()): PointInt {
        return out.setTo((((x + 1f) * .5f) * bitmap.width).toInt(), (((y + 1f) * .5f) * bitmap.height).toInt())
    }

    override fun readColor(bitmap: Bitmap32, x: Int, y: Int) {
        println("WARNNING. AGSoftware.readColor not implemented")
    }
    override fun readDepth(width: Int, height: Int, out: FloatArray) {
        println("WARNNING. AGSoftware.readDepth not implemented")
    }
    override fun readColorTexture(texture: Texture, x: Int, y: Int, width: Int, height: Int) {
        TODO("readColorTexture")
    }

}
