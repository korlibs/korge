package com.soywiz.korag

import com.soywiz.kds.Extra
import com.soywiz.kds.FastArrayList
import com.soywiz.kds.FloatArray2
import com.soywiz.kds.FloatArrayList
import com.soywiz.kds.IntArrayList
import com.soywiz.kds.Pool
import com.soywiz.kds.fastArrayListOf
import com.soywiz.kds.fastCastTo
import com.soywiz.kds.iterators.fastForEach
import com.soywiz.klock.measureTime
import com.soywiz.klogger.Console
import com.soywiz.kmem.*
import com.soywiz.kmem.unit.ByteUnits
import com.soywiz.korag.annotation.KoragExperimental
import com.soywiz.korag.gl.fromGl
import com.soywiz.korag.shader.Attribute
import com.soywiz.korag.shader.FragmentShader
import com.soywiz.korag.shader.Program
import com.soywiz.korag.shader.ProgramConfig
import com.soywiz.korag.shader.Uniform
import com.soywiz.korag.shader.VarType
import com.soywiz.korag.shader.VertexLayout
import com.soywiz.korag.shader.VertexShader
import com.soywiz.korag.shader.gl.GlslGenerator
import com.soywiz.korim.bitmap.Bitmap
import com.soywiz.korim.bitmap.Bitmap32
import com.soywiz.korim.bitmap.Bitmap8
import com.soywiz.korim.bitmap.BitmapSlice
import com.soywiz.korim.bitmap.Bitmaps
import com.soywiz.korim.bitmap.ForcedTexId
import com.soywiz.korim.color.Colors
import com.soywiz.korim.color.RGBA
import com.soywiz.korim.color.RGBAPremultiplied
import com.soywiz.korim.color.RGBAf
import com.soywiz.korio.annotations.KorIncomplete
import com.soywiz.korio.async.runBlockingNoJs
import com.soywiz.korio.lang.*
import com.soywiz.korma.geom.*
import com.soywiz.korma.math.nextMultipleOf
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.jvm.JvmInline
import kotlin.jvm.JvmOverloads
import kotlin.math.*


inline class AGReadKind(val ordinal: Int) {
    companion object {
        val COLOR = AGReadKind(0)
        val DEPTH = AGReadKind(1)
        val STENCIL = AGReadKind(2)
    }
    val size: Int get() = when (this) {
        COLOR -> 4
        DEPTH -> 4
        STENCIL -> 1
        else -> unreachable
    }

    override fun toString(): String = when (this) {
        COLOR -> "COLOR"
        DEPTH -> "DEPTH"
        STENCIL -> "STENCIL"
        else -> "-"
    }
}

//TODO: there are other possible values
enum class AGTextureTargetKind(val dims: Int) {
    TEXTURE_2D(2), TEXTURE_3D(3), TEXTURE_CUBE_MAP(3), EXTERNAL_TEXTURE(2);
    companion object {
        val VALUES = values()
    }
}


/** 2 bits required for encoding */
inline class AGBlendEquation(val ordinal: Int) {
    companion object {
        val ADD = AGBlendEquation(0)
        val SUBTRACT = AGBlendEquation(1)
        val REVERSE_SUBTRACT = AGBlendEquation(2)
    }

    override fun toString(): String = when (this) {
        ADD -> "ADD"
        SUBTRACT -> "SUBTRACT"
        REVERSE_SUBTRACT -> "REVERSE_SUBTRACT"
        else -> "-"
    }

    val op: String get() = when (this) {
        ADD -> "+"
        SUBTRACT -> "-"
        REVERSE_SUBTRACT -> "r-"
        else -> unreachable
    }

    fun apply(l: Double, r: Double): Double = when (this) {
        ADD -> l + r
        SUBTRACT -> l - r
        REVERSE_SUBTRACT -> r - l
        else -> unreachable
    }

    fun apply(l: Float, r: Float): Float = when (this) {
        ADD -> l + r
        SUBTRACT -> l - r
        REVERSE_SUBTRACT -> r - l
        else -> unreachable
    }

    fun apply(l: Int, r: Int): Int = when (this) {
        ADD -> l + r
        SUBTRACT -> l - r
        REVERSE_SUBTRACT -> r - l
        else -> unreachable
    }
}

/** 4 bits required for encoding */
inline class AGBlendFactor(val ordinal: Int) {
    companion object {
        val DESTINATION_ALPHA = AGBlendFactor(0)
        val DESTINATION_COLOR = AGBlendFactor(1)
        val ONE = AGBlendFactor(2)
        val ONE_MINUS_DESTINATION_ALPHA = AGBlendFactor(3)
        val ONE_MINUS_DESTINATION_COLOR = AGBlendFactor(4)
        val ONE_MINUS_SOURCE_ALPHA = AGBlendFactor(5)
        val ONE_MINUS_SOURCE_COLOR = AGBlendFactor(6)
        val SOURCE_ALPHA = AGBlendFactor(7)
        val SOURCE_COLOR = AGBlendFactor(8)
        val ZERO = AGBlendFactor(9)
    }

    override fun toString(): String = when (this) {
        DESTINATION_ALPHA -> "DESTINATION_ALPHA"
        DESTINATION_COLOR -> "DESTINATION_COLOR"
        ONE -> "ONE"
        ONE_MINUS_DESTINATION_ALPHA -> "ONE_MINUS_DESTINATION_ALPHA"
        ONE_MINUS_DESTINATION_COLOR -> "ONE_MINUS_DESTINATION_COLOR"
        ONE_MINUS_SOURCE_ALPHA -> "ONE_MINUS_SOURCE_ALPHA"
        ONE_MINUS_SOURCE_COLOR -> "ONE_MINUS_SOURCE_COLOR"
        SOURCE_ALPHA -> "SOURCE_ALPHA"
        SOURCE_COLOR -> "SOURCE_COLOR"
        ZERO -> "ZERO"
        else -> "-"
    }

    val op: String get() = when (this) {
        DESTINATION_ALPHA -> "dstA"
        DESTINATION_COLOR -> "dstRGB"
        ONE -> "1"
        ONE_MINUS_DESTINATION_ALPHA -> "(1 - dstA)"
        ONE_MINUS_DESTINATION_COLOR -> "(1 - dstRGB)"
        ONE_MINUS_SOURCE_ALPHA -> "(1 - srcA)"
        ONE_MINUS_SOURCE_COLOR -> "(1 - srcRGB)"
        SOURCE_ALPHA -> "srcA"
        SOURCE_COLOR -> "srcRGB"
        ZERO -> "0"
        else -> unreachable
    }

    fun get(srcC: Double, srcA: Double, dstC: Double, dstA: Double): Double = when (this) {
        DESTINATION_ALPHA -> dstA
        DESTINATION_COLOR -> dstC
        ONE -> 1.0
        ONE_MINUS_DESTINATION_ALPHA -> 1.0 - dstA
        ONE_MINUS_DESTINATION_COLOR -> 1.0 - dstC
        ONE_MINUS_SOURCE_ALPHA -> 1.0 - srcA
        ONE_MINUS_SOURCE_COLOR -> 1.0 - srcC
        SOURCE_ALPHA -> srcA
        SOURCE_COLOR -> srcC
        ZERO -> 0.0
        else -> unreachable
    }
}

inline class AGStencilOp(val ordinal: Int) {
    companion object {
        val DECREMENT_SATURATE = AGStencilOp(0)
        val DECREMENT_WRAP = AGStencilOp(1)
        val INCREMENT_SATURATE = AGStencilOp(2)
        val INCREMENT_WRAP = AGStencilOp(3)
        val INVERT = AGStencilOp(4)
        val KEEP = AGStencilOp(5)
        val SET = AGStencilOp(6)
        val ZERO = AGStencilOp(7)
    }

    override fun toString(): String = when (this) {
        DECREMENT_SATURATE -> "DECREMENT_SATURATE"
        DECREMENT_WRAP -> "DECREMENT_WRAP"
        INCREMENT_SATURATE -> "INCREMENT_SATURATE"
        INCREMENT_WRAP -> "INCREMENT_WRAP"
        INVERT -> "INVERT"
        KEEP -> "KEEP"
        SET -> "SET"
        ZERO -> "ZERO"
        else -> "-"
    }
}


