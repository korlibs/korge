/**
 * https://www.khronos.org/files/webgl/webgl-reference-card-1_0.pdf
 */

@file:Suppress("unused")

package korlibs.graphics.shader

import korlibs.datastructure.*
import korlibs.graphics.*
import korlibs.graphics.annotation.KoragExperimental
import korlibs.graphics.shader.gl.*
import korlibs.io.lang.*
import korlibs.math.*
import kotlin.reflect.*
import kotlin.collections.Iterable

enum class VarKind(val bytesSize: Int) {
    //BYTE(1), UNSIGNED_BYTE(1), SHORT(2), UNSIGNED_SHORT(2), INT(4), FLOAT(4) // @TODO: This cause problems on Kotlin/Native Objective-C header.h
    TBOOL(1), TBYTE(1), TUNSIGNED_BYTE(1), TSHORT(2), TUNSIGNED_SHORT(2), TINT(4), TFLOAT(4)
    //, TUNSIGNED_INT(4)
}

data class FuncDecl(
    override val name: String,
    override val rettype: VarType,
    override val args: List<Pair<String, VarType>>,
    val stm: Program.Stm
    ) : Program.FuncRef, Program.FuncRef0, Program.FuncRef1, Program.FuncRef2, Program.FuncRef3, Program.FuncRef4, Program.FuncRef5, Program.FuncRefN {
    val ref: Program.FuncRef = this
    operator fun getValue(thisRef: Any?, property: KProperty<*>): FuncDecl = this
}

interface VarTypeAccessor {
    val TVOID: VarType get() = VarType.TVOID
    val Mat2: VarType get() = VarType.Mat2
    val Mat3: VarType get() = VarType.Mat3
    val Mat4: VarType get() = VarType.Mat4
    val Sampler1D: VarType get() = VarType.Sampler1D
    val Sampler2D: VarType get() = VarType.Sampler2D
    val Sampler3D: VarType get() = VarType.Sampler3D
    val SamplerCube: VarType get() = VarType.SamplerCube
    val Int1: VarType get() = VarType.Int1
    val Float1: VarType get() = VarType.Float1
    val Float2: VarType get() = VarType.Float2
    val Float3: VarType get() = VarType.Float3
    val Float4: VarType get() = VarType.Float4
    val Short1: VarType get() = VarType.Short1
    val Short2: VarType get() = VarType.Short2
    val Short3: VarType get() = VarType.Short3
    val Short4: VarType get() = VarType.Short4
    val Bool1: VarType get() = VarType.Bool1
    val Byte4: VarType get() = VarType.Byte4
    val SByte1: VarType get() = VarType.SByte1
    val SByte2: VarType get() = VarType.SByte2
    val SByte3: VarType get() = VarType.SByte3
    val SByte4: VarType get() = VarType.SByte4
    val UByte1: VarType get() = VarType.UByte1
    val UByte2: VarType get() = VarType.UByte2
    val UByte3: VarType get() = VarType.UByte3
    val UByte4: VarType get() = VarType.UByte4
    val SShort1: VarType get() = VarType.SShort1
    val SShort2: VarType get() = VarType.SShort2
    val SShort3: VarType get() = VarType.SShort3
    val SShort4: VarType get() = VarType.SShort4
    val UShort1: VarType get() = VarType.UShort1
    val UShort2: VarType get() = VarType.UShort2
    val UShort3: VarType get() = VarType.UShort3
    val UShort4: VarType get() = VarType.UShort4
    val SInt1: VarType get() = VarType.SInt1
    val SInt2: VarType get() = VarType.SInt2
    val SInt3: VarType get() = VarType.SInt3
    val SInt4: VarType get() = VarType.SInt4
}

enum class SamplerVarType(val vtype: VarType) {
    Sampler1D(VarType.Sampler1D),
    Sampler2D(VarType.Sampler2D),
    Sampler3D(VarType.Sampler3D),
    SamplerCube(VarType.SamplerCube);
}

enum class VarType(val kind: VarKind, val elementCount: Int, val isMatrix: Boolean = false) {
	TVOID(VarKind.TBYTE, elementCount = 0),

	Mat2(VarKind.TFLOAT, elementCount = 4, isMatrix = true),
	Mat3(VarKind.TFLOAT, elementCount = 9, isMatrix = true),
	Mat4(VarKind.TFLOAT, elementCount = 16, isMatrix = true),

    //TODO: need to have a way of indicating Float/Int/UInt variations + more types of sampler to add
    Sampler1D(VarKind.TINT, elementCount = 1),
    Sampler2D(VarKind.TINT, elementCount = 1),
    Sampler3D(VarKind.TINT, elementCount = 1),
    SamplerCube(VarKind.TINT, elementCount = 1),

	Int1(VarKind.TINT, elementCount = 1),

    Float1(VarKind.TFLOAT, elementCount = 1),
	Float2(VarKind.TFLOAT, elementCount = 2),
	Float3(VarKind.TFLOAT, elementCount = 3),
	Float4(VarKind.TFLOAT, elementCount = 4),

	Short1(VarKind.TSHORT, elementCount = 1),
	Short2(VarKind.TSHORT, elementCount = 2),
	Short3(VarKind.TSHORT, elementCount = 3),
	Short4(VarKind.TSHORT, elementCount = 4),

	Bool1(VarKind.TBOOL, elementCount = 1),
    Bool2(VarKind.TBOOL, elementCount = 2),
    Bool3(VarKind.TBOOL, elementCount = 3),
    Bool4(VarKind.TBOOL, elementCount = 4),

    @Deprecated("Use UByte4 instead")
    Byte4(VarKind.TUNSIGNED_BYTE, elementCount = 4), // OLD: Is this right?

	SByte1(VarKind.TBYTE, elementCount = 1),
	SByte2(VarKind.TBYTE, elementCount = 2),
	SByte3(VarKind.TBYTE, elementCount = 3),
	SByte4(VarKind.TBYTE, elementCount = 4),

	UByte1(VarKind.TUNSIGNED_BYTE, elementCount = 1),
	UByte2(VarKind.TUNSIGNED_BYTE, elementCount = 2),
	UByte3(VarKind.TUNSIGNED_BYTE, elementCount = 3),
	UByte4(VarKind.TUNSIGNED_BYTE, elementCount = 4),

	SShort1(VarKind.TSHORT, elementCount = 1),
	SShort2(VarKind.TSHORT, elementCount = 2),
	SShort3(VarKind.TSHORT, elementCount = 3),
	SShort4(VarKind.TSHORT, elementCount = 4),

	UShort1(VarKind.TUNSIGNED_SHORT, elementCount = 1),
	UShort2(VarKind.TUNSIGNED_SHORT, elementCount = 2),
	UShort3(VarKind.TUNSIGNED_SHORT, elementCount = 3),
	UShort4(VarKind.TUNSIGNED_SHORT, elementCount = 4),

