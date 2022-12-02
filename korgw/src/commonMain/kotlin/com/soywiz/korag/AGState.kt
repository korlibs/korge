package com.soywiz.korag

import com.soywiz.kds.*
import com.soywiz.kds.iterators.*
import com.soywiz.klock.*
import com.soywiz.klogger.*
import com.soywiz.kmem.*
import com.soywiz.kmem.unit.*
import com.soywiz.korag.annotation.*
import com.soywiz.korag.gl.*
import com.soywiz.korag.shader.*
import com.soywiz.korag.shader.gl.*
import com.soywiz.korim.bitmap.*
import com.soywiz.korim.color.*
import com.soywiz.korio.lang.*
import com.soywiz.korma.geom.*
import kotlin.coroutines.*
import kotlin.jvm.*

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

inline class AGFullState(val data: IntArray = IntArray(6)) {
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

            val left = kotlin.math.max(prev.left, next.left)
            val top = kotlin.math.max(prev.top, next.top)
            val right = kotlin.math.min(prev.right, next.right)
            val bottom = kotlin.math.min(prev.bottom, next.bottom)

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


class AGProgram(val ag: AG, val program: Program, val programConfig: ProgramConfig) {
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


interface AGBaseRenderBuffer : Closeable {
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

open class AGBaseRenderBufferImpl(val ag: AG) : AGBaseRenderBuffer {
    override var x = 0
    override var y = 0
    override var width = AG.RenderBufferConsts.DEFAULT_INITIAL_WIDTH
    override var height = AG.RenderBufferConsts.DEFAULT_INITIAL_HEIGHT
    override var fullWidth = AG.RenderBufferConsts.DEFAULT_INITIAL_WIDTH
    override var fullHeight = AG.RenderBufferConsts.DEFAULT_INITIAL_HEIGHT
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
        ag.allRenderBuffers += this
    }

    override fun close() {
        ag.allRenderBuffers -= this
    }
}

open class AGRenderBuffer(ag: AG) : AGBaseRenderBufferImpl(ag) {
    open val id: Int = -1
    private var cachedTexVersion = -1
    private var _tex: AGTexture? = null
    protected var nsamples: Int = 1
    protected var hasDepth: Boolean = true
    protected var hasStencil: Boolean = true

    val tex: AGTexture
        get() {
            if (cachedTexVersion != ag.contextVersion) {
                cachedTexVersion = ag.contextVersion
                _tex = ag.createTexture(premultiplied = true).manualUpload().apply { isFbo = true }
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
    fun readBitmap(bmp: Bitmap32) = ag.readColor(bmp)
    fun readDepth(width: Int, height: Int, out: FloatArray): Unit = ag.readDepth(width, height, out)
    override fun close() {
        super.close()
        cachedTexVersion = -1
        _tex?.close()
        _tex = null
    }
}

class AGFinalRenderBuffer(ag: AG) : AGRenderBuffer(ag) {
    override val id = ag.lastRenderContextId++

    var frameBufferId: Int = -1

    // http://wangchuan.github.io/coding/2016/05/26/multisampling-fbo.html
    override fun set() {
        ag.setViewport(this)

        ag.commandsNoWait { list ->
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
        ag.commandsNoWait { list ->
            if (frameBufferId >= 0) {
                list.frameBufferDelete(frameBufferId)
                frameBufferId = -1
            }
        }
    }

    override fun toString(): String = "GlRenderBuffer[$id]($width, $height)"
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

/** List<VertexData> -> VAO */
@JvmInline
value class AGVertexArrayObject(
    val list: FastArrayList<AGVertexData>
) {
}

data class AGVertexData constructor(
    var _buffer: AGBuffer?,
    var layout: VertexLayout = VertexLayout()
) {
    val buffer: AGBuffer get() = _buffer!!
}

data class AGBatch constructor(
    var vertexData: FastArrayList<AGVertexData> = fastArrayListOf(AGVertexData(null)),
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

    private val singleVertexData = FastArrayList<AGVertexData>()

    private fun ensureSingleVertexData() {
        if (singleVertexData.isEmpty()) singleVertexData.add(AGVertexData(null))
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
    fun clone(): AGTextureUnit = AGTextureUnit(index, texture, linear, trilinear)
}

// @TODO: Move most of this to AGQueueProcessorOpenGL, avoid cyclic dependency and simplify
open class AGTexture constructor(
    val ag: AG,
    open val premultiplied: Boolean,
    val targetKind: AGTextureTargetKind = AGTextureTargetKind.TEXTURE_2D
) : Closeable {
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

    init {
        //Console.log("CREATED TEXTURE: $texId")
        //printTexStats()
    }

    private var alreadyClosed = false

    private fun printTexStats() {
        //Console.log("create=$createdCount, delete=$deletedCount, alive=${createdCount - deletedCount}")
    }
}


class AGTextureDrawer(val ag: AG) {
    val VERTEX_COUNT = 4
    val vertices = ag.createBuffer()
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

    fun draw(tex: AGTexture, left: Float, top: Float, right: Float, bottom: Float) {
        //tex.upload(Bitmap32(32, 32) { x, y -> Colors.RED })
        uniforms[DefaultShaders.u_Tex].set(AGTextureUnit(0, tex))

        val texLeft = -1f
        val texRight = +1f
        val texTop = -1f
        val texBottom = +1f

        setVertex(0, left, top, texLeft, texTop)
        setVertex(1, right, top, texRight, texTop)
        setVertex(2, left, bottom, texLeft, texBottom)
        setVertex(3, right, bottom, texRight, texBottom)

        vertices.upload(verticesData)
        ag.draw(
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

open class AGBuffer constructor(val ag: AG, val list: AGList) {
    var dirty = false
    internal var mem: com.soywiz.kmem.Buffer? = null
    internal var memOffset: Int = 0
    internal var memLength: Int = 0

    var estimatedMemoryUsage: ByteUnits = ByteUnits.fromBytes(0)

    init {
        ag.buffers += this
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
        ag.buffers -= this
        agId = 0
    }
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
    var updatedBufferCount: Int = 0,
) {
    override fun toString(): String =
        "AGStats(textures[$texturesCount] = $texturesMemory, buffers[$buffersCount] = $buffersMemory, renderBuffers[$renderBuffersCount] = $renderBuffersMemory, programs[$programCount], updatedBufferCount[$updatedBufferCount])"

    fun startFrame() {
        updatedBufferCount = 0
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

fun AGBlending.toRenderFboIntoBack() = this
fun AGBlending.toRenderImageIntoFbo() = this