/** 2 bits required for encoding */
inline class AGTriangleFace(val ordinal :Int) {
    companion object {
        val FRONT = AGTriangleFace(0)
        val BACK = AGTriangleFace(1)
        val FRONT_AND_BACK = AGTriangleFace(2)
        val NONE = AGTriangleFace(3)
    }

    override fun toString(): String = when (this) {
        FRONT -> "FRONT"
        BACK -> "BACK"
        FRONT_AND_BACK -> "FRONT_AND_BACK"
        NONE -> "NONE"
        else -> "-"
    }
}


/** 3 bits required for encoding */
inline class AGCompareMode(val ordinal: Int) {
    companion object {
        val ALWAYS = AGCompareMode(0)
        val EQUAL = AGCompareMode(1)
        val GREATER = AGCompareMode(2)
        val GREATER_EQUAL = AGCompareMode(3)
        val LESS = AGCompareMode(4)
        val LESS_EQUAL = AGCompareMode(5)
        val NEVER = AGCompareMode(6)
        val NOT_EQUAL = AGCompareMode(7)
    }

    override fun toString(): String = when (this) {
        ALWAYS -> "ALWAYS"
        EQUAL -> "EQUAL"
        GREATER -> "GREATER"
        GREATER_EQUAL -> "GREATER_EQUAL"
        LESS -> "LESS"
        LESS_EQUAL -> "LESS_EQUAL"
        NEVER -> "NEVER"
        NOT_EQUAL -> "NOT_EQUAL"
        else -> "-"
    }

    fun inverted(): AGCompareMode = when (this) {
        ALWAYS -> NEVER
        EQUAL -> NOT_EQUAL
        GREATER -> LESS_EQUAL
        GREATER_EQUAL -> LESS
        LESS -> GREATER_EQUAL
        LESS_EQUAL -> GREATER
        NEVER -> ALWAYS
        NOT_EQUAL -> EQUAL
        else -> NEVER
    }
}

// Default: CCW
/** 2 Bits required for encoding */
inline class AGFrontFace(val ordinal: Int) {
    companion object {
        val DEFAULT: AGFrontFace get() = CCW

        // @TODO: This is incorrect
        val BOTH = AGFrontFace(0)
        val CCW = AGFrontFace(1)
        val CW = AGFrontFace(2)
    }

    override fun toString(): String = when (this) {
        BOTH -> "BOTH"
        CCW -> "CCW"
        CW -> "CW"
        else -> "-"
    }
}


/** 2 Bits required for encoding */
inline class AGCullFace(val ordinal: Int) {
    companion object {
        val BOTH = AGCullFace(0)
        val FRONT = AGCullFace(1)
        val BACK = AGCullFace(2)
    }

    override fun toString(): String = when (this) {
        BOTH -> "BOTH"
        FRONT -> "FRONT"
        BACK -> "BACK"
        else -> "-"
    }
}


/** Encoded in 3 bits */
inline class AGDrawType(val ordinal: Int) {
    companion object {
        val POINTS = AGDrawType(0)
        val LINE_STRIP = AGDrawType(1)
        val LINE_LOOP = AGDrawType(2)
        val LINES = AGDrawType(3)
        val TRIANGLES = AGDrawType(4)
        val TRIANGLE_STRIP = AGDrawType(5)
        val TRIANGLE_FAN = AGDrawType(6)
    }

    override fun toString(): String = when (this) {
        POINTS -> "POINTS"
        LINE_STRIP -> "LINE_STRIP"
        LINE_LOOP -> "LINE_LOOP"
        LINES -> "LINES"
        TRIANGLES -> "TRIANGLES"
        TRIANGLE_STRIP -> "TRIANGLE_STRIP"
        TRIANGLE_FAN -> "TRIANGLE_FAN"
        else -> "-"
    }
}

/** Encoded in 2 bits */
inline class AGIndexType(val ordinal: Int) {
    companion object {
        val NONE = AGIndexType(0)
        val UBYTE = AGIndexType(1)
        val USHORT = AGIndexType(2)
        // https://developer.mozilla.org/en-US/docs/Web/API/WebGLRenderingContext/drawElements
        @Deprecated("UINT is not always supported on webgl")
        val UINT = AGIndexType(3)
    }

    override fun toString(): String = when (this) {
        NONE -> "null"
        UBYTE -> "UBYTE"
        USHORT -> "USHORT"
        UINT -> "UINT"
        else -> "-"
    }
}


/**
 * color(RGB) = (sourceColor * [srcRGB]) + (destinationColor * [dstRGB])
 * color(A) = (sourceAlpha * [srcA]) + (destinationAlpha * [dstA])
 *
 * Instead of + [eqRGB] and [eqA] determines the operation to use (+, - or reversed -)
 */
inline class AGBlending(val data: Int) {
    val srcRGB: AGBlendFactor get() = AGBlendFactor(data.extract4(0))
    val srcA: AGBlendFactor get() = AGBlendFactor(data.extract4(4))
    val dstRGB: AGBlendFactor get() = AGBlendFactor(data.extract4(8))
    val dstA: AGBlendFactor get() = AGBlendFactor(data.extract4(12))
    val eqRGB: AGBlendEquation get() = AGBlendEquation(data.extract2(16))
    val eqA: AGBlendEquation get() = AGBlendEquation(data.extract2(18))

    fun withSRC(rgb: AGBlendFactor, a: AGBlendFactor = rgb): AGBlending = AGBlending(data.insert4(rgb.ordinal, 0).insert4(a.ordinal, 4))
    fun withDST(rgb: AGBlendFactor, a: AGBlendFactor = rgb): AGBlending = AGBlending(data.insert4(rgb.ordinal, 8).insert4(a.ordinal, 12))
    fun withEQ(rgb: AGBlendEquation, a: AGBlendEquation = rgb): AGBlending = AGBlending(data.insert2(rgb.ordinal, 16).insert2(a.ordinal, 18))

    private fun applyColorComponent(srcC: Double, dstC: Double, srcA: Double, dstA: Double): Double {
        return this.eqRGB.apply(srcC * this.srcRGB.get(srcC, srcA, dstC, dstA), dstC * this.dstRGB.get(srcC, srcA, dstC, dstA))
    }

    private fun applyAlphaComponent(srcA: Double, dstA: Double): Double {
        return eqRGB.apply(srcA * this.srcA.get(0.0, srcA, 0.0, dstA), dstA * this.dstA.get(0.0, srcA, 0.0, dstA))
    }

    fun apply(src: RGBAf, dst: RGBAf, out: RGBAf = RGBAf()): RGBAf {
        out.rd = applyColorComponent(src.rd, dst.rd, src.ad, dst.ad)
        out.gd = applyColorComponent(src.gd, dst.gd, src.ad, dst.ad)
        out.bd = applyColorComponent(src.bd, dst.bd, src.ad, dst.ad)
        out.ad = applyAlphaComponent(src.ad, dst.ad)
        return out
    }

    fun apply(src: RGBA, dst: RGBA): RGBA {
        val srcA = src.ad
        val dstA = dst.ad
        val r = applyColorComponent(src.rd, dst.rd, srcA, dstA)
        val g = applyColorComponent(src.gd, dst.gd, srcA, dstA)
        val b = applyColorComponent(src.bd, dst.bd, srcA, dstA)
        val a = applyAlphaComponent(srcA, dstA)
        return RGBA.float(r, g, b, a)
    }

    val disabled: Boolean get() = this == NONE
    val enabled: Boolean get() = this != NONE

    override fun toString(): String = "Blending(outRGB = (srcRGB * ${srcRGB.op}) ${eqRGB.op} (dstRGB * ${dstRGB.op}), outA = (srcA * ${srcA.op}) ${eqA.op} (dstA * ${dstA.op}))"

    companion object {
        operator fun invoke(
            srcRGB: AGBlendFactor,
            dstRGB: AGBlendFactor,
            srcA: AGBlendFactor = srcRGB,
            dstA: AGBlendFactor = dstRGB,
            eqRGB: AGBlendEquation = AGBlendEquation.ADD,
            eqA: AGBlendEquation = eqRGB
        ): AGBlending = AGBlending(0).withSRC(srcRGB, srcA).withDST(dstRGB, dstA).withEQ(eqRGB, eqA)

        operator fun invoke(
            src: AGBlendFactor,
            dst: AGBlendFactor,
            eq: AGBlendEquation = AGBlendEquation.ADD,
        ): AGBlending = AGBlending(0).withSRC(src).withDST(dst).withEQ(eq)

        val NONE = AGBlending(AGBlendFactor.ONE, AGBlendFactor.ZERO, AGBlendFactor.ONE, AGBlendFactor.ZERO)
        val NORMAL = AGBlending(
            //GL_ONE, GL_ONE_MINUS_SRC_ALPHA <-- premultiplied
            AGBlendFactor.SOURCE_ALPHA, AGBlendFactor.ONE_MINUS_SOURCE_ALPHA,
            AGBlendFactor.ONE, AGBlendFactor.ONE_MINUS_SOURCE_ALPHA
        )
        val NORMAL_PRE = AGBlending(
            AGBlendFactor.ONE, AGBlendFactor.ONE_MINUS_SOURCE_ALPHA,
        )
        val ADD = AGBlending(
            AGBlendFactor.SOURCE_ALPHA, AGBlendFactor.DESTINATION_ALPHA,
            AGBlendFactor.ONE, AGBlendFactor.ONE
        )
        val ADD_PRE = AGBlending(
            AGBlendFactor.ONE, AGBlendFactor.ONE,
            AGBlendFactor.ONE, AGBlendFactor.ONE
        )
    }
}