	SInt1(VarKind.TINT, elementCount = 1),
	SInt2(VarKind.TINT, elementCount = 2),
	SInt3(VarKind.TINT, elementCount = 3),
	SInt4(VarKind.TINT, elementCount = 4),
	;

    fun withElementCount(length: Int): VarType {
        return when (kind) {
            VarKind.TBYTE -> BYTE(length)
            VarKind.TUNSIGNED_BYTE -> UBYTE(length)
            VarKind.TSHORT -> SHORT(length)
            VarKind.TUNSIGNED_SHORT -> USHORT(length)
            VarKind.TINT -> INT(length)
            VarKind.TFLOAT -> FLOAT(length)
            else -> TODO()
        }
    }

    val isSampler: Boolean get() = this == Sampler1D || this == Sampler2D || this == Sampler3D || this == SamplerCube

    val bytesSize: Int = kind.bytesSize * elementCount

	companion object {
        @Deprecated("", ReplaceWith("VarType.Sampler2D", "korlibs.graphics.shader.VarType"))
        val TextureUnit get() = Sampler2D

        fun BOOL(count: Int) =
            when (count) { 0 -> TVOID; 1 -> Bool1; 2 -> Bool2; 3 -> Bool3; 4 -> Bool4; else -> invalidOp; }

        fun BYTE(count: Int) =
			when (count) { 0 -> TVOID; 1 -> SByte1; 2 -> SByte2; 3 -> SByte3; 4 -> SByte4; else -> invalidOp; }

		fun UBYTE(count: Int) =
			when (count) { 0 -> TVOID; 1 -> UByte1; 2 -> UByte2; 3 -> UByte3; 4 -> UByte4; else -> invalidOp; }

		fun SHORT(count: Int) =
			when (count) { 0 -> TVOID; 1 -> SShort1; 2 -> SShort2; 3 -> SShort3; 4 -> SShort4; else -> invalidOp; }

		fun USHORT(count: Int) =
			when (count) { 0 -> TVOID; 1 -> UShort1; 2 -> UShort2; 3 -> UShort3; 4 -> UShort4; else -> invalidOp; }

		fun INT(count: Int) =
			when (count) { 0 -> TVOID; 1 -> SInt1; 2 -> SInt2; 3 -> SInt3; 4 -> SInt4; else -> invalidOp; }

		fun FLOAT(count: Int) =
			when (count) { 0 -> TVOID; 1 -> Float1; 2 -> Float2; 3 -> Float3; 4 -> Float4; else -> invalidOp; }

        fun MAT(count: Int) =
            when (count) { 0 -> TVOID; 1 -> Float1; 2 -> Mat2; 3 -> Mat3; 4 -> Mat4; else -> invalidOp; }
	}

}

//val out_Position = Output("gl_Position", VarType.Float4)
//val out_FragColor = Output("gl_FragColor", VarType.Float4)

enum class ShaderType {
	VERTEX, FRAGMENT
}

open class Operand(open val type: VarType) {
    val elementCount get() = type.elementCount
}

enum class Precision { DEFAULT, LOW, MEDIUM, HIGH }

open class OperandWithArray(type: VarType, val arrayCount: Int) : Operand(type) {
    val kind: VarKind = type.kind
    val stride: Int = type.elementCount
    val totalElements: Int = stride * arrayCount
    val totalBytes: Int = type.bytesSize * arrayCount
}

sealed class Variable(val name: String, type: VarType, arrayCount: Int, val precision: Precision = Precision.DEFAULT) : OperandWithArray(type, arrayCount) {
    constructor(name: String, type: VarType, precision: Precision = Precision.DEFAULT) : this(name, type, 1, precision)

    val indexNames = Array(arrayCount) { "$name[$it]" }
    var id: Int = 0
	var data: Any? = null
    inline fun <reified T : Variable> mequals(other: Any?) = (other is T) && (this.id == other.id) && (this.type == other.type) && (this.arrayCount == other.arrayCount) && (this.name == other.name)
    inline fun mhashcode() = id.hashCode() + (type.hashCode() * 7) + (name.hashCode() * 11)
    override fun equals(other: Any?): Boolean = mequals<Variable>(other)
    override fun hashCode(): Int = mhashcode()
}

sealed class VariableWithOffset(
    name: String,
    type: VarType,
    arrayCount: Int,
    precision: Precision = Precision.DEFAULT,
    val offset: Int? = null,
) : Variable(name, type, arrayCount, precision) {
}

open class Attribute(
	name: String,
	type: VarType,
	val normalized: Boolean,
	offset: Int? = null,
	val active: Boolean = true,
    precision: Precision = Precision.DEFAULT,
    val divisor: Int = 0,
    val fixedLocation: Int
) : VariableWithOffset(name, type, 1, precision, offset) {
	constructor(
        name: String,
        type: VarType,
        normalized: Boolean,
        precision: Precision = Precision.DEFAULT,
        fixedLocation: Int
    ) : this(name, type, normalized, null, true, precision, fixedLocation = fixedLocation)

    fun copy(
        name: String = this.name,
        type: VarType = this.type,
        normalized: Boolean = this.normalized,
        offset: Int? = this.offset,
        active: Boolean = this.active,
        precision: Precision = this.precision,
        divisor: Int = this.divisor,
        fixedLocation: Int = this.fixedLocation,
    ) = Attribute(name, type, normalized, offset, active, precision, divisor, fixedLocation = fixedLocation)
    fun inactived() = copy(active = false)
    fun withDivisor(divisor: Int) = copy(divisor = divisor)
    fun withFixedLocation(fixedLocation: Int) = copy(fixedLocation = fixedLocation)
	override fun toString(): String = "Attribute($name)"
    fun toStringEx(): String = "Attribute($name, type=$type, normalized=$normalized, offset=$offset, active=$active, precision=$precision, divisor=$divisor, fixedLocation=$fixedLocation)"
    override fun equals(other: Any?): Boolean = mequals<Attribute>(other) && this.normalized == (other as Attribute).normalized && this.offset == other.offset && this.active == other.active
    override fun hashCode(): Int {
        var out = mhashcode()
        out *= 7; out += normalized.hashCode()
        out *= 7; out += offset.hashCode()
        out *= 7; out += active.hashCode()
        return out
    }

    operator fun getValue(thisRef: Any?, property: KProperty<*>): Attribute = this

    class Provider(val type: VarType, val normalized: Boolean, val precision: Precision = Precision.DEFAULT, val fixedLocation: Int) {
        operator fun provideDelegate(thisRef: Any?, property: KProperty<*>): Attribute = Attribute(property.name, type, normalized, precision, fixedLocation)
    }
}
fun Attribute(
    type: VarType,
    normalized: Boolean,
    precision: Precision = Precision.DEFAULT,
    fixedLocation: Int,
): Attribute.Provider = Attribute.Provider(type, normalized, precision, fixedLocation)

