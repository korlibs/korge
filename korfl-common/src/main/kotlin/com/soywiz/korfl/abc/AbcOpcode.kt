package com.soywiz.korfl.abc

import com.soywiz.korio.stream.*

enum class AbcOpcode(val id: Int, val type: AbcOpcode.Kind) {
	Breakpoint(0x01, Kind.BasicOperation),
	Nop(0x02, Kind.BasicOperation),
	Throw(0x03, Kind.BasicOperation),
	GetSuper(0x04, Kind.MultinameOperation),
	SetSuper(0x05, Kind.MultinameOperation),
	DefaultXmlNamespace(0x06, Kind.StringOperation),
	DefaultXmlNamespaceL(0x07, Kind.BasicOperation),
	Kill(0x08, Kind.IntOperation),
	Label(0x09, Kind.LabelOperation),
	IfNotLessThan(0x0C, Kind.ConditionalJumpOperation),
	IfNotLessEqual(0x0D, Kind.ConditionalJumpOperation),
	IfNotGreaterThan(0x0E, Kind.ConditionalJumpOperation),
	IfNotGreaterEqual(0x0F, Kind.ConditionalJumpOperation),
	Jump(0x10, Kind.JumpOperation),
	IfTrue(0x11, Kind.ConditionalJumpOperation),
	IfFalse(0x12, Kind.ConditionalJumpOperation),
	IfEqual(0x13, Kind.ConditionalJumpOperation),
	IfNotEqual(0x14, Kind.ConditionalJumpOperation),
	IfLessThan(0x15, Kind.ConditionalJumpOperation),
	IfLessEqual(0x16, Kind.ConditionalJumpOperation),
	IfGreaterThan(0x17, Kind.ConditionalJumpOperation),
	IfGreaterEqual(0x18, Kind.ConditionalJumpOperation),
	IfStrictEqual(0x19, Kind.ConditionalJumpOperation),
	IfStrictNotEqual(0x1A, Kind.ConditionalJumpOperation),
	LookupSwitch(0x1B, Kind.LookupSwitchOperation),
	PushWith(0x1C, Kind.BasicOperation),
	PopScope(0x1D, Kind.BasicOperation),
	NextName(0x1E, Kind.BasicOperation),
	HasNext(0x1F, Kind.BasicOperation),
	PushNull(0x20, Kind.BasicOperation),
	PushUndefined(0x21, Kind.BasicOperation),
	NextValue(0x23, Kind.BasicOperation),
	PushByte(0x24, Kind.IntOperation),
	PushShort(0x25, Kind.IntOperation),
	PushTrue(0x26, Kind.BasicOperation),
	PushFalse(0x27, Kind.BasicOperation),
	PushNaN(0x28, Kind.BasicOperation),
	Pop(0x29, Kind.BasicOperation),
	Dup(0x2A, Kind.BasicOperation),
	Swap(0x2B, Kind.BasicOperation),
	PushString(0x2C, Kind.StringOperation),
	PushInt(0x2D, Kind.IntOperation),
	PushUInt(0x2E, Kind.UIntOperation),
	PushDouble(0x2F, Kind.NumberOperation),
	PushScope(0x30, Kind.BasicOperation),
	PushNamespace(0x31, Kind.NamespaceOperation),
	HasNext2(0x32, Kind.IntIntOperation),
	PushDecimal(0x33, Kind.BasicOperation),
	PushDNaN(0x34, Kind.BasicOperation),
	NewFunction(0x40, Kind.NewFunctionOperation),
	Call(0x41, Kind.IntOperation),
	Construct(0x42, Kind.IntOperation),
	CallMethod(0x43, Kind.MethodOperation),
	CallStatic(0x44, Kind.MethodOperation),
	CallSuper(0x45, Kind.MultinameIntOperation),
	CallProperty(0x46, Kind.MultinameIntOperation),
	ReturnVoid(0x47, Kind.BasicOperation),
	ReturnValue(0x48, Kind.BasicOperation),
	ConstructSuper(0x49, Kind.IntOperation),
	ConstructProp(0x4A, Kind.MultinameIntOperation),
	CallSuperId(0x4B, Kind.BasicOperation),
	CallPropLex(0x4C, Kind.MultinameIntOperation),
	CallInterface(0x4D, Kind.BasicOperation),
	CallSuperVoid(0x4E, Kind.MultinameIntOperation),
	CallPropVoid(0x4F, Kind.MultinameIntOperation),
	ApplyType(0x53, Kind.IntOperation),
	NewObject(0x55, Kind.IntOperation),
	NewArray(0x56, Kind.IntOperation),
	NewActivation(0x57, Kind.BasicOperation),
	NewClass(0x58, Kind.NewClassOperation),
	GetDescendants(0x59, Kind.MultinameOperation),
	NewCatch(0x5A, Kind.NewCatchOperation),
	FindPropGlobalStrict(0x5B, Kind.MultinameOperation),
	FindPropGlobal(0x5C, Kind.MultinameOperation),
	FindPropStrict(0x5D, Kind.MultinameOperation),
	FindProperty(0x5E, Kind.MultinameOperation),
	FindDef(0x5F, Kind.MultinameOperation),
	GetLex(0x60, Kind.MultinameOperation),
	SetProperty(0x61, Kind.MultinameOperation),
	GetLocal(0x62, Kind.IntOperation),
	SetLocal(0x63, Kind.IntOperation),
	GetGlobalScope(0x64, Kind.BasicOperation),
	GetScopeObject(0x65, Kind.IntOperation),
	GetProperty(0x66, Kind.MultinameOperation),
	GetPropertyLate(0x67, Kind.BasicOperation),
	InitProperty(0x68, Kind.MultinameOperation),
	SetPropertyLate(0x69, Kind.BasicOperation),
	DeleteProperty(0x6A, Kind.MultinameOperation),
	DeletePropertyLate(0x6B, Kind.BasicOperation),
	GetSlot(0x6C, Kind.IntOperation),
	SetSlot(0x6D, Kind.IntOperation),
	GetGlobalSlot(0x6E, Kind.IntOperation),
	SetGlobalSlot(0x6F, Kind.IntOperation),
	ConvertString(0x70, Kind.BasicOperation),
	EscXmlElem(0x71, Kind.BasicOperation),
	EscXmlAttr(0x72, Kind.BasicOperation),
	ConvertInt(0x73, Kind.BasicOperation),
	ConvertUInt(0x74, Kind.BasicOperation),
	ConvertDouble(0x75, Kind.BasicOperation),
	ConvertBoolean(0x76, Kind.BasicOperation),
	ConvertObject(0x77, Kind.BasicOperation),
	CheckFilter(0x78, Kind.BasicOperation),
	Coerce(0x80, Kind.MultinameOperation),
	CoerceBoolean(0x81, Kind.BasicOperation),
	CoerceAny(0x82, Kind.BasicOperation),
	CoerceInt(0x83, Kind.BasicOperation),
	CoerceDouble(0x84, Kind.BasicOperation),
	CoerceString(0x85, Kind.BasicOperation),
	AsType(0x86, Kind.MultinameOperation),
	AsTypeLate(0x87, Kind.BasicOperation),
	CoerceUInt(0x88, Kind.BasicOperation),
	CoerceObject(0x89, Kind.BasicOperation),
	Negate(0x90, Kind.BasicOperation),
	Increment(0x91, Kind.BasicOperation),
	IncLocal(0x92, Kind.IntOperation),
	Decrement(0x93, Kind.BasicOperation),
	DecLocal(0x94, Kind.IntOperation),
	TypeOf(0x95, Kind.BasicOperation),
	Not(0x96, Kind.BasicOperation),
	BitNot(0x97, Kind.BasicOperation),
	Concat(0x9A, Kind.BasicOperation),
	AddDouble(0x9B, Kind.BasicOperation),
	Add(0xA0, Kind.BasicOperation),
	Subtract(0xA1, Kind.BasicOperation),
	Multiply(0xA2, Kind.BasicOperation),
	Divide(0xA3, Kind.BasicOperation),
	Modulo(0xA4, Kind.BasicOperation),
	ShiftLeft(0xA5, Kind.BasicOperation),
	ShiftRight(0xA6, Kind.BasicOperation),
	ShiftRightUnsigned(0xA7, Kind.BasicOperation),
	BitAnd(0xA8, Kind.BasicOperation),
	BitOr(0xA9, Kind.BasicOperation),
	BitXor(0xAA, Kind.BasicOperation),
	Equals(0xAB, Kind.BasicOperation),
	StrictEquals(0xAC, Kind.BasicOperation),
	LessThan(0xAD, Kind.BasicOperation),
	LessEquals(0xAE, Kind.BasicOperation),
	GreaterThan(0xAF, Kind.BasicOperation),
	GreaterEquals(0xB0, Kind.BasicOperation),
	InstanceOf(0xB1, Kind.BasicOperation),
	IsType(0xB2, Kind.MultinameOperation),
	IsTypeLate(0xB3, Kind.BasicOperation),
	In(0xB4, Kind.BasicOperation),
	IncrementInt(0xC0, Kind.BasicOperation),
	DecrementInt(0xC1, Kind.BasicOperation),
	IncLocalInt(0xC2, Kind.IntOperation),
	DecLocalInt(0xC3, Kind.IntOperation),
	NegateInt(0xC4, Kind.BasicOperation),
	AddInt(0xC5, Kind.BasicOperation),
	SubtractInt(0xC6, Kind.BasicOperation),
	MultiplyInt(0xC7, Kind.BasicOperation),
	GetLocal0(0xD0, Kind.BasicOperation),
	GetLocal1(0xD1, Kind.BasicOperation),
	GetLocal2(0xD2, Kind.BasicOperation),
	GetLocal3(0xD3, Kind.BasicOperation),
	SetLocal0(0xD4, Kind.BasicOperation),
	SetLocal1(0xD5, Kind.BasicOperation),
	SetLocal2(0xD6, Kind.BasicOperation),
	SetLocal3(0xD7, Kind.BasicOperation),
	Debug(0xEF, Kind.IntStringIntIntOperation),
	DebugLine(0xF0, Kind.IntOperation),
	DebugFile(0xF1, Kind.StringOperation),
	BreakpointLine(0xF2, Kind.BasicOperation),
	SetByte(0x3A, Kind.BasicOperation),
	SetShort(0x3B, Kind.BasicOperation),
	SetInt(0x3C, Kind.BasicOperation),
	SetFloat(0x3D, Kind.BasicOperation),
	SetDouble(0x3E, Kind.BasicOperation),
	GetByte(0x35, Kind.BasicOperation),
	GetShort(0x36, Kind.BasicOperation),
	GetInt(0x37, Kind.BasicOperation),
	GetFloat(0x38, Kind.BasicOperation),
	GetDouble(0x39, Kind.BasicOperation),
	Sign1(0x50, Kind.BasicOperation),
	Sign8(0x51, Kind.BasicOperation),
	Sign16(0x52, Kind.BasicOperation);