inline class AGColorMaskState(
    val data: Int
) {
    val red: Boolean get() = data.extractBool(0)
    val green: Boolean get() = data.extractBool(1)
    val blue: Boolean get() = data.extractBool(2)
    val alpha: Boolean get() = data.extractBool(3)

    constructor(red: Boolean, green: Boolean, blue: Boolean, alpha: Boolean) : this(0.insert(red, 0).insert(green, 1).insert(blue, 2).insert(alpha, 3))
    constructor(value: Boolean = true) : this(value, value, value, value)

    fun copy(
        red: Boolean = this.red,
        green: Boolean = this.green,
        blue: Boolean = this.blue,
        alpha: Boolean = this.alpha
    ): AGColorMaskState = AGColorMaskState(red, green, blue, alpha)

    companion object {
        internal val DUMMY = AGColorMaskState()
        val ALL_ENABLED = AGColorMaskState(true)
        val ALL_BUT_ALPHA_ENABLED = AGColorMaskState(true, true, true, false)
        val ALL_DISABLED = AGColorMaskState(false)
    }
}

inline class AGRenderState(val data: Int) {
    companion object {
        operator fun invoke(): AGRenderState = DEFAULT
        val DEFAULT = AGRenderState(0).withDepth(0f, 1f).withDepthMask(true).withDepthFunc(AGCompareMode.ALWAYS).withFrontFace(AGFrontFace.BOTH)
    }

    val depthNear: Float get() = data.extractScaledf01(0, 12)
    val depthFar: Float get() = data.extractScaledf01(12, 12)
    val depthMask: Boolean get() = data.extractBool(24)
    val depthFunc: AGCompareMode get() = AGCompareMode(data.extract3(26))
    val frontFace: AGFrontFace get() = AGFrontFace(data.extract2(30))

    fun withDepth(near: Float, far: Float): AGRenderState = AGRenderState(data.insertScaledf01(near, 0, 12).insertScaledf01(far, 12, 12))
    fun withDepthMask(depthMask: Boolean): AGRenderState = AGRenderState(data.insert(depthMask, 24))
    fun withDepthFunc(depthFunc: AGCompareMode): AGRenderState = AGRenderState(data.insert3(depthFunc.ordinal, 26))
    fun withFrontFace(frontFace: AGFrontFace): AGRenderState = AGRenderState(data.insert2(frontFace.ordinal, 30))
}

inline class AGStencilFullState private constructor(private val data: Long) {
    constructor(opFunc: AGStencilOpFuncState = AGStencilOpFuncState.DEFAULT, ref: AGStencilReferenceState = AGStencilReferenceState.DEFAULT) : this(Long.fromLowHigh(opFunc.data, ref.data))
    val opFunc: AGStencilOpFuncState get() = AGStencilOpFuncState(data.low)
    val ref: AGStencilReferenceState get() = AGStencilReferenceState(data.high)

    fun withOpFunc(opFunc: AGStencilOpFuncState): AGStencilFullState = AGStencilFullState(opFunc, ref)
    fun withRef(ref: AGStencilReferenceState): AGStencilFullState = AGStencilFullState(opFunc, ref)
    fun withReferenceValue(referenceValue: Int): AGStencilFullState = withRef(ref.withReferenceValue(referenceValue))
}

inline class AGStencilReferenceState(val data: Int) {
    companion object {
        val DEFAULT = AGStencilReferenceState(0)
            .withReferenceValue(0)
            .withReadMask(0xFF)
            .withWriteMask(0xFF)
    }

    val referenceValue: Int get() = data.extract8(0)
    val readMask: Int get() = data.extract8(8)
    val writeMask: Int get() = data.extract8(16)

    fun withReferenceValue(referenceValue: Int): AGStencilReferenceState = AGStencilReferenceState(data.insert8(referenceValue, 0))
    fun withReadMask(readMask: Int): AGStencilReferenceState = AGStencilReferenceState(data.insert8(readMask, 8))
    fun withWriteMask(writeMask: Int): AGStencilReferenceState = AGStencilReferenceState(data.insert8(writeMask, 16))
}

inline class AGStencilOpFuncState(val data: Int) {
    companion object {
        val DEFAULT = AGStencilOpFuncState(0)
            .withEnabled(false)
            .withTriangleFace(AGTriangleFace.FRONT_AND_BACK)
            .withCompareMode(AGCompareMode.ALWAYS)
            .withAction(AGStencilOp.KEEP, AGStencilOp.KEEP, AGStencilOp.KEEP)
    }

    val enabled: Boolean get() = data.extractBool(0)
    val triangleFace: AGTriangleFace get() = AGTriangleFace(data.extract2(4))
    val compareMode: AGCompareMode get() = AGCompareMode(data.extract3(8))
    val actionOnBothPass: AGStencilOp get() = AGStencilOp(data.extract3(12))
    val actionOnDepthFail: AGStencilOp get() = AGStencilOp(data.extract3(16))
    val actionOnDepthPassStencilFail: AGStencilOp get() = AGStencilOp(data.extract3(20))

    fun withEnabled(enabled: Boolean): AGStencilOpFuncState = AGStencilOpFuncState(data.insert(enabled, 0))
    fun withTriangleFace(triangleFace: AGTriangleFace): AGStencilOpFuncState = AGStencilOpFuncState(data.insert2(triangleFace.ordinal, 4))
    fun withCompareMode(compareMode: AGCompareMode): AGStencilOpFuncState = AGStencilOpFuncState(data.insert3(compareMode.ordinal, 8))
    fun withActionOnBothPass(actionOnBothPass: AGStencilOp): AGStencilOpFuncState = AGStencilOpFuncState(data.insert3(actionOnBothPass.ordinal, 12))
    fun withActionOnDepthFail(actionOnDepthFail: AGStencilOp): AGStencilOpFuncState = AGStencilOpFuncState(data.insert3(actionOnDepthFail.ordinal, 16))
    fun withActionOnDepthPassStencilFail(actionOnDepthPassStencilFail: AGStencilOp): AGStencilOpFuncState = AGStencilOpFuncState(data.insert3(actionOnDepthPassStencilFail.ordinal, 20))

    // Shortcut
    fun withAction(actionOnBothPass: AGStencilOp, actionOnDepthFail: AGStencilOp = actionOnBothPass, actionOnDepthPassStencilFail: AGStencilOp = actionOnDepthFail): AGStencilOpFuncState = withActionOnBothPass(actionOnBothPass).withActionOnDepthFail(actionOnDepthFail).withActionOnDepthPassStencilFail(actionOnDepthPassStencilFail)
}