open class Varying(name: String, type: VarType, arrayCount: Int, precision: Precision = Precision.DEFAULT) : Variable(name, type, arrayCount, precision) {
    constructor(name: String, type: VarType, precision: Precision = Precision.DEFAULT) : this(name, type, 1, precision)
	override fun toString(): String = "Varying($name)"
    override fun equals(other: Any?): Boolean = mequals<Varying>(other)
    override fun hashCode(): Int = mhashcode()
    operator fun getValue(thisRef: Any?, property: KProperty<*>): Varying = this

    class Provider(val type: VarType, val arrayCount: Int, val precision: Precision = Precision.DEFAULT) {
        operator fun provideDelegate(thisRef: Any?, property: KProperty<*>): Varying = Varying(property.name, type, arrayCount, precision)
    }
}
fun Varying(type: VarType, arrayCount: Int = 1, precision: Precision = Precision.DEFAULT): Varying.Provider = Varying.Provider(type, arrayCount, precision)

fun Sampler(index: Int, stype: SamplerVarType): Sampler.Provider = Sampler.Provider(index, stype)
open class Sampler(
    name: String,
    val index: Int,
    val stype: SamplerVarType
) : Variable(name, stype.vtype) {
    override fun toString(): String = "Sampler($name, $index, $stype)"
    override fun equals(other: Any?): Boolean = mequals<Sampler>(other)
    override fun hashCode(): Int = mhashcode()
    operator fun getValue(thisRef: Any?, property: KProperty<*>): Sampler = this

    class Provider(val index: Int, val stype: SamplerVarType) {
        operator fun provideDelegate(thisRef: Any?, property: KProperty<*>): Sampler = Sampler(property.name, index, stype)
    }
}

open class Uniform(
    name: String, type: VarType, arrayCount: Int, precision: Precision = Precision.DEFAULT, offset: Int? = null,
    val typedUniform: TypedUniform<*>
) : VariableWithOffset(
    name, type, arrayCount, precision, offset
) {
    init {
        if (type.isSampler) error("Can't create uniform '$name' with a sampler type '$type', please use `Sampler` class instead")
    }

    val totalElementCount: Int get() = type.elementCount * arrayCount

    //constructor(name: String, type: VarType, precision: Precision = Precision.DEFAULT) : this(name, type, 1, precision)
	override fun toString(): String = "Uniform($name)"
    override fun equals(other: Any?): Boolean = mequals<Uniform>(other)
    override fun hashCode(): Int = mhashcode()
    operator fun getValue(thisRef: Any?, property: KProperty<*>): Uniform = this

    //class Provider(val type: VarType, val arrayCount: Int, val precision: Precision = Precision.DEFAULT) {
    //    operator fun provideDelegate(thisRef: Any?, property: KProperty<*>): Uniform = Uniform(property.name, type, arrayCount, precision)
    //}
}

class UniformBlockBase(val fixedLocation: Int = -1)

//fun Uniform(type: VarType, arrayCount: Int = 1, precision: Precision = Precision.DEFAULT): Uniform.Provider = Uniform.Provider(type, arrayCount, precision)

open class Temp(id: Int, type: VarType, arrayCount: Int, precision: Precision = Precision.DEFAULT) : Variable("temp$id", type, arrayCount, precision) {
    constructor(id: Int, type: VarType, precision: Precision = Precision.DEFAULT) : this(id, type, 1, precision)
	override fun toString(): String = "Temp($name)"
    override fun equals(other: Any?): Boolean = mequals<Temp>(other)
    override fun hashCode(): Int = mhashcode()
}

open class Arg(name: String, type: VarType, arrayCount: Int = 1, precision: Precision = Precision.DEFAULT) : Variable(name, type, arrayCount, precision) {
    override fun toString(): String = "Arg($name)"
    override fun equals(other: Any?): Boolean = mequals<Arg>(other)
    override fun hashCode(): Int = mhashcode()
    operator fun getValue(thisRef: Any?, property: KProperty<*>): Arg = this
}

object Output : Varying("out", VarType.Float4) {
	override fun toString(): String = "Output"
    override fun equals(other: Any?): Boolean = mequals<Output>(other)
    override fun hashCode(): Int = mhashcode()
}

inline fun Shader.appending(block: Program.Builder.() -> Unit): FragmentShader {
    // @TODO: Raw shaders don't support appending
    if (this.isRaw) return this
    val pair = ProgramBuilderDefault().WITH(this).also(block)._buildFuncs()
    return Shader(this.type, pair.first, pair.second)
}

inline fun Program.replacingVertex(extraName: String, block: ProgramBuilderDefault.() -> Unit): Program =
    this.copy(vertex = VertexShader(block), name = "$name-$extraName")
inline fun Program.replacingFragment(extraName: String, block: ProgramBuilderDefault.() -> Unit): Program =
    this.copy(fragment = FragmentShader(block), name = "$name-$extraName")

inline fun Program.appendingVertex(extraName: String, block: Program.Builder.() -> Unit): Program =
    this.copy(vertex = this.vertex.appending(block), name = "$name-$extraName")

inline fun Program.appendingFragment(extraName: String, block: Program.Builder.() -> Unit): Program =
    this.copy(fragment = this.fragment.appending(block), name = "$name-$extraName")

data class UniformInProgram(val uniform: Uniform, val index: Int)

data class Program(val vertex: VertexShader, val fragment: FragmentShader, val name: String = "program-${vertex.name}-${fragment.name}") : AutoCloseable {
	val uniforms = (vertex.uniforms + fragment.uniforms)
    val typedUniforms = (vertex.typedUniforms + fragment.typedUniforms)
    val uniformBlocks = typedUniforms.map { it.block }.distinct()
    val samplers = (vertex.samplers + fragment.samplers)
    // @TODO: Proper indices
    val uniformsToIndex = uniforms.withIndex().associate { it.value to UniformInProgram(it.value, it.index) }
	val attributes = vertex.attributes + fragment.attributes
    val cachedHashCode = (vertex.hashCode() * 11) + (fragment.hashCode() * 7) + name.hashCode()
    override fun hashCode(): Int = cachedHashCode
    override fun equals(other: Any?): Boolean = (this === other) || ((other is Program) && (this.vertex == other.vertex)
        && (this.fragment == other.fragment) && (this.name == other.name))

    override fun close() {
	}

	override fun toString(): String =
		"Program(name=$name, attributes=${attributes.map { it.name }}, uniforms=${uniforms.map { it.name }})"