	class KindContext(
		val op: AbcOpcode,
		val cpool: AbcConstantPool,
		val s: SyncStream
	) {
		val ints get() = cpool.ints
		val uints get() = cpool.uints
		val doubles get() = cpool.doubles
		val strings get() = cpool.strings
		val namespaces get() = cpool.namespaces
		val namespaceSets get() = cpool.namespaceSets
		val multinames get() = cpool.multinames
	}

	enum class Kind(val read: KindContext.() -> AbcOperation = { AbcBasicOperation(op) }) {
		BasicOperation({ AbcBasicOperation(op) }),
		LabelOperation({ AbcLabelOperation(op, s.position) }),
		IntOperation({
			AbcIntOperation(op, when (op) {
				AbcOpcode.GetScopeObject -> s.readU8()
				AbcOpcode.PushByte -> s.readS8()
				AbcOpcode.PushShort -> (s.readU30() shl 2) shr 2
				else -> s.readU30()
			})
		}),
		UIntOperation({ AbcIntOperation(op, uints[s.readU30()]) }),
		NumberOperation({ AbcDoubleOperation(op, doubles[s.readU30()]) }),
		IntIntOperation({
			if (op == AbcOpcode.HasNext2) {
				AbcIntIntOperation(op, s.readS32_le(), s.readS32_le())
			} else {
				AbcIntIntOperation(op, s.readU30(), s.readU30())
			}
		}),
		IntStringIntIntOperation({ AbcIntStringIntIntOperation(op, s.readU8(), strings[s.readU30()], s.readU8(), s.readU30()) }),
		StringOperation({ AbcStringOperation(op, strings[s.readU30()]) }),
		MultinameOperation({ AbcMultinameOperation(op, multinames[s.readU30()]) }),
		MultinameIntOperation({ AbcMultinameIntOperation(op, multinames[s.readU30()], s.readU30()) }),
		ConditionalJumpOperation({ AbcJumpOperation(op, s.position + 0x04 + s.readS24_le()) }),
		JumpOperation({ AbcJumpOperation(op, s.position + 0x04 + s.readS24_le()) }),
		NewClassOperation({ AbcNewClassOperation(op, s.readU30()) }),
		LookupSwitchOperation({
			val defaultMarker = s.position + s.readS24_le()
			val markers = (0 until (s.readU30() + 1)).map { s.position + s.readS24_le() }.toLongArray()
			AbcLookupSwitchOperation(op, defaultMarker, markers)
		}),
		NamespaceOperation({ AbcNamespaceOperation(op, namespaces[s.readU30()]) }),
		NewFunctionOperation({ AbcIntOperation(op, s.readU30()) }),
		MethodOperation({ AbcIntIntOperation(op, s.readU30(), s.readU30()) }),
		NewCatchOperation({ AbcIntOperation(op, s.readU30()) }),
	}