//open val supportInstancedDrawing: Boolean get() = false

inline class AGFullState(val data: Int32Buffer = Int32Buffer(6)) {
    var blending: AGBlending
        get() = AGBlending(data[0])
        set(value) { data[0] = value.data }
    var stencilOpFunc: AGStencilOpFuncState
        get() = AGStencilOpFuncState(data[1])
        set(value) { data[1] = value.data }
    var stencilRef: AGStencilReferenceState
        get() = AGStencilReferenceState(data[2])
        set(value) { data[2] = value.data }
    var colorMask: AGColorMaskState
        get() = AGColorMaskState(data[3])
        set(value) { data[3] = value.data }
    var scissor: AGScissor
        get() = AGScissor(data[4], data[5])
        set(value) {
            data[4] = value.xy
            data[5] = value.wh
        }
}

inline class AGScissor(val data: Long) {
    constructor(xy: Int, wh: Int) : this(Long.fromLowHigh(xy, wh))
    constructor(x: Int, y: Int, width: Int, height: Int) : this(0.insert16(x, 0).insert16(y, 16), 0.insert16(width, 0).insert16(height, 16))
    constructor(x: Double, y: Double, width: Double, height: Double) : this(x.toIntRound(), y.toIntRound(), width.toIntRound(), height.toIntRound())
    //constructor(x: Double, y: Double, width: Double, height: Double) : this(x.toInt(), y.toInt(), width.toInt(), height.toInt())

    val xy: Int get() = data.low
    val wh: Int get() = data.high

    val x: Int get() = xy.extract16Signed(0)
    val y: Int get() = xy.extract16Signed(16)
    val width: Int get() = wh.extract16Signed(0)
    val height: Int get() = wh.extract16Signed(16)

    val top get() = y
    val left get() = x
    val right get() = x + width
    val bottom get() = y + height

    fun withXY(x: Int, y: Int): AGScissor = AGScissor(0.insert16(x, 0).insert16(y, 16), wh)
    fun withWH(width: Int, height: Int): AGScissor = AGScissor(xy, 0.insert16(width, 0).insert16(height, 16))
    fun copy(x: Int = this.x, y: Int = this.y, width: Int = this.width, height: Int = this.height): AGScissor = AGScissor(x, y, width, height)
    override fun toString(): String {
        if (this == NIL) return "null"
        return "Scissor(x=${x}, y=${y}, width=${width}, height=${height})"
    }

    fun toRect(out: Rectangle = Rectangle()): Rectangle = out.setTo(x, y, width, height)
    fun toRectOrNull(out: Rectangle = Rectangle()): Rectangle? {
        if (this == NIL) return null
        return out.setTo(x, y, width, height)
    }

    companion object {
        val EMPTY = AGScissor(0, 0)
        val FULL = AGScissor(0, 0x7FFF7FFF)
        val NIL = AGScissor(-1, 0x7FFF7FFF)
        fun fromBounds(left: Int, top: Int, right: Int, bottom: Int): AGScissor = AGScissor(left, top, right - left, bottom - top)
        fun fromBounds(left: Double, top: Double, right: Double, bottom: Double): AGScissor = AGScissor(left, top, right - left, bottom - top)

        operator fun invoke(rect: IRectangle?): AGScissor {
            if (rect == null) return NIL
            return AGScissor(rect.x, rect.y, rect.width, rect.height)
        }

        // null is equivalent to Scissor(-Inf, -Inf, +Inf, +Inf)
        fun combine(prev: AGScissor, next: AGScissor): AGScissor {
            if (prev == NIL) return next
            if (next == NIL) return prev

            val intersectsX = prev.left <= next.right && prev.right >= next.left
            val intersectsY = prev.top <= next.bottom && prev.bottom >= next.top
            if (!intersectsX || !intersectsY) return EMPTY

            val left = max(prev.left, next.left)
            val top = max(prev.top, next.top)
            val right = min(prev.right, next.right)
            val bottom = min(prev.bottom, next.bottom)

            return fromBounds(left, top, right, bottom)
        }
    }
}

enum class AGBufferKind { INDEX, VERTEX }

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
    val parentFeatures: AGFeatures? get() = null
    val graphicExtensions: Set<String> get() = emptySet()
    val isInstancedSupported: Boolean get() = parentFeatures?.isInstancedSupported ?: false
    val isStorageMultisampleSupported: Boolean get() = parentFeatures?.isStorageMultisampleSupported ?: false
    val isFloatTextureSupported: Boolean get() = parentFeatures?.isFloatTextureSupported ?: false
}

@OptIn(KorIncomplete::class)
abstract class AG(val checked: Boolean = false) : AGFeatures, Extra by Extra.Mixin() {
    abstract val nativeComponent: Any

    open fun contextLost() {
        Console.info("AG.contextLost()", this)
        //printStackTrace("AG.contextLost")
        commandsSync { it.contextLost() }
    }

    val tempVertexBufferPool = Pool { createBuffer() }
    val tempIndexBufferPool = Pool { createBuffer() }
    val tempTexturePool = Pool { createTexture() }

    open val maxTextureSize = Size(2048, 2048)

    open val devicePixelRatio: Double = 1.0
    open val pixelsPerLogicalInchRatio: Double = 1.0
    /** Approximate on iOS */
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


    interface BitmapSourceBase {
        val rgba: Boolean
        val width: Int
        val height: Int
        val depth: Int get() = 1
    }