    data class Ternary(val cond: Operand, val otrue: Operand, val ofalse: Operand) : Operand(otrue.type)
    data class Unop(val op: String, val right: Operand) : Operand(right.type) {
        fun toBinop(): Binop {
            val left = when (right.type.kind) {
                VarKind.TINT -> IntLiteral(0)
                else -> FloatLiteral(0f)
            }
            return Binop(left, op, right)
        }
    }
    data class Binop(val left: Operand, val op: String, val right: Operand) : Operand(left.type)
    data class IntLiteral(val value: Int) : Operand(VarType.Int1)
    data class FloatLiteral(val value: Float) : Operand(VarType.Float1)
    data class BoolLiteral(val value: Boolean) : Operand(VarType.Bool1)
    data class Vector(override val type: VarType, val ops: Array<out Operand>) : Operand(type) {
        override fun equals(other: Any?): Boolean = (other is Vector) && (this.type == other.type) && (this.ops.contentEquals(other.ops))
        override fun hashCode(): Int = (type.hashCode() * 7) + (ops.contentHashCode())
    }
    data class Swizzle(val left: Operand, val swizzle: String) : Operand(left.type.withElementCount(swizzle.length))
	data class ArrayAccess(val left: Operand, val index: Operand) : Operand(left.type)

    abstract class BaseFunc(type: VarType) : Operand(type) {
        abstract val name: String
        abstract val ops: List<Operand>
    }

    data class Func(override val name: String, override val ops: List<Operand>, override val type: VarType = VarType.Float1) : BaseFunc(type) {
		constructor(name: String, vararg ops: Operand, type: VarType = VarType.Float1) : this(name, ops.toList(), type)
	}

    open class CustomFunc(open val ref: FuncRef, override val ops: List<Operand>) : BaseFunc(ref.rettype) {
        override val name: String get() = ref.name

    }

    data class CustomFunc0(override val ref: FuncRef) : CustomFunc(ref, listOf())
    data class CustomFunc1(override val ref: FuncRef, val p0: Operand) : CustomFunc(ref, listOf(p0))
    data class CustomFunc2(override val ref: FuncRef, val p0: Operand, val p1: Operand) : CustomFunc(ref, listOf(p0, p1))
    data class CustomFunc3(override val ref: FuncRef, val p0: Operand, val p1: Operand, val p2: Operand) : CustomFunc(ref, listOf(p0, p1, p2))
    data class CustomFunc4(override val ref: FuncRef, val p0: Operand, val p1: Operand, val p2: Operand, val p3: Operand) : CustomFunc(ref, listOf(p0, p1, p2, p3))
    data class CustomFunc5(override val ref: FuncRef, val p0: Operand, val p1: Operand, val p2: Operand, val p3: Operand, val p4: Operand) : CustomFunc(ref, listOf(p0, p1, p2, p3, p4))
    data class CustomFuncN(override val ref: FuncRef, override val ops: List<Operand>) : CustomFunc(ref, ops)

    interface FuncRef {
        val name: String
        val rettype: VarType
        val args: List<Pair<String, VarType>>
    }

    interface FuncRef0 : FuncRef
    interface FuncRef1 : FuncRef
    interface FuncRef2 : FuncRef
    interface FuncRef3 : FuncRef
    interface FuncRef4 : FuncRef
    interface FuncRef5 : FuncRef
    interface FuncRefN : FuncRef

	sealed class Stm {
		data class Stms(val stms: List<Stm>) : Stm()
        data class Set(val to: Operand, val from: Operand) : Stm()
        data class Return(val result: Operand?) : Stm()
        object Break : Stm() {
            override fun toString(): String = "Break"
        }
        object Continue : Stm() {
            override fun toString(): String = "Continue"
        }
        object Discard : Stm() {
            override fun toString(): String = "Discard"
        }
        data class If(val cond: Operand, val tbody: Stm, var fbody: Stm? = null) : Stm()
        data class ForSimple(val loopVar: Variable, val min: Operand, val maxExclusive: Operand, val body: Stm) : Stm()
        data class Raw(val strings: Map<String, String>, val other: Stm? = null) : Stm() {
            fun stringOrNull(name: String): String? = strings[name]
            fun string(name: String, default: String = ""): String = stringOrNull(name) ?: default
        }
	}

    class BuilderContext {
        val outputFuncs: ArrayList<FuncDecl> = arrayListOf()
        var tempLastId = 3
    }

    interface ExpressionBuilder {
        companion object {
            @PublishedApi internal val INSTANCE = object : ExpressionBuilder {}
            inline operator fun <T> invoke(block: ExpressionBuilder.() -> T): T = block(INSTANCE)
        }

        //infix fun Operand.set(to: Operand) = Stm.Set(this, to)
        val out: Output get() = Output
        //fun out(to: Operand) = Stm.Set(if (type == ShaderType.VERTEX) out_Position else out_FragColor, to)

        fun sin(arg: Operand): Operand = Func("sin", arg)
        fun cos(arg: Operand): Operand = Func("cos", arg)
        fun tan(arg: Operand): Operand = Func("tan", arg)

        fun asin(arg: Operand): Operand = Func("asin", arg)
        fun acos(arg: Operand): Operand = Func("acos", arg)
        fun atan(y_over_x: Operand): Operand = Func("atan", y_over_x)
        fun atan(y: Operand, x: Operand): Operand = Func("atan", y, x)

        // @TODO: https://en.wikipedia.org/wiki/Atan2#Definition_and_computation (IF chain)
        //fun atan2(a: Operand, b: Operand): Operand = atan(a / b) * 2f.lit

        fun radians(arg: Operand): Operand = Func("radians", arg)
        fun degrees(arg: Operand): Operand = Func("degrees", arg)

        // Sampling
        fun texture2D(sampler: Operand, coord: Operand): Operand = Func("texture2D", sampler, coord, type = VarType.Float4)
        fun texture(sampler: Operand, P: Operand): Operand = Func("texture", sampler, P)

        fun func(name: String, vararg args: Operand): Operand = Func(name, *args.map { it }.toTypedArray())

        fun TERNARY(cond: Operand, otrue: Operand, ofalse: Operand): Operand = Ternary(cond, otrue, ofalse)

        // CAST
        fun int(v: Operand): Operand = Func("int", v)
        fun float(v: Operand): Operand = Func("float", v)

        fun pow(b: Operand, e: Operand): Operand = Func("pow", b, e)
        fun exp(v: Operand): Operand = Func("exp", v)
        fun exp2(v: Operand): Operand = Func("exp2", v)
        fun log(v: Operand): Operand = Func("log", v)
        fun log2(v: Operand): Operand = Func("log2", v)
        fun sqrt(v: Operand): Operand = Func("sqrt", v)
        fun inversesqrt(v: Operand): Operand = Func("inversesqrt", v)

        fun abs(v: Operand): Operand = Func("abs", v)
        fun sign(v: Operand): Operand = Func("sign", v)
        fun ceil(v: Operand): Operand = Func("ceil", v)
        fun floor(v: Operand): Operand = Func("floor", v)

        /** The fractional part of v. This is calculated as v - floor(v). */
        fun fract(v: Operand): Operand = Func("fract", v)

        fun clamp01(v: Operand): Operand = clamp(v, 0f.lit, 1f.lit)
        fun clamp(v: Operand, min: Operand, max: Operand): Operand = Func("clamp", v, min, max)
        fun min(a: Operand, b: Operand): Operand = Func("min", a, b)
        fun max(a: Operand, b: Operand): Operand = Func("max", a, b)
        fun mod(a: Operand, b: Operand): Operand = Func("mod", a, b)