	companion object {
		val BY_ID = values().map { it.id to it }.toMap()
	}
}

interface AbcOperation {
	val opcode: AbcOpcode

	companion object {
		fun read(cpool: AbcConstantPool, s: SyncStream): AbcOperation {
			val iop = s.readU8()
			val op = AbcOpcode.BY_ID[iop]
			return op!!.type.read(AbcOpcode.KindContext(op, cpool, s))
		}
	}
}

data class AbcBasicOperation(override val opcode: AbcOpcode) : AbcOperation
data class AbcLabelOperation(override val opcode: AbcOpcode, val position: Long) : AbcOperation
data class AbcLookupSwitchOperation(override val opcode: AbcOpcode, val defaultMarker: Long, val markers: LongArray) : AbcOperation
data class AbcIntStringIntIntOperation(override val opcode: AbcOpcode, val int1: Int, val string: String, val int2: Int, val int3: Int) : AbcOperation
data class AbcIntOperation(override val opcode: AbcOpcode, val value: Int) : AbcOperation
data class AbcJumpOperation(override val opcode: AbcOpcode, val position: Long) : AbcOperation
data class AbcStringOperation(override val opcode: AbcOpcode, val value: String) : AbcOperation
data class AbcIntIntOperation(override val opcode: AbcOpcode, val value1: Int, val value2: Int) : AbcOperation
data class AbcDoubleOperation(override val opcode: AbcOpcode, val value: Double) : AbcOperation
data class AbcNewClassOperation(override val opcode: AbcOpcode, val value: Int) : AbcOperation
data class AbcMultinameOperation(override val opcode: AbcOpcode, val multiname: ABC.AbstractMultiname) : AbcOperation
data class AbcMultinameIntOperation(override val opcode: AbcOpcode, val multiname: ABC.AbstractMultiname, val value: Int) : AbcOperation
data class AbcNamespaceOperation(override val opcode: AbcOpcode, val namespace: ABC.Namespace) : AbcOperation