    class SyncBitmapSourceList(
        override val rgba: Boolean,
        override val width: Int,
        override val height: Int,
        override val depth: Int,
        val gen: () -> List<Bitmap>?
    ) : BitmapSourceBase {
        companion object {
            val NIL = SyncBitmapSourceList(true, 0, 0, 0) { null }
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

    var createdTextureCount = 0
    var deletedTextureCount = 0

    private val textures = LinkedHashSet<Texture>()
    private val texturesCount: Int get() = textures.size
    private val texturesMemory: ByteUnits get() = ByteUnits.fromBytes(textures.sumOf { it.estimatedMemoryUsage.bytesLong })

    // @TODO: Move most of this to AGQueueProcessorOpenGL, avoid cyclic dependency and simplify
    open inner class Texture constructor(
        open val premultiplied: Boolean,
        val targetKind: AGTextureTargetKind = AGTextureTargetKind.TEXTURE_2D
    ) : Closeable {
        var isFbo: Boolean = false
        var requestMipmaps: Boolean = false
        var mipmaps: Boolean = false; internal set
        var source: BitmapSourceBase = SyncBitmapSource.NIL
        internal var uploaded: Boolean = false
        internal var generating: Boolean = false
        internal var generated: Boolean = false
        internal var tempBitmaps: List<Bitmap?>? = null
        var ready: Boolean = false; internal set

        var cachedVersion = contextVersion
        var texId = commandsNoWait { it.createTexture() }

        var forcedTexId: ForcedTexId? = null
        val implForcedTexId: Int get() = forcedTexId?.forcedTexId ?: -1
        val implForcedTexTarget: AGTextureTargetKind get() = forcedTexId?.forcedTexTarget?.let { AGTextureTargetKind.fromGl(it) } ?: targetKind

        init {
            createdTextureCount++
            textures += this
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

        fun upload(list: List<Bitmap>, width: Int, height: Int): Texture {
            list.fastForEach { checkBitmaps(it) }
            return upload(SyncBitmapSourceList(rgba = true, width = width, height = height, depth = list.size) { list })
        }

        fun upload(bmp: Bitmap?, mipmaps: Boolean = false): Texture {
            bmp?.let { checkBitmaps(it) }
            this.forcedTexId = (bmp as? ForcedTexId?)
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

        var estimatedMemoryUsage: ByteUnits = ByteUnits.fromBytes(0L)

        fun upload(source: BitmapSourceBase, mipmaps: Boolean = false): Texture {
            this.source = source
            estimatedMemoryUsage = ByteUnits.fromBytes(source.width * source.height * source.depth * 4)
            uploadedSource()
            invalidate()
            this.requestMipmaps = mipmaps
            return this
        }

        protected open fun uploadedSource() {
        }

        fun uploadAndBindEnsuring(bmp: Bitmap?, mipmaps: Boolean = false): Texture =
            upload(bmp, mipmaps).bindEnsuring()
        fun uploadAndBindEnsuring(bmp: BitmapSlice<Bitmap>?, mipmaps: Boolean = false): Texture =
            upload(bmp, mipmaps).bindEnsuring()
        fun uploadAndBindEnsuring(source: BitmapSourceBase, mipmaps: Boolean = false): Texture =
            upload(source, mipmaps).bindEnsuring()

        fun doMipmaps(source: BitmapSourceBase, requestMipmaps: Boolean): Boolean {
            return requestMipmaps && source.width.isPowerOfTwo && source.height.isPowerOfTwo
        }

        open fun bind(): Unit = commandsNoWait { it.bindTexture(texId, implForcedTexTarget, implForcedTexId) }
        open fun unbind(): Unit = commandsNoWait { it.bindTexture(0, implForcedTexTarget) }

        private var closed = false
        override fun close() {
            if (!alreadyClosed) {
                alreadyClosed = true
                source = SyncBitmapSource.NIL
                tempBitmaps = null
                deletedTextureCount++
                textures -= this
                //Console.log("CLOSED TEXTURE: $texId")
                //printTexStats()
            }

            if (!closed) {
                closed = true
                if (cachedVersion == contextVersion) {
                    if (texId != 0) {
                        commandsNoWait { it.deleteTexture(texId) }
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
        fun manualUpload(): Texture {
            uploaded = true
            return this
        }

        fun bindEnsuring(): Texture {
            commandsNoWait { it.bindTextureEnsuring(this) }
            return this
        }

        open fun actualSyncUpload(source: BitmapSourceBase, bmps: List<Bitmap?>?, requestMipmaps: Boolean) {
            //this.bind() // Already bound
            this.mipmaps = doMipmaps(source, requestMipmaps)
        }

        init {
            //Console.log("CREATED TEXTURE: $texId")
            //printTexStats()
        }

        private var alreadyClosed = false

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

    private val buffers = LinkedHashSet<AGBuffer>()
    private val buffersCount: Int get() = buffers.size
    private val buffersMemory: ByteUnits get() = ByteUnits.fromBytes(buffers.sumOf { it.estimatedMemoryUsage.bytesLong })

    open inner class AGBuffer constructor(val list: AGList) {
        var dirty = false
        internal var mem: com.soywiz.kmem.Buffer? = null
        internal var memOffset: Int = 0
        internal var memLength: Int = 0

        var estimatedMemoryUsage: ByteUnits = ByteUnits.fromBytes(0)

        init {
            buffers += this
        }

        open fun afterSetMem() {
            estimatedMemoryUsage = ByteUnits.fromBytes(memLength)
        }

        private fun allocateMem(size: Int): com.soywiz.kmem.Buffer {
            if (mem == null || mem!!.sizeInBytes < size) {
                mem = Buffer(size.nextPowerOfTwo)
            }
            return mem!!
            //return Buffer(size)
        }

        fun upload(data: ByteArray, offset: Int = 0, length: Int = data.size): AGBuffer =
            _upload(allocateMem(length).also { it.setArrayInt8(0, data, offset, length) }, 0, length)

        fun upload(data: FloatArray, offset: Int = 0, length: Int = data.size): AGBuffer =
            _upload(allocateMem(length * 4).also { it.setArrayFloat32(0, data, offset, length) }, 0, length * 4)

        fun upload(data: IntArray, offset: Int = 0, length: Int = data.size): AGBuffer =
            _upload(allocateMem(length * 4).also { it.setArrayInt32(0, data, offset, length) }, 0, length * 4)

        fun upload(data: ShortArray, offset: Int = 0, length: Int = data.size): AGBuffer =
            _upload(allocateMem(length * 2).also { it.setArrayInt16(0, data, offset, length) }, 0, length * 2)

        fun upload(data: com.soywiz.kmem.Buffer, offset: Int = 0, length: Int = data.size): AGBuffer =
            _upload(data, offset, length)

        private fun getLen(len: Int, dataSize: Int): Int {
            return if (len >= 0) len else dataSize
        }

        fun upload(data: Any, offset: Int = 0, length: Int = -1): AGBuffer {
            return when (data) {
                is ByteArray -> upload(data, offset, getLen(length, data.size))
                is ShortArray -> upload(data, offset, getLen(length, data.size))
                is IntArray -> upload(data, offset, getLen(length, data.size))
                is FloatArray -> upload(data, offset, getLen(length, data.size))
                is com.soywiz.kmem.Buffer -> upload(data, offset, getLen(length, data.size))
                is IntArrayList -> upload(data.data, offset, getLen(length, data.size))
                is FloatArrayList -> upload(data.data, offset, getLen(length, data.size))
                else -> TODO()
            }
        }

        private fun _upload(data: com.soywiz.kmem.Buffer, offset: Int = 0, length: Int = data.size): AGBuffer {
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
            buffers -= this
            agId = 0
        }
    }

    val dummyTexture by lazy { createTexture() }

    fun createTexture(): Texture = createTexture(premultiplied = true)
    fun createTexture(bmp: Bitmap, mipmaps: Boolean = false): Texture = createTexture(bmp.premultiplied).upload(bmp, mipmaps)
    fun createTexture(bmp: BitmapSlice<Bitmap>, mipmaps: Boolean = false): Texture =
        createTexture(bmp.premultiplied).upload(bmp, mipmaps)

    fun createTexture(bmp: Bitmap, mipmaps: Boolean = false, premultiplied: Boolean = true): Texture =
        createTexture(premultiplied).upload(bmp, mipmaps)

    open fun createTexture(premultiplied: Boolean, targetKind: AGTextureTargetKind = AGTextureTargetKind.TEXTURE_2D): Texture =
        Texture(premultiplied, targetKind)

    open fun createBuffer(): AGBuffer = commandsNoWaitNoExecute { AGBuffer(it) }
    @Deprecated("")
    fun createIndexBuffer() = createBuffer()
    @Deprecated("")
    fun createVertexBuffer() = createBuffer()

    fun createVertexData(vararg attributes: Attribute, layoutSize: Int? = null) = AG.VertexData(createVertexBuffer(), VertexLayout(*attributes, layoutSize = layoutSize))

    fun createIndexBuffer(data: ShortArray, offset: Int = 0, length: Int = data.size - offset) =
        createIndexBuffer().apply {
            upload(data, offset, length)
        }

    fun createIndexBuffer(data: com.soywiz.kmem.Buffer, offset: Int = 0, length: Int = data.size - offset) =
        createIndexBuffer().apply {
            upload(data, offset, length)
        }

    fun createVertexBuffer(data: FloatArray, offset: Int = 0, length: Int = data.size - offset) =
        createVertexBuffer().apply {
            upload(data, offset, length)
        }

    fun createVertexBuffer(data: com.soywiz.kmem.Buffer, offset: Int = 0, length: Int = data.size - offset) =
        createVertexBuffer().apply {
            upload(data, offset, length)
        }

    @Deprecated("Use draw(Batch) or drawV2() instead")
    fun draw(
        vertices: AGBuffer,
        program: Program,
        type: AGDrawType,
        vertexLayout: VertexLayout,
        vertexCount: Int,
        indices: AGBuffer? = null,
        indexType: AGIndexType = AGIndexType.USHORT,
        offset: Int = 0,
        blending: AGBlending = AGBlending.NORMAL,
        uniforms: AGUniformValues = AGUniformValues.EMPTY,
        stencilRef: AGStencilReferenceState = AGStencilReferenceState.DEFAULT,
        stencilOpFunc: AGStencilOpFuncState = AGStencilOpFuncState.DEFAULT,
        colorMask: AGColorMaskState = AGColorMaskState.ALL_ENABLED,
        renderState: AGRenderState = AGRenderState.DEFAULT,
        scissor: AGScissor = AGScissor.NIL,
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
        batch.stencilRef = stencilRef
        batch.stencilOpFunc = stencilOpFunc
        batch.colorMask = colorMask
        batch.renderState = renderState
        batch.scissor = scissor
        batch.instances = instances
    })

    fun drawV2(
        vertexData: FastArrayList<VertexData>,
        program: Program,
        type: AGDrawType,
        vertexCount: Int,
        indices: AGBuffer? = null,
        indexType: AGIndexType = AGIndexType.USHORT,
        offset: Int = 0,
        blending: AGBlending = AGBlending.NORMAL,
        uniforms: AGUniformValues = AGUniformValues.EMPTY,
        stencilRef: AGStencilReferenceState = AGStencilReferenceState.DEFAULT,
        stencilOpFunc: AGStencilOpFuncState = AGStencilOpFuncState.DEFAULT,
        colorMask: AGColorMaskState = AGColorMaskState.ALL_ENABLED,
        renderState: AGRenderState = AGRenderState.DEFAULT,
        scissor: AGScissor = AGScissor.NIL,
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
        batch.stencilRef = stencilRef
        batch.stencilOpFunc = stencilOpFunc
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
        var _buffer: AGBuffer?,
        var layout: VertexLayout = VertexLayout()
    ) {
        val buffer: AGBuffer get() = _buffer!!
    }

    data class Batch constructor(
        var vertexData: FastArrayList<VertexData> = fastArrayListOf(VertexData(null)),
        var program: Program = DefaultShaders.PROGRAM_DEBUG,
        var type: AGDrawType = AGDrawType.TRIANGLES,
        var vertexCount: Int = 0,
        var indices: AGBuffer? = null,
        var indexType: AGIndexType = AGIndexType.USHORT,
        var offset: Int = 0,
        var blending: AGBlending = AGBlending.NORMAL,
        var uniforms: AGUniformValues = AGUniformValues.EMPTY,
        var stencilOpFunc: AGStencilOpFuncState = AGStencilOpFuncState.DEFAULT,
        var stencilRef: AGStencilReferenceState = AGStencilReferenceState.DEFAULT,
        var colorMask: AGColorMaskState = AGColorMaskState(),
        var renderState: AGRenderState = AGRenderState(),
        var scissor: AGScissor = AGScissor.NIL,
        var instances: Int = 1
    ) {

        var stencilFull: AGStencilFullState
            get() = AGStencilFullState(stencilOpFunc, stencilRef)
            set(value) {
                stencilOpFunc = value.opFunc
                stencilRef = value.ref
            }

        private val singleVertexData = FastArrayList<VertexData>()

        private fun ensureSingleVertexData() {
            if (singleVertexData.isEmpty()) singleVertexData.add(VertexData(null))
            vertexData = singleVertexData
        }

        @Deprecated("Use vertexData instead")
        var vertices: AGBuffer
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
        val stencilRef = batch.stencilRef
        val stencilOpFunc = batch.stencilOpFunc
        val colorMask = batch.colorMask
        val renderState = batch.renderState
        val scissor = batch.scissor

        //println("SCISSOR: $scissor")

        //finalScissor.setTo(0, 0, backWidth, backHeight)

        commandsNoWait { list ->
            list.setScissorState(this, scissor)

            getProgram(program, config = when {
                uniforms.useExternalSampler() -> ProgramConfig.EXTERNAL_TEXTURE_SAMPLER
                else -> ProgramConfig.DEFAULT
            }).use(list)

            list.vertexArrayObjectSet(VertexArrayObject(batch.vertexData)) {
                list.uniformsSet(uniforms) {
                    list.setState(blending, stencilOpFunc, stencilRef, colorMask, renderState)

                    //val viewport = Buffer(4 * 4)
                    //gl.getIntegerv(KmlGl.VIEWPORT, viewport)
                    //println("viewport=${viewport.getAlignedInt32(0)},${viewport.getAlignedInt32(1)},${viewport.getAlignedInt32(2)},${viewport.getAlignedInt32(3)}")

                    list.draw(type, vertexCount, offset, instances, if (indices != null) indexType else AGIndexType.NONE, indices)
                }
            }
        }
    }

    fun AGUniformValues.useExternalSampler(): Boolean {
        var useExternalSampler = false
        this.fastForEach { uniform, value ->
            val uniformType = uniform.type
            when (uniformType) {
                VarType.Sampler2D -> {
                    val unit = value.fastCastTo<TextureUnit>()
                    val tex = (unit.texture.fastCastTo<Texture?>())
                    if (tex != null) {
                        if (tex.implForcedTexTarget == AGTextureTargetKind.EXTERNAL_TEXTURE) {
                            useExternalSampler = true
                        }
                    }
                }
                else -> Unit
            }
        }
        //println("useExternalSampler=$useExternalSampler")
        return useExternalSampler
    }

    open fun disposeTemporalPerFrameStuff() = Unit

    val frameRenderBuffers = LinkedHashSet<RenderBuffer>()
    val renderBuffers = Pool<RenderBuffer>() { createRenderBuffer() }

    interface BaseRenderBuffer : Closeable {
        val x: Int
        val y: Int
        val width: Int
        val height: Int
        val fullWidth: Int
        val fullHeight: Int
        val scissor: RectangleInt?
        val estimatedMemoryUsage: ByteUnits
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

    private val allRenderBuffers = LinkedHashSet<BaseRenderBuffer>()
    private val renderBufferCount: Int get() = allRenderBuffers.size
    private val renderBuffersMemory: ByteUnits get() = ByteUnits.fromBytes(allRenderBuffers.sumOf { it.estimatedMemoryUsage.bytesLong })

    open inner class BaseRenderBufferImpl : BaseRenderBuffer {
        override var x = 0
        override var y = 0
        override var width = RenderBufferConsts.DEFAULT_INITIAL_WIDTH
        override var height = RenderBufferConsts.DEFAULT_INITIAL_HEIGHT
        override var fullWidth = RenderBufferConsts.DEFAULT_INITIAL_WIDTH
        override var fullHeight = RenderBufferConsts.DEFAULT_INITIAL_HEIGHT
        private val _scissor = RectangleInt()
        override var scissor: RectangleInt? = null

        override var estimatedMemoryUsage: ByteUnits = ByteUnits.fromBytes(0)

        override fun setSize(x: Int, y: Int, width: Int, height: Int, fullWidth: Int, fullHeight: Int) {
            estimatedMemoryUsage = ByteUnits.fromBytes(fullWidth * fullHeight * (4 + 4))

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

        init {
            allRenderBuffers += this
        }

        override fun close() {
            allRenderBuffers -= this
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

    open inner class RenderBuffer : BaseRenderBufferImpl() {
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
        override fun close() {
            super.close()
            cachedTexVersion = -1
            _tex?.close()
            _tex = null
        }
    }

    protected fun setViewport(buffer: BaseRenderBuffer) {
        commandsNoWait { it.viewport(buffer.x, buffer.y, buffer.width, buffer.height) }
        //println("setViewport: ${buffer.x}, ${buffer.y}, ${buffer.width}, ${buffer.height}")
    }

    var lastRenderContextId = 0

    inner class GlRenderBuffer : RenderBuffer() {
        override val id = lastRenderContextId++

        var frameBufferId: Int = -1

        // http://wangchuan.github.io/coding/2016/05/26/multisampling-fbo.html
        override fun set() {
            setViewport(this)

            commandsNoWait { list ->
                if (dirty) {
                    if (frameBufferId < 0) {
                        frameBufferId = list.frameBufferCreate()
                    }
                    list.frameBufferSet(frameBufferId, tex.texId, width, height, hasStencil, hasDepth)
                }
                list.frameBufferUse(frameBufferId)
            }
        }

        override fun close() {
            super.close()
            commandsNoWait { list ->
                if (frameBufferId >= 0) {
                    list.frameBufferDelete(frameBufferId)
                    frameBufferId = -1
                }
            }
        }

        override fun toString(): String = "GlRenderBuffer[$id]($width, $height)"
    }

    open fun createRenderBuffer(): RenderBuffer = GlRenderBuffer()

    //open fun createRenderBuffer() = RenderBuffer()

    fun flip() {
        disposeTemporalPerFrameStuff()
        renderBuffers.free(frameRenderBuffers)
        if (frameRenderBuffers.isNotEmpty()) frameRenderBuffers.clear()
        flipInternal()
        commandsSync { it.finish() }
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
        scissor: AGScissor = AGScissor.NIL,
    ) {
        commandsNoWait { list ->
            //println("CLEAR: $color, $depth")
            list.setScissorState(this, scissor)
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

    fun clearStencil(stencil: Int = 0, scissor: AGScissor = AGScissor.NIL) = clear(clearColor = false, clearDepth = false, clearStencil = true, stencil = stencil, scissor = scissor)
    fun clearDepth(depth: Float = 1f, scissor: AGScissor = AGScissor.NIL) = clear(clearColor = false, clearDepth = true, clearStencil = false, depth = depth, scissor = scissor)
    fun clearColor(color: RGBA = Colors.TRANSPARENT_BLACK, scissor: AGScissor = AGScissor.NIL) = clear(clearColor = true, clearDepth = false, clearStencil = false, color = color, scissor = scissor)

    val renderBufferStack = FastArrayList<BaseRenderBuffer?>()

    //@PublishedApi
    @KoragExperimental
    var currentRenderBuffer: BaseRenderBuffer? = null
        private set

    val currentRenderBufferOrMain: BaseRenderBuffer get() = currentRenderBuffer ?: mainRenderBuffer

    val isRenderingToWindow: Boolean get() = currentRenderBufferOrMain === mainRenderBuffer
    val isRenderingToTexture: Boolean get() = !isRenderingToWindow

    @Deprecated("", ReplaceWith("isRenderingToTexture"))
    val renderingToTexture: Boolean get() = isRenderingToTexture

    inline fun backupTexture(tex: Texture?, callback: () -> Unit) {
        if (tex != null) {
            readColorTexture(tex, 0, 0, backWidth, backHeight)
        }
        try {
            callback()
        } finally {
            if (tex != null) drawTexture(tex)
        }
    }

    inline fun setRenderBufferTemporally(rb: BaseRenderBuffer, callback: (BaseRenderBuffer) -> Unit) {
        pushRenderBuffer(rb)
        try {
            callback(rb)
        } finally {
            popRenderBuffer()
        }
    }

    var adjustFrameRenderBufferSize = false
    //var adjustFrameRenderBufferSize = true

    //open fun fixWidthForRenderToTexture(width: Int): Int = kotlin.math.max(64, width).nextPowerOfTwo
    //open fun fixHeightForRenderToTexture(height: Int): Int = kotlin.math.max(64, height).nextPowerOfTwo

    open fun fixWidthForRenderToTexture(width: Int): Int = if (adjustFrameRenderBufferSize) width.nextMultipleOf(64) else width
    open fun fixHeightForRenderToTexture(height: Int): Int = if (adjustFrameRenderBufferSize) height.nextMultipleOf(64) else height

    //open fun fixWidthForRenderToTexture(width: Int): Int = width
    //open fun fixHeightForRenderToTexture(height: Int): Int = height

    inline fun tempAllocateFrameBuffer(width: Int, height: Int, hasDepth: Boolean = false, hasStencil: Boolean = true, msamples: Int = 1, block: (rb: RenderBuffer) -> Unit) {
        val rb = unsafeAllocateFrameRenderBuffer(width, height, hasDepth = hasDepth, hasStencil = hasStencil, msamples = msamples)
        try {
            block(rb)
        } finally {
            unsafeFreeFrameRenderBuffer(rb)
        }
    }

    inline fun tempAllocateFrameBuffers2(width: Int, height: Int, hasDepth: Boolean = false, hasStencil: Boolean = true, msamples: Int = 1, block: (rb0: RenderBuffer, rb1: RenderBuffer) -> Unit) {
        tempAllocateFrameBuffer(width, height, hasDepth, hasStencil, msamples) { rb0 ->
            tempAllocateFrameBuffer(width, height, hasDepth, hasStencil, msamples) { rb1 ->
                block(rb0, rb1)
            }
        }
    }

    @KoragExperimental
    fun unsafeAllocateFrameRenderBuffer(width: Int, height: Int, hasDepth: Boolean = false, hasStencil: Boolean = true, msamples: Int = 1, onlyThisFrame: Boolean = true): RenderBuffer {
        val realWidth = fixWidthForRenderToTexture(kotlin.math.max(width, 64))
        val realHeight = fixHeightForRenderToTexture(kotlin.math.max(height, 64))
        val rb = renderBuffers.alloc()
        if (onlyThisFrame) frameRenderBuffers += rb
        rb.setSize(0, 0, realWidth, realHeight, realWidth, realHeight)
        rb.setExtra(hasDepth = hasDepth, hasStencil = hasStencil)
        rb.setSamples(msamples)
        //println("unsafeAllocateFrameRenderBuffer($width, $height), real($realWidth, $realHeight), $rb")
        return rb
    }

    @KoragExperimental
    fun unsafeFreeFrameRenderBuffer(rb: RenderBuffer) {
        if (frameRenderBuffers.remove(rb)) {
        }
        renderBuffers.free(rb)
    }

    @OptIn(KoragExperimental::class)
    inline fun renderToTexture(
        width: Int, height: Int,
        render: (rb: RenderBuffer) -> Unit,
        hasDepth: Boolean = false, hasStencil: Boolean = false, msamples: Int = 1,
        use: (tex: Texture, texWidth: Int, texHeight: Int) -> Unit
    ) {
        commandsNoWait { list ->
            list.flush()
        }
        tempAllocateFrameBuffer(width, height, hasDepth, hasStencil, msamples) { rb ->
            setRenderBufferTemporally(rb) {
                clear(Colors.TRANSPARENT_BLACK) // transparent
                render(rb)
            }
            use(rb.tex, rb.width, rb.height)
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

    fun getRenderBufferAtStackPoint(offset: Int): BaseRenderBuffer {
        if (offset == 0) return currentRenderBufferOrMain
        return renderBufferStack.getOrNull(renderBufferStack.size + offset) ?: mainRenderBuffer
    }

    fun pushRenderBuffer(renderBuffer: BaseRenderBuffer) {
        renderBufferStack.add(currentRenderBuffer)
        setRenderBuffer(renderBuffer)
    }

    fun popRenderBuffer() {
        setRenderBuffer(renderBufferStack.last())
        renderBufferStack.removeAt(renderBufferStack.size - 1)
    }

    // @TODO: Rename to Sync and add Suspend versions
    fun readPixel(x: Int, y: Int): RGBA {
        val rawColor = Bitmap32(1, 1, premultiplied = isRenderingToTexture).also { readColor(it, x, y) }.ints[0]
        return if (isRenderingToTexture) RGBAPremultiplied(rawColor).depremultiplied else RGBA(rawColor)
    }

    open fun readColor(bitmap: Bitmap32, x: Int = 0, y: Int = 0) {
        commandsSync { it.readPixels(x, y, bitmap.width, bitmap.height, bitmap.ints, AGReadKind.COLOR) }
    }
    open fun readDepth(width: Int, height: Int, out: FloatArray) {
        commandsSync { it.readPixels(0, 0, width, height, out, AGReadKind.DEPTH) }
    }
    open fun readStencil(bitmap: Bitmap8) {
        commandsSync { it.readPixels(0, 0, bitmap.width, bitmap.height, bitmap.data, AGReadKind.STENCIL) }
    }
    fun readDepth(out: FloatArray2): Unit = readDepth(out.width, out.height, out.data)
    open fun readColorTexture(texture: Texture, x: Int = 0, y: Int = 0, width: Int = backWidth, height: Int = backHeight): Unit = TODO()
    fun readColor(): Bitmap32 = Bitmap32(backWidth, backHeight, premultiplied = isRenderingToTexture).apply { readColor(this) }
    fun readDepth(): FloatArray2 = FloatArray2(backWidth, backHeight) { 0f }.apply { readDepth(this) }

    inner class TextureDrawer {
        val VERTEX_COUNT = 4
        val vertices = createBuffer()
        val vertexLayout = VertexLayout(DefaultShaders.a_Pos, DefaultShaders.a_Tex)
        val verticesData = Buffer(VERTEX_COUNT * vertexLayout.totalSize)
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
        val uniforms = AGUniformValues()

        fun setVertex(n: Int, px: Float, py: Float, tx: Float, ty: Float) {
            val offset = n * 4
            verticesData.setFloat32(offset + 0, px)
            verticesData.setFloat32(offset + 1, py)
            verticesData.setFloat32(offset + 2, tx)
            verticesData.setFloat32(offset + 3, ty)
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
                type = AGDrawType.TRIANGLE_STRIP,
                vertexLayout = vertexLayout,
                vertexCount = 4,
                uniforms = uniforms,
                blending = AGBlending.NONE
            )
        }
    }

    //////////////

    private var programCount = 0
    // @TODO: Simplify this. Why do we need external? Maybe we could copy external textures into normal ones to avoid issues
    //private val programs = FastIdentityMap<Program, FastIdentityMap<ProgramConfig, AgProgram>>()
    //private val programs = HashMap<Program, FastIdentityMap<ProgramConfig, AgProgram>>()
    //private val normalPrograms = FastIdentityMap<Program, AgProgram>()
    //private val externalPrograms = FastIdentityMap<Program, AgProgram>()
    private val normalPrograms = HashMap<Program, AgProgram>()
    private val externalPrograms = HashMap<Program, AgProgram>()

    @JvmOverloads
    fun getProgram(program: Program, config: ProgramConfig = ProgramConfig.DEFAULT): AgProgram {
        val map = if (config.externalTextureSampler) externalPrograms else normalPrograms
        return map.getOrPut(program) { AgProgram(program, config) }
    }

    inner class AgProgram(val program: Program, val programConfig: ProgramConfig) {
        var cachedVersion = -1
        var programId = 0

        fun ensure(list: AGList) {
            if (cachedVersion != contextVersion) {
                val time = measureTime {
                    programCount++
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
            if (programId != 0) {
                programCount--
                list.deleteProgram(programId)
            }
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

    protected val _globalState = AGGlobalState(checked)
    var contextVersion: Int by _globalState::contextVersion
    @PublishedApi internal val _list = _globalState.createList()

    val multithreadedRendering: Boolean get() = false

    @OptIn(ExperimentalContracts::class)
    @Deprecated("Use commandsNoWait instead")
    inline fun <T> commands(block: (AGList) -> T): T {
        contract { callsInPlace(block, InvocationKind.EXACTLY_ONCE) }
        return commandsNoWait(block)
    }

    /**
     * Queues commands, and wait for them to be executed synchronously
     */
    @OptIn(ExperimentalContracts::class)
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
    inline fun <T> commandsNoWait(block: (AGList) -> T): T {
        contract { callsInPlace(block, InvocationKind.EXACTLY_ONCE) }
        val result = block(_list)
        if (!multithreadedRendering) _executeList(_list)
        return result
    }

    /**
     * Queues commands without waiting. In non-multithreaded mode, not even executing
     */
    @OptIn(ExperimentalContracts::class)
    inline fun <T> commandsNoWaitNoExecute(block: (AGList) -> T): T {
        contract { callsInPlace(block, InvocationKind.EXACTLY_ONCE) }
        return block(_list)
    }

    /**
     * Queues commands and suspend until they are executed
     */
    @OptIn(ExperimentalContracts::class)
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

    private val stats = AGStats()

    fun getStats(out: AGStats = stats): AGStats {
        out.texturesMemory = this.texturesMemory
        out.texturesCount = this.texturesCount
        out.buffersMemory = this.buffersMemory
        out.buffersCount = this.buffersCount
        out.renderBuffersMemory = this.renderBuffersMemory
        out.renderBuffersCount = this.renderBufferCount
        out.texturesCreated = this.createdTextureCount
        out.texturesDeleted = this.deletedTextureCount
        out.programCount = this.programCount
        return out
    }

    class AGStats(
        var texturesCount: Int = 0,
        var texturesMemory: ByteUnits = ByteUnits.fromBytes(0),
        var buffersCount: Int = 0,
        var buffersMemory: ByteUnits = ByteUnits.fromBytes(0),
        var renderBuffersCount: Int = 0,
        var renderBuffersMemory: ByteUnits = ByteUnits.fromBytes(0),
        var texturesCreated: Int = 0,
        var texturesDeleted: Int = 0,
        var programCount: Int = 0,
    ) {
        override fun toString(): String =
            "AGStats(textures[$texturesCount] = $texturesMemory, buffers[$buffersCount] = $buffersMemory, renderBuffers[$renderBuffersCount] = $renderBuffersMemory, programs[$programCount])"
    }
}

class AGUniformValues() : Iterable<Pair<Uniform, Any>> {
    companion object {
        internal val EMPTY = AGUniformValues()

        fun valueToString(value: Any?): String {
            if (value is FloatArray) return value.toList().toString()
            return value.toString()
        }
    }

    fun clone(): AGUniformValues = AGUniformValues().also { it.setTo(this) }

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

    operator fun plus(other: AGUniformValues): AGUniformValues {
        return AGUniformValues().put(this).put(other)
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

    fun put(uniform: Uniform, value: Any): AGUniformValues {
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

    fun put(uniforms: AGUniformValues?): AGUniformValues {
        if (uniforms == null) return this
        for (n in 0 until uniforms.size) {
            this.put(uniforms._uniforms[n], uniforms._values[n])
        }
        return this
    }

    fun setTo(uniforms: AGUniformValues) {
        clear()
        put(uniforms)
    }

    override fun iterator(): Iterator<Pair<Uniform, Any>> = iterator {
        fastForEach { uniform, value -> yield(uniform to value) }
    }

    override fun toString() = "{" + keys.zip(values)
        .joinToString(", ") { "${it.first}=${valueToString(it.second)}" } + "}"
}

fun AGBlending.toRenderFboIntoBack() = this
fun AGBlending.toRenderImageIntoFbo() = this