        //fun lerp(a: Operand, b: Operand, c: Operand): Operand = Func("lerp", a, b, c)

        // https://learnwebgl.brown37.net/12_shader_language/documents/webgl-reference-card-1_0.pdf
        // #extension GL_OES_standard_derivatives : enable
        // https://stackoverflow.com/questions/68573364/enable-extension-and-fwidth-in-glsl
        fun fwidth(a: Operand): Operand = Func("fwidth", a)
        fun dFdx(a: Operand): Operand = Func("dFdx", a)
        fun dFdy(a: Operand): Operand = Func("dFdy", a)

        //lessThan


        //@JvmName("modInfix") infix fun Operand.mod(that: Operand): Operand = mod(this, that)

        fun mix(a: Operand, b: Operand, step: Operand): Operand =
            Func("mix", a, b, step)
        fun step(a: Operand, b: Operand): Operand = Func("step", a, b)
        fun smoothstep(a: Operand, b: Operand, c: Operand): Operand =
            Func("smoothstep", a, b, c)

        fun length(a: Operand): Operand = Func("length", a)
        fun distance(a: Operand, b: Operand): Operand = Func("distance", a, b)
        fun dot(a: Operand, b: Operand): Operand = Func("dot", a, b)
        fun cross(a: Operand, b: Operand): Operand = Func("cross", a, b)
        fun normalize(a: Operand): Operand = Func("normalize", a)
        fun faceforward(a: Operand, b: Operand, c: Operand): Operand =
            Func("faceforward", a, b, c)
        fun reflect(a: Operand, b: Operand): Operand = Func("reflect", a, b)
        fun refract(a: Operand, b: Operand, c: Operand): Operand =
            Func("refract", a, b, c)

        val Int.lit: IntLiteral get() = IntLiteral(this)
        @Deprecated("", ReplaceWith("this.toFloat().lit"))
        val Double.lit: FloatLiteral get() = this.toFloat().lit
        val Float.lit: FloatLiteral get() = FloatLiteral(this)
        val Boolean.lit: BoolLiteral get() = BoolLiteral(this)
        //val Number.lit: Operand get() = this // @TODO: With Kotlin.JS you cannot differentiate Int, Float, Double with 'is'. Since it generates typeof $receiver === 'number' for all of them
        fun lit(type: VarType, vararg ops: Operand): Operand = Vector(type, ops)

        fun vec1(vararg ops: Operand): Operand = Vector(VarType.Float1, ops)
        fun vec2(vararg ops: Operand): Operand = Vector(VarType.Float2, ops)
        fun vec3(vararg ops: Operand): Operand = Vector(VarType.Float3, ops)
        fun vec4(vararg ops: Operand): Operand = Vector(VarType.Float4, ops)

        fun mat2(vararg ops: Operand): Operand = Vector(VarType.Mat2, ops)
        fun mat3(vararg ops: Operand): Operand = Vector(VarType.Mat3, ops)
        fun mat4(vararg ops: Operand): Operand = Vector(VarType.Mat4, ops)

        fun vec1(vararg ops: Float): Operand = Vector(VarType.Float1, Array(ops.size) { ops[it].lit })
        fun vec2(vararg ops: Float): Operand = Vector(VarType.Float2, Array(ops.size) { ops[it].lit })
        fun vec3(vararg ops: Float): Operand = Vector(VarType.Float3, Array(ops.size) { ops[it].lit })
        fun vec4(vararg ops: Float): Operand = Vector(VarType.Float4, Array(ops.size) { ops[it].lit })

        //fun Operand.swizzle(swizzle: String): Operand = Swizzle(this, swizzle)

        operator fun Operand.get(index: Int): Operand {
            return when {
                this is OperandWithArray -> ArrayAccess(this, index.lit)
                this.type.isMatrix -> ArrayAccess(this, index.lit)
                else -> when (index) {
                    0 -> this.x
                    1 -> this.y
                    2 -> this.z
                    3 -> this.w
                    else -> error("Invalid index $index")
                }
            }
        }
        operator fun Operand.get(swizzle: String): Operand = Swizzle(this, swizzle)
        val Operand.x: Operand get() = this["x"]
        val Operand.y: Operand get() = this["y"]
        val Operand.z: Operand get() = this["z"]
        val Operand.w: Operand get() = this["w"]

        val Operand.r: Operand get() = this["x"]
        val Operand.g: Operand get() = this["y"]
        val Operand.b: Operand get() = this["z"]
        val Operand.a: Operand get() = this["w"]

        operator fun Operand.unaryMinus(): Operand = Unop("-", this)
        fun Operand.not(): Operand = Unop("!", this)

        operator fun Operand.minus(that: Operand): Operand = Binop(this, "-", that)
        operator fun Operand.plus(that: Operand): Operand = Binop(this, "+", that)
        operator fun Operand.times(that: Operand): Operand = Binop(this, "*", that)
        operator fun Operand.div(that: Operand): Operand = Binop(this, "/", that)
        operator fun Operand.rem(that: Operand): Operand = Binop(this, "%", that)

        infix fun Operand.eq(that: Operand): Operand = Binop(this, "==", that)
        infix fun Operand.ne(that: Operand): Operand = Binop(this, "!=", that)
        infix fun Operand.lt(that: Operand): Operand = Binop(this, "<", that)
        infix fun Operand.le(that: Operand): Operand = Binop(this, "<=", that)
        infix fun Operand.gt(that: Operand): Operand = Binop(this, ">", that)
        infix fun Operand.ge(that: Operand): Operand = Binop(this, ">=", that)

        operator fun Float.minus(that: Operand): Operand = this.lit.minus(that)
        operator fun Float.plus(that: Operand): Operand = this.lit.plus(that)
        operator fun Float.times(that: Operand): Operand = this.lit.times(that)
        operator fun Float.div(that: Operand): Operand = this.lit.div(that)
        operator fun Float.rem(that: Operand): Operand = this.lit.rem(that)

        operator fun Operand.minus(that: Float): Operand = this.minus(that.lit)
        operator fun Operand.plus(that: Float): Operand = this.plus(that.lit)
        operator fun Operand.times(that: Float): Operand = this.times(that.lit)
        operator fun Operand.div(that: Float): Operand = this.div(that.lit)
        operator fun Operand.rem(that: Float): Operand = this.rem(that.lit)

        infix fun Operand.eq(that: Float): Operand = this.eq(that.lit)
        infix fun Operand.ne(that: Float): Operand = this.ne(that.lit)
        infix fun Operand.lt(that: Float): Operand = this.lt(that.lit)
        infix fun Operand.le(that: Float): Operand = this.le(that.lit)
        infix fun Operand.gt(that: Float): Operand = this.gt(that.lit)
        infix fun Operand.ge(that: Float): Operand = this.ge(that.lit)

        infix fun Operand.and(that: Operand): Operand = Binop(this, "&&", that)
        infix fun Operand.or(that: Operand): Operand = Binop(this, "||", that)

        fun Operand.inRange(low: Operand, high: Operand): Operand {
            return (this ge low) and (this lt high)
        }
    }

	// http://mew.cx/glsl_quickref.pdf
	open class Builder(
        private val context: BuilderContext = BuilderContext()
    ) : VarTypeAccessor, ExpressionBuilder {
        @PublishedApi
        internal val outputStms: ArrayList<Stm> = arrayListOf<Stm>()

        constructor(parent: Builder) : this(parent.context)

        //fun createChildBuilder(): Builder = Builder(type)
        fun createChildBuilder(): Builder = Builder(this)
        fun createChildFuncBuilder(): FuncBuilder = FuncBuilder(this)
        fun _build(): Stm = Stm.Stms(outputStms.toList())
        fun _funcs(): List<FuncDecl> {
            val funcs = LinkedHashMap<String, FuncDecl>()
            object : Visitor<Unit>(Unit) {
                override fun visit(func: CustomFunc) {
                    funcs[func.name] = func.ref as FuncDecl
                }
            }.visit(outputStms)
            for (func in context.outputFuncs) {
                funcs[func.name] = func
            }
            return funcs.values.toList()
        }
        fun _buildFuncs(): Pair<Stm, List<FuncDecl>> = _build() to _funcs()

		//inner class BuildIf(val stmIf: Stm.If) {
		//	fun ELSEIF(cond: Operand, callback: Builder.() -> Unit): BuildIf {
		//		//val body = Builder(type)
		//		//body.callback()
		//		//outputStms += Stm.If(cond, Stm.Stms(body.outputStms))
		//		TODO()
		//	}
//
		//	infix fun ELSE(callback: Builder.() -> Unit) {
		//		//val body = Builder(type)
		//		//body.callback()
		//		//outputStms += Stm.If(cond, Stm.Stms(body.outputStms))
		//		TODO()
		//	}
		//}

        class FuncBuilder(parent: Builder) : Builder(parent) {
            val args = arrayListOf<Arg>()

            fun ARG(name: String, type: VarType): Arg {
                return Arg(name, type).also { args += it }
            }

            fun ARG(type: VarType): ArgProvider = ArgProvider(type, this)

            class ArgProvider(val type: VarType, val builder: FuncBuilder) {
                operator fun provideDelegate(thisRef: Any?, property: KProperty<*>): Arg =
                    Arg(property.name, type).also { builder.args += it }
            }

            //fun RETURN(expr: Operand) {
            //    outputStms.add(Stm.Return(expr))
            //}
        }

        fun FUNC(name: String, rettype: VarType, block: FuncBuilder.() -> Unit): FuncRefN {
            val funcBuild = createChildFuncBuilder().apply(block)
            return FuncDecl(name, rettype, funcBuild.args.map { it.name to it.type }, funcBuild._build()).also {
                context.outputFuncs += it
            }
        }

        // NOTE: dummy: Unit = Unit is there to force returns to be a named argument

        fun FUNCN(returns: VarType, block: FuncBuilder.() -> Unit): FuncProvider<FuncRefN> = FuncProvider(returns, emptyList(), this) { block() }
        fun FUNC(dummy: Unit = Unit, returns: VarType, block: FuncBuilder.() -> Unit): FuncProvider<FuncRef0> = FuncProvider(returns, listOf(), this) { block() }
        fun FUNC(p0: VarType, dummy: Unit = Unit, returns: VarType, block: FuncBuilder.(p0: Arg) -> Unit): FuncProvider<FuncRef1> = FuncProvider(returns, listOf(p0), this) { block(it[0]) }
        fun FUNC(p0: VarType, p1: VarType, dummy: Unit = Unit, returns: VarType, block: FuncBuilder.(p0: Arg, p1: Arg) -> Unit): FuncProvider<FuncRef2> = FuncProvider(returns, listOf(p0, p1), this) { block(it[0], it[1]) }
        fun FUNC(p0: VarType, p1: VarType, p2: VarType, dummy: Unit = Unit, returns: VarType, block: FuncBuilder.(p0: Arg, p1: Arg, p2: Arg) -> Unit): FuncProvider<FuncRef3> = FuncProvider(returns, listOf(p0, p1, p2), this) { block(it[0], it[1], it[2]) }
        fun FUNC(
            p0: VarType,
            p1: VarType,
            p2: VarType,
            p3: VarType,
            dummy: Unit = Unit,
            returns: VarType,
            block: FuncBuilder.(p0: Arg, p1: Arg, p2: Arg, p3: Arg) -> Unit
        ): FuncProvider<FuncRef4> = FuncProvider(returns, listOf(p0, p1, p2, p3), this) { block(it[0], it[1], it[2], it[3]) }
        fun FUNC(
            p0: VarType,
            p1: VarType,
            p2: VarType,
            p3: VarType,
            p4: VarType,
            dummy: Unit = Unit,
            returns: VarType,
            block: FuncBuilder.(p0: Arg, p1: Arg, p2: Arg, p3: Arg, p4: Arg) -> Unit
        ): FuncProvider<FuncRef5> = FuncProvider(returns, listOf(p0, p1, p2, p3, p4), this) { block(it[0], it[1], it[2], it[3], it[4]) }

        fun TEMP(type: VarType): Temp = Temp(context.tempLastId++, type)

        class FuncDeclGetter<T : FuncRef>(val decl: FuncDecl) {
            operator fun getValue(thisRef: Any?, property: KProperty<*>): T = decl as T
        }

        class FuncProvider<T : Program.FuncRef>(val rettype: VarType, val varTypes: List<VarType>, val builder: Program.Builder, val block: FuncBuilder.(args: List<Arg>) -> Unit) {
            operator fun provideDelegate(thisRef: Any?, property: KProperty<*>): FuncDeclGetter<T> {
                return FuncDeclGetter(builder.FUNC(property.name, rettype) {
                    val args = varTypes.mapIndexed { index, type -> ARG("p$index", type) }
                    block(this, args)
                } as FuncDecl)
            }
        }

        operator fun FuncRefN.invoke(vararg operands: Operand): CustomFunc = CustomFunc(this, operands.toList())
        operator fun FuncRef0.invoke(): CustomFunc = CustomFunc(this, listOf())
        operator fun FuncRef1.invoke(p0: Operand): CustomFunc = CustomFunc(this, listOf(p0))
        operator fun FuncRef2.invoke(p0: Operand, p1: Operand): CustomFunc = CustomFunc(this, listOf(p0, p1))
        operator fun FuncRef3.invoke(p0: Operand, p1: Operand, p2: Operand): CustomFunc = CustomFunc(this, listOf(p0, p1, p2))
        operator fun FuncRef4.invoke(p0: Operand, p1: Operand, p2: Operand, p3: Operand): CustomFunc = CustomFunc(this, listOf(p0, p1, p2, p3))
        operator fun FuncRef5.invoke(p0: Operand, p1: Operand, p2: Operand, p3: Operand, p4: Operand): CustomFunc = CustomFunc(this, listOf(p0, p1, p2, p3, p4))

        // infix // Single value parameter
        @Deprecated("Experimental, doesn't work yet")
        fun Stm.If.ELSE_IF(cond: Operand, callback: Builder.() -> Unit): Stm.If {
            val body = createChildBuilder()
            body.callback()
            val ifBody = IF(cond) {
                callback()
            }
            this.fbody = ifBody
            return ifBody
        }

		infix fun Stm.If.ELSE(callback: Builder.() -> Unit) {
			val body = createChildBuilder()
			body.callback()
			this.fbody = body._build()
		}

		inline fun IF(cond: Operand, callback: Builder.() -> Unit): Stm.If {
			val body = createChildBuilder()
			body.callback()
			val stmIf = Stm.If(cond, body._build())
			outputStms += stmIf
			return stmIf
		}

        // WHEN NOT A CONSTANT, REQUIRES WEBGL 2.0 and OPENGL ES 3.0
        @KoragExperimental
        inline fun FOR_0_UNTIL(len: Operand, callback: Builder.(Operand) -> Unit): Stm.ForSimple {
            val temp = createTemp(VarType.Int1)
            val body = createChildBuilder().apply { callback(temp) }
            val stmFor = Stm.ForSimple(temp, 0.lit, len, body._build())
            outputStms += stmFor
            return stmFor
        }

        // This construct is compatible with WebGL 1
        inline fun FOR_0_UNTIL_FIXED(len: Int, callback: Builder.(Operand) -> Unit): Stm.ForSimple {
            return FOR_0_UNTIL(len.lit, callback)
        }

        // This construct is compatible with WebGL 1
        inline fun FOR_0_UNTIL_FIXED_BREAK(len: Operand, maxLen: Int = 1024, callback: Builder.(Operand) -> Unit): Stm.ForSimple {
            return FOR_0_UNTIL_FIXED(maxLen) { x ->
                IF(x ge len) {
                    BREAK()
                }
                callback(x)
            }
        }

        fun IF_ELSE_BINARY_LOOKUP(ref: Operand, min: Int, max: Int, block: Builder.(Int) -> Unit) {
            if (min >= max) {
                block(min)
            } else if (max - min <= 1) {
                IF(ref eq min.toFloat().lit) { block(min) } ELSE { block(max) }
            } else {
                val middle = ((min + max) / 2)
                IF(ref le middle.toFloat().lit) {
                    IF_ELSE_BINARY_LOOKUP(ref, min, middle, block)
                } ELSE {
                    IF_ELSE_BINARY_LOOKUP(ref, middle + 1, max, block)
                }
            }
        }

        fun IF_ELSE_LIST(ref: Operand, min: Int, max: Int, block: Builder.(Int) -> Unit) {
            if (min >= max) {
                block(min)
            } else {
                IF(ref eq min.toFloat().lit) { block(min) } ELSE { IF_ELSE_LIST(ref, min + 1, max, block) }
            }
        }

        fun WITH(shader: Shader): Builder {
            PUT(shader)
            return this
        }

        fun PUT(shader: Shader) {
            if (shader.stm is Stm.Stms) {
                outputStms.addAll(shader.stm.stms)
            } else {
                outputStms.add(shader.stm)
            }
            context.outputFuncs.addAll(shader.functions)
        }
		fun SET(target: Operand, expr: Operand) { outputStms += Stm.Set(target, expr) }
		fun DISCARD() { outputStms += Stm.Discard }
        fun BREAK() { outputStms += Stm.Break }
        fun CONTINUE() { outputStms += Stm.Continue }
        fun RETURN(operand: Operand? = null) { outputStms += Stm.Return(operand) }

        fun createTemp(type: VarType, arrayCount: Int) = Temp(context.tempLastId++, type, arrayCount)
		fun createTemp(type: VarType) = Temp(context.tempLastId++, type, 1)

        @Deprecated("", ReplaceWith("SET(this, from)"))
		infix fun Operand.set(from: Operand) { SET(this, from) }
        @Deprecated("", ReplaceWith("SET(this, from)"))
		infix fun Operand.setTo(from: Operand) { SET(this, from) }

        @Deprecated("", ReplaceWith("SET(this, from)"))
		fun Operand.assign(from: Operand) { SET(this, from) }
	}

	open class Visitor<E>(val default: E) {
        open fun visit(func: FuncDecl) {
            visit(func.stm)
        }

        open fun visit(stms: List<Stm?>) {
            for (stm in stms) visit(stm)
        }

		open fun visit(stm: Stm?) = when (stm) {
		    null -> Unit
			is Stm.Stms -> visit(stm)
			is Stm.Set -> visit(stm)
			is Stm.If -> visit(stm)
            is Stm.ForSimple -> visit(stm)
			is Stm.Discard -> visit(stm)
            is Stm.Continue -> visit(stm)
            is Stm.Break -> visit(stm)
            is Stm.Raw -> visit(stm)
            is Stm.Return -> visit(stm)
        }

		open fun visit(stms: Stm.Stms) {
			for (stm in stms.stms) visit(stm)
		}

		open fun visit(stm: Stm.If) {
			visit(stm.cond)
			visit(stm.tbody)
            visit(stm.fbody)
		}

        open fun visit(stm: Stm.ForSimple) {
            visit(stm.loopVar)
            visit(stm.min)
            visit(stm.body)
        }

        open fun visit(stm: Stm.Set) {
			visit(stm.from)
			visit(stm.to)
		}

		open fun visit(stm: Stm.Discard) {
		}

        open fun visit(stm: Stm.Break) {
        }

        open fun visit(stm: Stm.Continue) {
        }

        open fun visit(stm: Stm.Raw) {
            visit(stm.other)
        }

        open fun visit(stm: Stm.Return) {
            stm.result?.let { visit(it) }
        }

        open fun visit(operand: Operand): E = when (operand) {
			is Variable -> visit(operand)
            is Unop -> visit(operand)
			is Binop -> visit(operand)
            is Ternary -> visit(operand)
			is BoolLiteral -> visit(operand)
			is IntLiteral -> visit(operand)
			is FloatLiteral -> visit(operand)
			is Vector -> visit(operand)
			is Swizzle -> visit(operand)
			is ArrayAccess -> visit(operand)
			is BaseFunc -> visit(operand)
			else -> invalidOp("Don't know how to visit operand $operand")
		}

        open fun visit(func: BaseFunc): E {
            for (op in func.ops) visit(op)
            return when (func) {
                is Func -> visit(func)
                is CustomFunc -> visit(func)
                else -> invalidOp("Don't know how to visit func $func")
            }
        }

        open fun visit(func: Func): E {
			return default
		}

        open fun visit(func: CustomFunc): E {
            return default
        }

        open fun visit(operand: Variable): E = when (operand) {
			is Attribute -> visit(operand)
			is Varying -> visit(operand)
            is Sampler -> visit(operand)
			is Uniform -> visit(operand)
            is TypedUniform<*> -> visit(operand)
			is Output -> visit(operand)
			is Temp -> visit(operand)
            is Arg -> visit(operand)
        }

		open fun visit(temp: Temp): E = default
        open fun visit(arg: Arg): E = default
		open fun visit(attribute: Attribute): E = default
		open fun visit(varying: Varying): E = default
		open fun visit(uniform: Uniform): E = default
        open fun visit(sampler: Sampler): E = default
        //open fun visit(uniform: NewTypedUniform<*>): E = default
        open fun visit(typedUniform: TypedUniform<*>): E = visit(typedUniform.uniform)
		open fun visit(output: Output): E = default
        open fun visit(operand: Unop): E {
            visit(operand.right)
            return default
        }
		open fun visit(operand: Binop): E {
			visit(operand.left)
			visit(operand.right)
			return default
		}
        open fun visit(operand: Ternary): E {
            visit(operand.cond)
            visit(operand.otrue)
            visit(operand.ofalse)
            return default
        }

		open fun visit(operand: Swizzle): E {
			visit(operand.left)
			return default
		}

		open fun visit(operand: ArrayAccess): E {
			visit(operand.left)
			visit(operand.index)
			return default
		}

		open fun visit(operand: Vector): E {
			for (op in operand.ops) visit(op)
			return default
		}

		open fun visit(operand: IntLiteral): E = default
		open fun visit(operand: FloatLiteral): E = default
		open fun visit(operand: BoolLiteral): E = default
	}
}

data class Shader(val type: ShaderType, val stm: Program.Stm, val functions: List<FuncDecl>, val name: String? = null) {
    private val stmHashCode = stm.hashCode()
    private val functionsHashCode = functions.hashCode()
    private val cachedHashCode = (type.hashCode() * 17) + stmHashCode + (functionsHashCode * 53)

    val isRaw get() = stm is Program.Stm.Raw

    val globals = GlobalsProgramVisitor().also { it.visit(stm) }

	val uniforms = globals.uniforms
    val typedUniforms = globals.typedUniforms
    val attributes = globals.attributes
    val samplers = globals.samplers

    override fun equals(other: Any?): Boolean = (this === other) || (other is Shader && (this.type == other.type) && (this.cachedHashCode == other.cachedHashCode) && (this.stm == other.stm) && (this.functions == other.functions))
    override fun hashCode(): Int = cachedHashCode
}

typealias VertexShader = Shader
typealias FragmentShader = Shader

fun VertexShader(stm: Program.Stm, funcs: List<FuncDecl> = listOf()): Shader = Shader(ShaderType.VERTEX, stm, funcs)
fun VertexShader(info: Pair<Program.Stm, List<FuncDecl>>): Shader = VertexShader(info.first, info.second)

fun FragmentShader(stm: Program.Stm, funcs: List<FuncDecl> = listOf()): Shader = Shader(ShaderType.FRAGMENT, stm, funcs)
fun FragmentShader(info: Pair<Program.Stm, List<FuncDecl>>): Shader = FragmentShader(info.first, info.second)

@KoragExperimental
fun VertexShader(rawStrings: Map<String, String>, stm: Program.Stm? = null) = VertexShader(Program.Stm.Raw(rawStrings, stm))
@KoragExperimental
fun FragmentShader(rawStrings: Map<String, String>, stm: Program.Stm? = null) = FragmentShader(Program.Stm.Raw(rawStrings, stm))

private const val NAME_GLSL = "GLSL"

@KoragExperimental
@Deprecated("Use VertexShaderRawGlSl instead")
fun VertexShader(stm: Program.Stm, glsl: String) = VertexShader(mapOf(NAME_GLSL to glsl), stm)
@KoragExperimental
@Deprecated("Use FragmentShaderRawGlSl instead")
fun FragmentShader(stm: Program.Stm, glsl: String) = FragmentShader(mapOf(NAME_GLSL to glsl), stm)

inline fun VertexShader(callback: ProgramBuilderDefault.() -> Unit): VertexShader {
	val builder = ProgramBuilderDefault()
	builder.callback()
	return VertexShader(builder._buildFuncs())
}

inline fun FragmentShader(callback: ProgramBuilderDefault.() -> Unit): FragmentShader {
	val builder = ProgramBuilderDefault()
	builder.callback()
	return FragmentShader(builder._buildFuncs())
}

typealias VertexLayout = ProgramLayout<Attribute>

inline val ProgramLayout<Attribute>.attributes: List<Attribute> get() = items
inline val ProgramLayout<Attribute>.attributePositions: IntArrayList get() = _positions

open class ProgramLayout<TVariable : VariableWithOffset>(
    @PublishedApi internal val items: List<TVariable>,
    private val layoutSize: Int?,
    val fixedLocation: Int = -1
): Iterable<TVariable> by items {
	constructor(variables: List<TVariable>) : this(variables, null)
	constructor(vararg variables: TVariable, layoutSize: Int? = null) : this(variables.toFastList(), layoutSize)

	private var _lastPos: Int = 0

	val alignments: IntArrayList = items.mapInt {
		val a = it.type.kind.bytesSize
		if (a <= 1) 1 else a
	}

	@PublishedApi internal val _positions: IntArrayList = items.mapInt {
		_lastPos = when {
            it.offset != null -> it.offset
            else -> _lastPos.nextAlignedTo(it.type.kind.bytesSize)
        }
		val out = _lastPos
		_lastPos += it.type.bytesSize
		out
	}

	val maxAlignment: Int = alignments.maxOrNull() ?: 1
    /** Size in bytes for each vertex */
	val totalSize: Int = layoutSize ?: _lastPos.nextAlignedTo(maxAlignment)

    protected fun names(): String = items.joinToString(", ") { it.name }
	override fun toString(): String = "${this::class.portableSimpleName}[${names()}, fixedLocation=$fixedLocation]"
}

/*
@Deprecated("")
class UniformBlock(uniforms: List<Uniform>, fixedLocation: Int) : ProgramLayout<Uniform>(uniforms, null, fixedLocation = fixedLocation) {
    constructor(vararg uniforms: Uniform, fixedLocation: Int) : this(uniforms.toList(), fixedLocation)
    companion object {
        val EMPTY = UniformBlock(fixedLocation = -1)
    }
}

 */

//typealias UniformLayout = ProgramLayout<Uniform>
//typealias UniformBlock = ProgramLayout<Uniform>
inline val ProgramLayout<Uniform>.uniforms: List<Uniform> get() = items
inline val ProgramLayout<Uniform>.uniformPositions: IntArrayList get() = _positions
