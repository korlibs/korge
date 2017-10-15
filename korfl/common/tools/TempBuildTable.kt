package com.soywiz.korfl.abc

fun main(args: Array<String>) {
	val rexenums = Regex("public static const (\\w+):uint = (0x\\w+);")
	val rexkind = Regex("case Opcodes.(\\w+):\\s+return new (\\w+)")

	data class Opcode(val name: String, var id: String, var kind: String = "")

	val opcodes = LinkedHashMap<String, Opcode>()
	for (it in rexenums.findAll(enum)) {
		val name = it.groupValues[1]
		val id = it.groupValues[2]
		opcodes[name] = Opcode(name, id)
	}
	for (it in rexkind.findAll(factory)) {
		opcodes[it.groupValues[1]]?.kind = it.groupValues[2]
	}

	for (opcode in opcodes.values) {
		println("${opcode.name}(${opcode.id}, Kind.${opcode.kind}),")
	}
	//println(opcodes)
}

private val enum = """
public class Opcodes
	{
		public static const Breakpoint:uint = 0x01;
		public static const Nop:uint = 0x02;
		public static const Throw:uint = 0x03;
		public static const GetSuper:uint = 0x04;
		public static const SetSuper:uint = 0x05;
		public static const DefaultXmlNamespace:uint = 0x06;
		public static const DefaultXmlNamespaceL:uint = 0x07;
		public static const Kill:uint = 0x08;
		public static const Label:uint = 0x09;
		public static const IfNotLessThan:uint = 0x0C;
		public static const IfNotLessEqual:uint = 0x0D;
		public static const IfNotGreaterThan:uint = 0x0E;
		public static const IfNotGreaterEqual:uint = 0x0F;
		public static const Jump:uint = 0x10;
		public static const IfTrue:uint = 0x11;
		public static const IfFalse:uint = 0x12;
		public static const IfEqual:uint = 0x13;
		public static const IfNotEqual:uint = 0x14;
		public static const IfLessThan:uint = 0x15;
		public static const IfLessEqual:uint = 0x16;
		public static const IfGreaterThan:uint = 0x17;
		public static const IfGreaterEqual:uint = 0x18;
		public static const IfStrictEqual:uint = 0x19;
		public static const IfStrictNotEqual:uint = 0x1A;
		public static const LookupSwitch:uint = 0x1B;
		public static const PushWith:uint = 0x1C;
		public static const PopScope:uint = 0x1D;
		public static const NextName:uint = 0x1E;
		public static const HasNext:uint = 0x1F;
		public static const PushNull:uint = 0x20;
		public static const PushUndefined:uint = 0x21;
		public static const NextValue:uint = 0x23;
		public static const PushByte:uint = 0x24;
		public static const PushShort:uint = 0x25;
		public static const PushTrue:uint = 0x26;
		public static const PushFalse:uint = 0x27;
		public static const PushNaN:uint = 0x28;
		public static const Pop:uint = 0x29;
		public static const Dup:uint = 0x2A;
		public static const Swap:uint = 0x2B;
		public static const PushString:uint = 0x2C;
		public static const PushInt:uint = 0x2D;
		public static const PushUInt:uint = 0x2E;
		public static const PushDouble:uint = 0x2F;
		public static const PushScope:uint = 0x30;
		public static const PushNamespace:uint = 0x31;
		public static const HasNext2:uint = 0x32;

		public static const PushDecimal:uint = 0x33;	// NEW: PushDecimal according to FlexSDK, lix8 according to Tamarin
		public static const PushDNaN:uint = 0x34;		// NEW: PushDNaN according to Flex SDK, lix16 according to Tamarin

		public static const NewFunction:uint = 0x40;
		public static const Call:uint = 0x41;
		public static const Construct:uint = 0x42;
		public static const CallMethod:uint = 0x43;
		public static const CallStatic:uint = 0x44;
		public static const CallSuper:uint = 0x45;
		public static const CallProperty:uint = 0x46;
		public static const ReturnVoid:uint = 0x47;
		public static const ReturnValue:uint = 0x48;
		public static const ConstructSuper:uint = 0x49;
		public static const ConstructProp:uint = 0x4A;
		public static const CallSuperId:uint = 0x4B;	// NOT HANDLED
		public static const CallPropLex:uint = 0x4C;
		public static const CallInterface:uint = 0x4D;	// NOT HANDLED
		public static const CallSuperVoid:uint = 0x4E;
		public static const CallPropVoid:uint = 0x4F;
		public static const ApplyType:uint = 0x53;
		public static const NewObject:uint = 0x55;
		public static const NewArray:uint = 0x56;
		public static const NewActivation:uint = 0x57;
		public static const NewClass:uint = 0x58;
		public static const GetDescendants:uint = 0x59;
		public static const NewCatch:uint = 0x5A;

//		public static const FindPropGlobalStrict:uint = 0x5B;	// NEW from Tamarin (internal)
//		public static const FindPropGlobal:uint = 0x5C;			// NEW from Tamarin (internal)

		public static const FindPropStrict:uint = 0x5D;
		public static const FindProperty:uint = 0x5E;
		public static const FindDef:uint = 0x5F;		// NOT HANDLED
		public static const GetLex:uint = 0x60;
		public static const SetProperty:uint = 0x61;
		public static const GetLocal:uint = 0x62;
		public static const SetLocal:uint = 0x63;
		public static const GetGlobalScope:uint = 0x64;
		public static const GetScopeObject:uint = 0x65;
		public static const GetProperty:uint = 0x66;
		public static const GetPropertyLate:uint = 0x67;
		public static const InitProperty:uint = 0x68;
		public static const SetPropertyLate:uint = 0x69;
		public static const DeleteProperty:uint = 0x6A;
		public static const DeletePropertyLate:uint = 0x6B;
		public static const GetSlot:uint = 0x6C;
		public static const SetSlot:uint = 0x6D;
		public static const GetGlobalSlot:uint = 0x6E;
		public static const SetGlobalSlot:uint = 0x6F;
		public static const ConvertString:uint = 0x70;
		public static const EscXmlElem:uint = 0x71;
		public static const EscXmlAttr:uint = 0x72;
		public static const ConvertInt:uint = 0x73;
		public static const ConvertUInt:uint = 0x74;
		public static const ConvertDouble:uint = 0x75;
		public static const ConvertBoolean:uint = 0x76;
		public static const ConvertObject:uint = 0x77;
		public static const CheckFilter:uint = 0x78;
															// 0x79 convert_m
															// 0x7A convert_m_p
		public static const Coerce:uint = 0x80;
		public static const CoerceBoolean:uint = 0x81;
		public static const CoerceAny:uint = 0x82;
		public static const CoerceInt:uint = 0x83;
		public static const CoerceDouble:uint = 0x84;
		public static const CoerceString:uint = 0x85;
		public static const AsType:uint = 0x86;
		public static const AsTypeLate:uint = 0x87;
		public static const CoerceUInt:uint = 0x88;
		public static const CoerceObject:uint = 0x89;
															// 0x8F negate_p
		public static const Negate:uint = 0x90;
		public static const Increment:uint = 0x91;
		public static const IncLocal:uint = 0x92;
		public static const Decrement:uint = 0x93;
		public static const DecLocal:uint = 0x94;
		public static const TypeOf:uint = 0x95;
		public static const Not:uint = 0x96;
		public static const BitNot:uint = 0x97;
		public static const Concat:uint = 0x9A;
		public static const AddDouble:uint = 0x9B;
															// 0x9c increment_p
															// 0x9d inclocal_p
															// 0x9e decrement_p
															// 0x9f declocal_p
		public static const Add:uint = 0xA0;
		public static const Subtract:uint = 0xA1;
		public static const Multiply:uint = 0xA2;
		public static const Divide:uint = 0xA3;
		public static const Modulo:uint = 0xA4;
		public static const ShiftLeft:uint = 0xA5;
		public static const ShiftRight:uint = 0xA6;
		public static const ShiftRightUnsigned:uint = 0xA7;
		public static const BitAnd:uint = 0xA8;
		public static const BitOr:uint = 0xA9;
		public static const BitXor:uint = 0xAA;
		public static const Equals:uint = 0xAB;
		public static const StrictEquals:uint = 0xAC;
		public static const LessThan:uint = 0xAD;
		public static const LessEquals:uint = 0xAE;
		public static const GreaterThan:uint = 0xAF;
		public static const GreaterEquals:uint = 0xB0;
		public static const InstanceOf:uint = 0xB1;
		public static const IsType:uint = 0xB2;
		public static const IsTypeLate:uint = 0xB3;
		public static const In:uint = 0xB4;

		public static const IncrementInt:uint = 0xC0;
		public static const DecrementInt:uint = 0xC1;
		public static const IncLocalInt:uint = 0xC2;
		public static const DecLocalInt:uint = 0xC3;
		public static const NegateInt:uint = 0xC4;
		public static const AddInt:uint = 0xC5;
		public static const SubtractInt:uint = 0xC6;
		public static const MultiplyInt:uint = 0xC7;
		public static const GetLocal0:uint = 0xD0;
		public static const GetLocal1:uint = 0xD1;
		public static const GetLocal2:uint = 0xD2;
		public static const GetLocal3:uint = 0xD3;
		public static const SetLocal0:uint = 0xD4;
		public static const SetLocal1:uint = 0xD5;
		public static const SetLocal2:uint = 0xD6;
		public static const SetLocal3:uint = 0xD7;
														// 0xee abs_jump
		public static const Debug:uint = 0xEF;
		public static const DebugLine:uint = 0xF0;
		public static const DebugFile:uint = 0xF1;
		public static const BreakpointLine:uint = 0xF2;
														// 0xf3 timestamp
														// 0xf5 verifypass
														// 0xf6 alloc
														// 0xf7 mark
														// 0xf8 wb
														// 0xf9 prologue
														// 0xfa sendenter
														// 0xfb doubletoatom
														// 0xfc sweep
														// 0xfd codegenop
														// 0xfe verifyop
														// 0xff decode

		// Alchemy Opcodes
		public static const SetByte:uint = 0x3A;
		public static const SetShort:uint = 0x3B;
		public static const SetInt:uint = 0x3C;
		public static const SetFloat:uint = 0x3D;
		public static const SetDouble:uint = 0x3E;
		public static const GetByte:uint = 0x35;
		public static const GetShort:uint = 0x36;
		public static const GetInt:uint = 0x37;
		public static const GetFloat:uint = 0x38;
		public static const GetDouble:uint = 0x39;
		public static const Sign1:uint = 0x50;
		public static const Sign8:uint = 0x51;
		public static const Sign16:uint = 0x52;



		/*
		// Enumerated opcode type bit flags
		public static const A:uint = 0x01, ALCHEMY:uint = 0x01;
		public static const X:uint = 0x02, EXPERIMENTAL:uint = 0x02;
		public static const T:uint = 0x04, THROWS:uint = 0x04;
		public static const opcodeInfo:Vector.<OpcodeInfo> = Vector.<OpcodeInfo> (

			new OpcodeInfo("Unknown_0x00",         "OP_0x00",        BasicOperation, false, true ),
			new OpcodeInfo("Breakpoint",           "bkpt",           BasicOperation, false, false),
			new OpcodeInfo("Nop",                  "nop",            BasicOperation, false, false),
			new OpcodeInfo("Throw",                "throw",          BasicOperation, false, false),
			new OpcodeInfo("GetSuper",             "getsuper",       BasicOperation, false, false),
			new OpcodeInfo("SetSuper",             "setsuper",       BasicOperation, false, false),
			new OpcodeInfo("DefaultXMLNamespace",  "dxns",           BasicOperation, false, false),
			new OpcodeInfo("DefaultXMLNamespaceL", "dxnslate",       BasicOperation, false, false),
			new OpcodeInfo("Kill",                 "kill",           BasicOperation, false, false),
			new OpcodeInfo("Label",                "label",          BasicOperation, false, false),
			new OpcodeInfo("Unknown_0x0A",         "OP_0x0A",        BasicOperation, false, true ),
			new OpcodeInfo("Unknown_0x0B",         "OP_0x0B",        BasicOperation, false, true ),
			new OpcodeInfo("IfNotLessThan",        "ifnlt",          BasicOperation, false, false),
			new OpcodeInfo("IfNotLessEqual",       "ifnle",          BasicOperation, false, false),
			new OpcodeInfo("IfNotGreaterThan",     "ifngt",          BasicOperation, false, false),
			new OpcodeInfo("IfNotGreaterEqual",    "ifnge",          BasicOperation, false, false),

			new OpcodeInfo("Jump",                 "jump",           BasicOperation, false, false),
			new OpcodeInfo("IfTrue",               "iftrue",         BasicOperation, false, false),
			new OpcodeInfo("IfFalse",              "iffalse",        BasicOperation, false, false),
			new OpcodeInfo("IfEqual",              "ifeq",           BasicOperation, false, false),
			new OpcodeInfo("IfNotEqual",           "ifne",           BasicOperation, false, false),
			new OpcodeInfo("IfLessThan",           "iflt",           BasicOperation, false, false),
			new OpcodeInfo("IfLessEqual",          "ifle",           BasicOperation, false, false),
			new OpcodeInfo("IfGreaterThan",        "ifgt",           BasicOperation, false, false),
			new OpcodeInfo("IfGreaterEqual",       "ifge",           BasicOperation, false, false),
			new OpcodeInfo("IfStrictEqual",        "ifstricteq",     BasicOperation, false, false),
			new OpcodeInfo("IfStrictNotEqual",     "ifstrictne",     BasicOperation, false, false),
			new OpcodeInfo("LookupSwitch",         "lookupswitch",   BasicOperation, false, false),
			new OpcodeInfo("PushWith",             "pushwith",       BasicOperation, false, false),
			new OpcodeInfo("PopScope",             "popscope",       BasicOperation, false, false),
			new OpcodeInfo("NextName",             "nextname",       BasicOperation, false, false),
			new OpcodeInfo("HasNext",              "hasnext",        BasicOperation, false, false),

			new OpcodeInfo("PushNull",             "pushnull",       BasicOperation, false, false),
			new OpcodeInfo("PushUndefined",        "pushundefined",  BasicOperation, false, false),
			new OpcodeInfo("Unknown_0x22",         "OP_0x22",        BasicOperation, false, true ),
			new OpcodeInfo("NextValue",            "nextvalue",      BasicOperation, false, false),
			new OpcodeInfo("PushByte",             "pushbyte",       BasicOperation, false, false),
			new OpcodeInfo("PushShort",            "pushshort",      BasicOperation, false, false),
			new OpcodeInfo("PushTrue",             "pushtrue",       BasicOperation, false, false),
			new OpcodeInfo("PushFalse",            "pushfalse",      BasicOperation, false, false),
			new OpcodeInfo("PushNaN",              "pushnan",        BasicOperation, false, false),
			new OpcodeInfo("Pop",                  "pop",            BasicOperation, false, false),
			new OpcodeInfo("Dup",                  "dup",            BasicOperation, false, false),
			new OpcodeInfo("Swap",                 "swap",           BasicOperation, false, false),
			new OpcodeInfo("PushString",           "pushstring",     BasicOperation, false, false),
			new OpcodeInfo("PushInt",              "pushint",        BasicOperation, false, false),
			new OpcodeInfo("PushUInt",             "pushuint",       BasicOperation, false, false),
			new OpcodeInfo("PushDouble",           "pushdouble",     BasicOperation, false, false),

			new OpcodeInfo("PushScope",            "pushscope",      BasicOperation, false, true ),
			new OpcodeInfo("PushNamespace",        "pushnamespace",  BasicOperation, false, false),
			new OpcodeInfo("HasNext2",             "hasnext2",       BasicOperation, false, false),
			new OpcodeInfo("Unknown_0x33",         "OP_0x33",        BasicOperation, false, true ), // UNDOCUMENTED: PushDecimal according to Flex SDK, lix8 internal-only according to Tamarin
			new OpcodeInfo("Unknown_0x34",         "OP_0x34",        BasicOperation, false, true ), // UNDOCUMENTED: PushDNaN according to Flex SDK, lix16 internal-only according to Tamarin
			new OpcodeInfo("GetByte",              "OP_li8",         BasicOperation, true,  false),
			new OpcodeInfo("GetShort",             "OP_li16",        BasicOperation, true,  false),
			new OpcodeInfo("GetInt",               "OP_li32",        BasicOperation, true,  false),
			new OpcodeInfo("GetFloat",             "OP_lf32",        BasicOperation, true,  false),
			new OpcodeInfo("GetDouble",            "OP_lf64",        BasicOperation, true,  false),
			new OpcodeInfo("SetByte",              "OP_si8",         BasicOperation, true,  false),
			new OpcodeInfo("SetShort",             "OP_si16",        BasicOperation, true,  false),
			new OpcodeInfo("SetInt",               "OP_si32",        BasicOperation, true,  false),
			new OpcodeInfo("SetFloat",             "OP_sf32",        BasicOperation, true,  false),
			new OpcodeInfo("SetDouble",            "OP_sf64",        BasicOperation, true,  false),
			new OpcodeInfo("Unknown_0x3F",         "OP_0x3F",        BasicOperation, false, false),

			new OpcodeInfo("NewFunction",          "newfunction",    BasicOperation, false, false),
			new OpcodeInfo("Call",                 "call",           BasicOperation, false, false),
			new OpcodeInfo("Construct",            "construct",      BasicOperation, false, false),
			new OpcodeInfo("CallMethod",           "callmethod",     BasicOperation, false, false),
			new OpcodeInfo("CallStatic",           "callstatic",     BasicOperation, false, false),
			new OpcodeInfo("CallSuper",            "callsuper",      BasicOperation, false, false),
			new OpcodeInfo("CallProperty",         "callproperty",   BasicOperation, false, false),
			new OpcodeInfo("ReturnVoid",           "returnvoid",     BasicOperation, false, false),
			new OpcodeInfo("ReturnValue",          "returnvalue",    BasicOperation, false, false),
			new OpcodeInfo("ConstructSuper",       "constructsuper", BasicOperation, false, false),
			new OpcodeInfo("ConstuctProp",         "constructprop",  BasicOperation, false, false),
			new OpcodeInfo("CallSuperId",          "callsuperid",    BasicOperation, false, false),
			new OpcodeInfo("CallPropLex",          "callproplex",    BasicOperation, false, false),
			new OpcodeInfo("CallInterface",        "callinterface",  BasicOperation, false, false),
			new OpcodeInfo("CallSuperVoid",        "callsupervoid",  BasicOperation, false, false),
			new OpcodeInfo("CallPropVoid",         "callpropvoid",   BasicOperation, false, false),

			new OpcodeInfo("Sign1",                "OP_sxi1",        BasicOperation, true,  false),
			new OpcodeInfo("Sign8",                "OP_sxi8",        BasicOperation, true,  false),
			new OpcodeInfo("Sign16",               "OP_sxi16",       BasicOperation, true,  false),
			new OpcodeInfo("ApplyType",            "applytype",      BasicOperation, false, false),
			new OpcodeInfo("Unknown_0x54",         "OP_0x54",        BasicOperation, false, true ),
			new OpcodeInfo("NewObject",            "newobject",      BasicOperation, false, false),
			new OpcodeInfo("NewArray",             "newarray",       BasicOperation, false, false),
			new OpcodeInfo("NewActivation",        "newactivation",  BasicOperation, false, false),
			new OpcodeInfo("NewClass",             "newclass",       BasicOperation, false, false),
			new OpcodeInfo("GetDescendants",       "getdescendants", BasicOperation, false, false),
			new OpcodeInfo("NewCatch",             "newcatch",       BasicOperation, false, false),
			new OpcodeInfo("Unknown_0x5B",         "OP_0x5B",        BasicOperation, false, true ), // UNDOCUMENTED: FindPropGlobalStrict internal-only according to Tamarin
			new OpcodeInfo("Unknown_0x5C",         "OP_0x5C",        BasicOperation, false, true ), // UNDOCUMENTED: FindPropGlobal internal-only according to Tamarin
			new OpcodeInfo("FindPropStrict",       "findpropstrict", BasicOperation, false, false),
			new OpcodeInfo("FindProperty",         "findproperty",   BasicOperation, false, false),
			new OpcodeInfo("FindDef",              "finddef",        BasicOperation, false, false),

			new OpcodeInfo("GetLex",               "getlex",         BasicOperation, false, false),
			new OpcodeInfo("SetProperty",          "setproperty",    BasicOperation, false, false),
			new OpcodeInfo("GetLocal",             "getlocal",       BasicOperation, false, false),
			new OpcodeInfo("SetLocal",             "setlocal",       BasicOperation, false, false),
			new OpcodeInfo("GetGlobalScope",       "getglobalscope", BasicOperation, false, false),
			new OpcodeInfo("GetScopeObject",       "getscopeobject", BasicOperation, false, false),
			new OpcodeInfo("GetProperty",          "getproperty",    BasicOperation, false, false),
			new OpcodeInfo("GetPropertyLate",      "OP_0x67",        BasicOperation, false, false),
			new OpcodeInfo("InitProperty",         "initproperty",   BasicOperation, false, false),
			new OpcodeInfo("SetPropertyLate",      "OP_0x69",        BasicOperation, false, false),
			new OpcodeInfo("DeleteProperty",       "deleteproperty", BasicOperation, false, false),
			new OpcodeInfo("DeletePropertyLate",   "OP_0x6B" ,       BasicOperation, false, false),
			new OpcodeInfo("GetSlot",              "getslot",        BasicOperation, false, false),
			new OpcodeInfo("SetSlot",              "setslot",        BasicOperation, false, false),
			new OpcodeInfo("GetGlobalSlot",        "getglobalslot",  BasicOperation, false, false),
			new OpcodeInfo("SetGlobalSlot",        "setglobalslot",  BasicOperation, false, false),

			new OpcodeInfo("ConvertString",        "convert_s",      BasicOperation, false, false),
			new OpcodeInfo("EscXmlElem",           "esc_xelem",      BasicOperation, false, false),
			new OpcodeInfo("EscXmlAttr",           "esc_xattr",      BasicOperation, false, false),
			new OpcodeInfo("ConvertInt",           "convert_i",      BasicOperation, false, false),
			new OpcodeInfo("ConvertUInt",          "convert_u",      BasicOperation, false, false),
			new OpcodeInfo("ConvertDouble",        "convert_d",      BasicOperation, false, false),
			new OpcodeInfo("ConvertBoolean",       "convert_b",      BasicOperation, false, false),
			new OpcodeInfo("ConvertObject",        "convert_o",      BasicOperation, false, false),
			new OpcodeInfo("CheckFilter",          "checkfilter",    BasicOperation, false, false),
			new OpcodeInfo("Unknown_0x79",         "OP_0x79",        BasicOperation, false, true ), // UNDOCUMENTED: convert_m according to Flex SDK
			new OpcodeInfo("Unknown_0x7A",         "OP_0x7A",        BasicOperation, false, true ), // UNDOCUMENTED: convert_m_p according to Flex SDK
			new OpcodeInfo("Unknown_0x7B",         "OP_0x7B",        BasicOperation, false, true),
			new OpcodeInfo("Unknown_0x7C",         "OP_0x7C",        BasicOperation, false, true),
			new OpcodeInfo("Unknown_0x7D",         "OP_0x7D",        BasicOperation, false, true),
			new OpcodeInfo("Unknown_0x7E",         "OP_0x7E",        BasicOperation, false, true),
			new OpcodeInfo("Unknown_0x7F",         "OP_0x7F",        BasicOperation, false, true),

			new OpcodeInfo("Coerce",               "coerce",         BasicOperation, false, false),
			new OpcodeInfo("CoerceBoolean",        "coerce_b",       BasicOperation, false, false),
			new OpcodeInfo("CoerceAny",            "coerce_a",       BasicOperation, false, false),
			new OpcodeInfo("CoerceInt",            "coerce_i",       BasicOperation, false, false),
			new OpcodeInfo("CoerceDouble",         "coerce_d",       BasicOperation, false, false),
			new OpcodeInfo("CoerceString",         "coerce_s",       BasicOperation, false, false),
			new OpcodeInfo("AsType",               "astype",         BasicOperation, false, false),
			new OpcodeInfo("AsTypeLate",           "astypelate",     BasicOperation, false, false),
			new OpcodeInfo("CoerceUInt",           "coerce_u",       BasicOperation, false, false),
			new OpcodeInfo("CoerceObject",         "coerce_o",       BasicOperation, false, false),
			new OpcodeInfo("Unknown_0x8A",         "OP_0x8A",        BasicOperation, false, true ),
			new OpcodeInfo("Unknown_0x8B",         "OP_0x8B" ,       BasicOperation, false, true ),
			new OpcodeInfo("Unknown_0x8C",         "OP_0x8C",        BasicOperation, false, true ),
			new OpcodeInfo("Unknown_0x8D",         "OP_0x8D",        BasicOperation, false, true ),
			new OpcodeInfo("Unknown_0x8E",         "OP_0x8E",        BasicOperation, false, true ),
			new OpcodeInfo("Unknown_0x8F",         "OP_0x8F",       BasicOperation,  false, true ), // UNDOCUMENTED: negate_p according to Flex SDK

			new OpcodeInfo("Negate",               "negate",         BasicOperation, false, false),
			new OpcodeInfo("Increment",            "increment",      BasicOperation, false, false),
			new OpcodeInfo("IncLocal",             "inclocal",       BasicOperation, false, false),
			new OpcodeInfo("Decrement",            "decrement",      BasicOperation, false, false),
			new OpcodeInfo("DecLocal",             "declocal",       BasicOperation, false, false),
			new OpcodeInfo("TypeOf",               "typeof",         BasicOperation, false, false),
			new OpcodeInfo("Not",                  "not",            BasicOperation, false, false),
			new OpcodeInfo("BitNot",               "bitnot",         BasicOperation, false, false),
			new OpcodeInfo("Unknown_0x98",         "OP_0x98",        BasicOperation, false, true ),
			new OpcodeInfo("Unknown_0x99",         "OP_0x99",        BasicOperation, false, true ),
			new OpcodeInfo("Concat",               "concat",         BasicOperation, false, false),
			new OpcodeInfo("AddDouble",            "add_d" ,         BasicOperation, false, false),
			new OpcodeInfo("Unknown_0x9C",         "OP_0x9C",        BasicOperation, false, true ), // UNDOCUMENTED: increment_p according to Flex SDK
			new OpcodeInfo("Unknown_0x9D",         "OP_0x9D",        BasicOperation, false, true ), // UNDOCUMENTED: inclocal_p according to Flex SDK
			new OpcodeInfo("Unknown_0x9E",         "OP_0x9E",        BasicOperation, false, true ), // UNDOCUMENTED: decrement_p according to Flex SDK
			new OpcodeInfo("Unknown_0x9F",         "OP_0x9F",        BasicOperation, false, true ),

			new OpcodeInfo("Add",                  "add",            BasicOperation, false, false),
			new OpcodeInfo("Subtract",             "subtract",       BasicOperation, false, false),
			new OpcodeInfo("Multiply",             "multiply",       BasicOperation, false, false),
			new OpcodeInfo("Divide",               "divide",         BasicOperation, false, false),
			new OpcodeInfo("Modulo",               "modulo",         BasicOperation, false, false),
			new OpcodeInfo("ShiftLeft",            "lshift",         BasicOperation, false, false),
			new OpcodeInfo("ShiftRight",           "rshift",         BasicOperation, false, false),
			new OpcodeInfo("ShiftRightUnsigned",   "urshift",        BasicOperation, false, false),
			new OpcodeInfo("BitAnd",               "bitand",         BasicOperation, false, false),
			new OpcodeInfo("BitOr",                "bitor",          BasicOperation, false, false),
			new OpcodeInfo("BitXor",               "bitxor",         BasicOperation, false, false),
			new OpcodeInfo("Equals",               "equals" ,        BasicOperation, false, false),
			new OpcodeInfo("StrictEquals",         "strictequals",   BasicOperation, false, false),
			new OpcodeInfo("LessThan",             "lessthan",       BasicOperation, false, false),
			new OpcodeInfo("LessEquals",           "lessequals",     BasicOperation, false, false),
			new OpcodeInfo("GreaterThan",          "greaterthan",    BasicOperation, false, false),

			new OpcodeInfo("GreaterEquals",        "greaterequals",  BasicOperation, false, false),
			new OpcodeInfo("InstanceOf",           "instanceof",     BasicOperation, false, false),
			new OpcodeInfo("IsType",               "istype",         BasicOperation, false, false),
			new OpcodeInfo("IsTypeLate",           "istypelate",     BasicOperation, false, false),
			new OpcodeInfo("In",                   "in",             BasicOperation, false, false),
			new OpcodeInfo("Unknown_0xB5",         "OP_0xB5",        BasicOperation, false, true ), // UNDOCUMENTED: add_p according to Flex SDK
			new OpcodeInfo("Unknown_0xB6",         "OP_0xB6",        BasicOperation, false, true ), // UNDOCUMENTED: subtract_p according to Flex SDK
			new OpcodeInfo("Unknown_0xB7",         "OP_0xB7",        BasicOperation, false, true ), // UNDOCUMENTED: multiply_p according to Flex SDK
			new OpcodeInfo("Unknown_0xB8",         "OP_0xB8",        BasicOperation, false, true ), // UNDOCUMENTED: divide_p according to Flex SDK
			new OpcodeInfo("Unknown_0xB9",         "OP_0xB9",        BasicOperation, false, true ), // UNDOCUMENTED: modulo_p according to Flex SDK
			new OpcodeInfo("Unknown_0xBA",         "OP_0xBA",        BasicOperation, false, true ),
			new OpcodeInfo("Unknown_0xBB",         "OP_0xBB",        BasicOperation, false, true ),
			new OpcodeInfo("Unknown_0xBC",         "OP_0xBC",        BasicOperation, false, true ),
			new OpcodeInfo("Unknown_0xBD",         "OP_0xBD",        BasicOperation, false, true ),
			new OpcodeInfo("Unknown_0xBE",         "OP_0xBE",        BasicOperation, false, true ),
			new OpcodeInfo("Unknown_0xBF",         "OP_0xBF",        BasicOperation, false, true ),

			new OpcodeInfo("IncrementInt",         "increment_i",    BasicOperation, false, false),
			new OpcodeInfo("DecrementInt",         "decrement_i",    BasicOperation, false, false),
			new OpcodeInfo("IncLocalInt",          "inclocal_i",     BasicOperation, false, false),
			new OpcodeInfo("DecLocalInt",          "declocal_i",     BasicOperation, false, false),
			new OpcodeInfo("NegateInt",            "negate_i",       BasicOperation, false, false),
			new OpcodeInfo("AddInt",               "add_i",          BasicOperation, false, false),
			new OpcodeInfo("SubtractInt",          "subtract_i",     BasicOperation, false, false),
			new OpcodeInfo("MultiplyInt",          "multiply_i",     BasicOperation, false, false),
			new OpcodeInfo("Unknown_0xC8",         "OP_0xC8",        BasicOperation, false, true ),
			new OpcodeInfo("Unknown_0xC9",         "OP_0xC9",        BasicOperation, false, true ),
			new OpcodeInfo("Unknown_0xCA",         "OP_0xCA",        BasicOperation, false, true ),
			new OpcodeInfo("Unknown_0xCB",         "OP_0xCB" ,       BasicOperation, false, true ),
			new OpcodeInfo("Unknown_0xCC",         "OP_0xCC",        BasicOperation, false, true ),
			new OpcodeInfo("Unknown_0xCD",         "OP_0xCD",        BasicOperation, false, true ),
			new OpcodeInfo("Unknown_0xCE",         "OP_0xCE",        BasicOperation, false, true ),
			new OpcodeInfo("Unknown_0xCF",         "OP_0xCF",        BasicOperation, false, true ),

			new OpcodeInfo("GetLocal0",            "getlocal0",      BasicOperation, false, false),
			new OpcodeInfo("GetLocal1",            "getlocal1",      BasicOperation, false, false),
			new OpcodeInfo("GetLocal2",            "getlocal2",      BasicOperation, false, false),
			new OpcodeInfo("GetLocal3",            "getlocal3",      BasicOperation, false, false),
			new OpcodeInfo("SetLocal0",            "setlocal0",      BasicOperation, false, false),
			new OpcodeInfo("SetLocal1",            "setlocal1",      BasicOperation, false, false),
			new OpcodeInfo("SetLocal2",            "setlocal2",      BasicOperation, false, false),
			new OpcodeInfo("SetLocal3",            "setlocal3",      BasicOperation, false, false),
			new OpcodeInfo("Unknown_0xD8",         "OP_0xD8",        BasicOperation, false, true ),
			new OpcodeInfo("Unknown_0xD9",         "OP_0xD9",        BasicOperation, false, true ),
			new OpcodeInfo("Unknown_0xDA",         "OP_0xDA",        BasicOperation, false, true ),
			new OpcodeInfo("Unknown_0xDB",         "OP_0xDB" ,       BasicOperation, false, true ),
			new OpcodeInfo("Unknown_0xDC",         "OP_0xDC",        BasicOperation, false, true ),
			new OpcodeInfo("Unknown_0xDD",         "OP_0xDD",        BasicOperation, false, true ),
			new OpcodeInfo("Unknown_0xDE",         "OP_0xDE",        BasicOperation, false, true ),
			new OpcodeInfo("Unknown_0xDF",         "OP_0xDF",        BasicOperation, false, true ),

			new OpcodeInfo("Unknown_0xE0",         "OP_0xE0",        BasicOperation, false, true ),
			new OpcodeInfo("Unknown_0xE1",         "OP_0xE1",        BasicOperation, false, true ),
			new OpcodeInfo("Unknown_0xE2",         "OP_0xE2",        BasicOperation, false, true ),
			new OpcodeInfo("Unknown_0xE3",         "OP_0xE3",        BasicOperation, false, true ),
			new OpcodeInfo("Unknown_0xE4",         "OP_0xE4",        BasicOperation, false, true ),
			new OpcodeInfo("Unknown_0xE5",         "OP_0xE5",        BasicOperation, false, true ),
			new OpcodeInfo("Unknown_0xE6",         "OP_0xE6",        BasicOperation, false, true ),
			new OpcodeInfo("Unknown_0xE7",         "OP_0xE7",        BasicOperation, false, true ),
			new OpcodeInfo("Unknown_0xE8",         "OP_0xE8",        BasicOperation, false, true ),
			new OpcodeInfo("Unknown_0xE9",         "OP_0xE9",        BasicOperation, false, true ),
			new OpcodeInfo("Unknown_0xEA",         "OP_0xEA",        BasicOperation, false, true ),
			new OpcodeInfo("Unknown_0xEB",         "OP_0xEB" ,       BasicOperation, false, true ),
			new OpcodeInfo("Unknown_0xEC",         "OP_0xEC",        BasicOperation, false, true ),
			new OpcodeInfo("Unknown_0xED",         "OP_0xED",        BasicOperation, false, true ),
			new OpcodeInfo("Unknown_0xEE",         "OP_0xEE",        BasicOperation, false, true ), // UNDOCUMENTED: abs_jump according to Tamarin and Flex SDK
			new OpcodeInfo("Debug",                "debug",          BasicOperation, false, false),

			new OpcodeInfo("DebugLine",            "debugline",      BasicOperation, false, false),
			new OpcodeInfo("DebugFile",            "debugfile",      BasicOperation, false, false),
			new OpcodeInfo("BreakpointLine",       "bkptline",       BasicOperation, false, false),
			new OpcodeInfo("Unknown_0xF3",         "OP_0xF3",        BasicOperation, false, true ), // UNDOCUMENTED: timestamp according to Tamarin and Flex SDK
			new OpcodeInfo("Unknown_0xF4",         "OP_0xF4",        BasicOperation, false, true ),
			new OpcodeInfo("VerifyPass",           "verifypass",    BasicOperation, false, true ),
			new OpcodeInfo("Alloc",                "alloc",         BasicOperation, false, true ),
			new OpcodeInfo("Mark",                 "mark",          BasicOperation, false, true ),
			new OpcodeInfo("WB",                   "wb",            BasicOperation, false, true ),
			new OpcodeInfo("Prologue",             "prologue",      BasicOperation, false, true ),
			new OpcodeInfo("SendEnter",            "sendenter",     BasicOperation, false, true ),
			new OpcodeInfo("DoubleToAtom",         "doubletoatom",  BasicOperation, false, true ),
			new OpcodeInfo("Sweep",                "sweep",         BasicOperation, false, true ),
			new OpcodeInfo("CodeGenOp",            "codegenop",     BasicOperation, false, true ),
			new OpcodeInfo("VerifyOp",             "verifyop",      BasicOperation, false, true ),
			new OpcodeInfo("Decode",               "decode",        BasicOperation, false, true )
		);
		*/


		protected static var _opNames:Vector.<String>;





		public static function get opNames():Vector.<String> {
			if (!_opNames) {

				var i:int;
				_opNames = new Vector.<String>(MAX_VALUE, true);
				for (i = MIN_VALUE; i < MAX_VALUE; i++) {
					_opNames[i] = "Unknown_" + i.toString(16);
				}

				_opNames[ Breakpoint ] = "Breakpoint";
				_opNames[ Nop ] = "Nop";
				_opNames[ Throw ] = "Throw";
				_opNames[ GetSuper ] = "GetSuper";
				_opNames[ SetSuper ] = "SetSuper";
				_opNames[ DefaultXmlNamespace ] = "DefaultXmlNamespace";
				_opNames[ DefaultXmlNamespaceL ] = "DefaultXmlNamespaceL";
				_opNames[ Kill ] = "Kill";
				_opNames[ Label ] = "Label";
				_opNames[ IfNotLessThan ] = "IfNotLessThan";
				_opNames[ IfNotLessEqual ] = "IfNotLessEqual";
				_opNames[ IfNotGreaterThan ] = "IfNotGreaterThan";
				_opNames[ IfNotGreaterEqual ] = "IfNotGreaterEqual";
				_opNames[ Jump ] = "Jump";
				_opNames[ IfTrue ] = "IfTrue";
				_opNames[ IfFalse ] = "IfFalse";
				_opNames[ IfEqual ] = "IfEqual";
				_opNames[ IfNotEqual ] = "IfNotEqual";
				_opNames[ IfLessThan ] = "IfLessThan";
				_opNames[ IfLessEqual ] = "IfLessEqual";
				_opNames[ IfGreaterThan ] = "IfGreaterThan";
				_opNames[ IfGreaterEqual ] = "IfGreaterEqual";
				_opNames[ IfStrictEqual ] = "IfStrictEqual";
				_opNames[ IfStrictNotEqual ] = "IfStrictNotEqual";
				_opNames[ LookupSwitch ] = "LookupSwitch";
				_opNames[ PushWith ] = "PushWith";
				_opNames[ PopScope ] = "PopScope";
				_opNames[ NextName ] = "NextName";
				_opNames[ HasNext ] = "HasNext";
				_opNames[ PushNull ] = "PushNull";
				_opNames[ PushUndefined ] = "PushUndefined";
				_opNames[ NextValue ] = "NextValue";
				_opNames[ PushByte ] = "PushByte";
				_opNames[ PushShort ] = "PushShort";
				_opNames[ PushTrue ] = "PushTrue";
				_opNames[ PushFalse ] = "PushFalse";
				_opNames[ PushNaN ] = "PushNaN";
				_opNames[ Pop ] = "Pop";
				_opNames[ Dup ] = "Dup";
				_opNames[ Swap ] = "Swap";
				_opNames[ PushString ] = "PushString";
				_opNames[ PushInt ] = "PushInt";
				_opNames[ PushUInt ] = "PushUInt";
				_opNames[ PushDouble ] = "PushDouble";
				_opNames[ PushScope ] = "PushScope";
				_opNames[ PushNamespace ] = "PushNamespace";
				_opNames[ HasNext2 ] = "HasNext2";
				_opNames[ NewFunction ] = "NewFunction";
				_opNames[ Call ] = "Call";
				_opNames[ Construct ] = "Construct";
				_opNames[ CallMethod ] = "CallMethod";
				_opNames[ CallStatic ] = "CallStatic";
				_opNames[ CallSuper ] = "CallSuper";
				_opNames[ CallProperty ] = "CallProperty";
				_opNames[ ReturnVoid ] = "ReturnVoid";
				_opNames[ ReturnValue ] = "ReturnValue";
				_opNames[ ConstructSuper ] = "ConstructSuper";
				_opNames[ ConstructProp ] = "ConstructProp";
				_opNames[ CallSuperId ] = "CallSuperId";
				_opNames[ CallPropLex ] = "CallPropLex";
				_opNames[ CallInterface ] = "CallInterface";
				_opNames[ CallSuperVoid ] = "CallSuperVoid";
				_opNames[ CallPropVoid ] = "CallPropVoid";
				_opNames[ ApplyType ] = "ApplyType";
				_opNames[ NewObject ] = "NewObject";
				_opNames[ NewArray ] = "NewArray";
				_opNames[ NewActivation ] = "NewActivation";
				_opNames[ NewClass ] = "NewClass";
				_opNames[ GetDescendants ] = "GetDescendants";
				_opNames[ NewCatch ] = "NewCatch";
				_opNames[ FindPropStrict ] = "FindPropStrict";
				_opNames[ FindProperty ] = "FindProperty";
				_opNames[ FindDef ] = "FindDef";
				_opNames[ GetLex ] = "GetLex";
				_opNames[ SetProperty ] = "SetProperty";
				_opNames[ GetLocal ] = "GetLocal";
				_opNames[ SetLocal ] = "SetLocal";
				_opNames[ GetGlobalScope ] = "GetGlobalScope";
				_opNames[ GetScopeObject ] = "GetScopeObject";
				_opNames[ GetProperty ] = "GetProperty";
				_opNames[ GetPropertyLate ] = "GetPropertyLate";
				_opNames[ InitProperty ] = "InitProperty";
				_opNames[ SetPropertyLate ] = "SetPropertyLate";
				_opNames[ DeleteProperty ] = "DeleteProperty";
				_opNames[ DeletePropertyLate ] = "DeletePropertyLate";
				_opNames[ GetSlot ] = "GetSlot";
				_opNames[ SetSlot ] = "SetSlot";
				_opNames[ GetGlobalSlot ] = "GetGlobalSlot";
				_opNames[ SetGlobalSlot ] = "SetGlobalSlot";
				_opNames[ ConvertString ] = "ConvertString";
				_opNames[ EscXmlElem ] = "EscXmlElem";
				_opNames[ EscXmlAttr ] = "EscXmlAttr";
				_opNames[ ConvertInt ] = "ConvertInt";
				_opNames[ ConvertUInt ] = "ConvertUInt";
				_opNames[ ConvertDouble ] = "ConvertDouble";
				_opNames[ ConvertBoolean ] = "ConvertBoolean";
				_opNames[ ConvertObject ] = "ConvertObject";
				_opNames[ CheckFilter ] = "CheckFilter";
				_opNames[ Coerce ] = "Coerce";
				_opNames[ CoerceBoolean ] = "CoerceBoolean";
				_opNames[ CoerceAny ] = "CoerceAny";
				_opNames[ CoerceInt ] = "CoerceInt";
				_opNames[ CoerceDouble ] = "CoerceDouble";
				_opNames[ CoerceString ] = "CoerceString";
				_opNames[ AsType ] = "AsType";
				_opNames[ AsTypeLate ] = "AsTypeLate";
				_opNames[ CoerceUInt ] = "CoerceUInt";
				_opNames[ CoerceObject ] = "CoerceObject";
				_opNames[ Negate ] = "Negate";
				_opNames[ Increment ] = "Increment";
				_opNames[ IncLocal ] = "IncLocal";
				_opNames[ Decrement ] = "Decrement";
				_opNames[ DecLocal ] = "DecLocal";
				_opNames[ TypeOf ] = "TypeOf";
				_opNames[ Not ] = "Not";
				_opNames[ BitNot ] = "BitNot";
				_opNames[ Concat ] = "Concat";
				_opNames[ AddDouble ] = "AddDouble";
				_opNames[ Add ] = "Add";
				_opNames[ Subtract ] = "Subtract";
				_opNames[ Multiply ] = "Multiply";
				_opNames[ Divide ] = "Divide";
				_opNames[ Modulo ] = "Modulo";
				_opNames[ ShiftLeft ] = "ShiftLeft";
				_opNames[ ShiftRight ] = "ShiftRight";
				_opNames[ ShiftRightUnsigned ] = "ShiftRightUnsigned";
				_opNames[ BitAnd ] = "BitAnd";
				_opNames[ BitOr ] = "BitOr";
				_opNames[ BitXor ] = "BitXor";
				_opNames[ Equals ] = "Equals";
				_opNames[ StrictEquals ] = "StrictEquals";
				_opNames[ LessThan ] = "LessThan";
				_opNames[ LessEquals ] = "LessEquals";
				_opNames[ GreaterThan ] = "GreaterThan";
				_opNames[ GreaterEquals ] = "GreaterEquals";
				_opNames[ InstanceOf ] = "InstanceOf";
				_opNames[ IsType ] = "IsType";
				_opNames[ IsTypeLate ] = "IsTypeLate";
				_opNames[ In ] = "In";
				_opNames[ IncrementInt ] = "IncrementInt";
				_opNames[ DecrementInt ] = "DecrementInt";
				_opNames[ IncLocalInt ] = "IncLocalInt";
				_opNames[ DecLocalInt ] = "DecLocalInt";
				_opNames[ NegateInt ] = "NegateInt";
				_opNames[ AddInt ] = "AddInt";
				_opNames[ SubtractInt ] = "SubtractInt";
				_opNames[ MultiplyInt ] = "MultiplyInt";
				_opNames[ GetLocal0 ] = "GetLocal0";
				_opNames[ GetLocal1 ] = "GetLocal1";
				_opNames[ GetLocal2 ] = "GetLocal2";
				_opNames[ GetLocal3 ] = "GetLocal3";
				_opNames[ SetLocal0 ] = "SetLocal0";
				_opNames[ SetLocal1 ] = "SetLocal1";
				_opNames[ SetLocal2 ] = "SetLocal2";
				_opNames[ SetLocal3 ] = "SetLocal3";
				_opNames[ Debug ] = "Debug";
				_opNames[ DebugLine ] = "DebugLine";
				_opNames[ DebugFile ] = "DebugFile";
				_opNames[ BreakpointLine ] = "BreakpointLine";

				_opNames[ SetByte ] = "SetByte";
				_opNames[ SetShort ] = "SetShort";
				_opNames[ SetInt ] = "SetInt";
				_opNames[ SetFloat ] = "SetFloat";
				_opNames[ SetDouble ] = "SetDouble";
				_opNames[ GetByte ] = "GetByte";
				_opNames[ GetShort ] = "GetShort";
				_opNames[ GetInt ] = "GetInt";
				_opNames[ GetFloat ] = "GetFloat";
				_opNames[ GetDouble ] = "GetDouble";
				_opNames[ Sign1 ] = "Sign1";
				_opNames[ Sign8 ] = "Sign8";
				_opNames[ Sign16 ] = "Sign16";

			}

			return _opNames;
		}

	}
"""

// https://github.com/imcj/as3abc/blob/master/src/com/codeazur/as3abc/factories/OperationFactory.as
private val factory = """
					case Opcodes.Add:					return new BasicOperation(code);
				case Opcodes.AddDouble:				return new BasicOperation(code);
				case Opcodes.AddInt:				return new BasicOperation(code);
				case Opcodes.ApplyType:				return new IntOperation(code);
				case Opcodes.AsType:				return new MultinameOperation(code);
				case Opcodes.AsTypeLate:			return new BasicOperation(code);
				case Opcodes.BitAnd:				return new BasicOperation(code);
				case Opcodes.BitNot:				return new BasicOperation(code);
				case Opcodes.BitOr:					return new BasicOperation(code);
				case Opcodes.BitXor:				return new BasicOperation(code);
				case Opcodes.Breakpoint:			return new BasicOperation(code);
				case Opcodes.BreakpointLine:		return new BasicOperation(code);
				case Opcodes.Call:					return new IntOperation(code);
				case Opcodes.CallMethod:			return new MethodOperation(code);
				case Opcodes.CallProperty:			return new MultinameIntOperation(code);
				case Opcodes.CallPropLex:			return new MultinameIntOperation(code);
				case Opcodes.CallPropVoid:			return new MultinameIntOperation(code);
				case Opcodes.CallStatic:			return new MethodOperation(code);
				case Opcodes.CallSuper:				return new MultinameIntOperation(code);
				case Opcodes.CallSuperVoid:			return new MultinameIntOperation(code);
				case Opcodes.CheckFilter:			return new BasicOperation(code);
				case Opcodes.Coerce:				return new MultinameOperation(code);
				case Opcodes.CoerceAny:				return new BasicOperation(code);
				case Opcodes.CoerceBoolean:			return new BasicOperation(code);
				case Opcodes.CoerceDouble:			return new BasicOperation(code);
				case Opcodes.CoerceInt:				return new BasicOperation(code);
				case Opcodes.CoerceObject:			return new BasicOperation(code);
				case Opcodes.CoerceString:			return new BasicOperation(code);
				case Opcodes.CoerceUInt:			return new BasicOperation(code);
				case Opcodes.Concat:				return new BasicOperation(code);
				case Opcodes.Construct:				return new IntOperation(code);
				case Opcodes.ConstructProp:			return new MultinameIntOperation(code);
				case Opcodes.ConstructSuper:		return new IntOperation(code);
				case Opcodes.ConvertBoolean:		return new BasicOperation(code);
				case Opcodes.ConvertDouble:			return new BasicOperation(code);
				case Opcodes.ConvertInt:			return new BasicOperation(code);
				case Opcodes.ConvertObject:			return new BasicOperation(code);
				case Opcodes.ConvertString:			return new BasicOperation(code);
				case Opcodes.ConvertUInt:			return new BasicOperation(code);
				case Opcodes.Debug:					return new IntStringIntIntOperation(code);
				case Opcodes.DebugFile:				return new StringOperation(code);
				case Opcodes.DebugLine:				return new IntOperation(code);
				case Opcodes.DecLocal:				return new IntOperation(code);
				case Opcodes.DecLocalInt:			return new IntOperation(code);
				case Opcodes.Decrement:				return new BasicOperation(code);
				case Opcodes.DecrementInt:			return new BasicOperation(code);
				case Opcodes.DefaultXmlNamespace:	return new StringOperation(code);
				case Opcodes.DefaultXmlNamespaceL:	return new BasicOperation(code);
				case Opcodes.DeleteProperty:		return new MultinameOperation(code);
				case Opcodes.DeletePropertyLate:	return new BasicOperation(code);
				case Opcodes.Divide:				return new BasicOperation(code);
				case Opcodes.Dup:					return new BasicOperation(code);
				case Opcodes.Equals:				return new BasicOperation(code);
				case Opcodes.EscXmlAttr:			return new BasicOperation(code);
				case Opcodes.EscXmlElem:			return new BasicOperation(code);
				case Opcodes.FindProperty:			return new MultinameOperation(code);
				case Opcodes.FindPropStrict:		return new MultinameOperation(code);
				case Opcodes.GetByte:				return new BasicOperation(code);
				case Opcodes.GetDescendants:		return new MultinameOperation(code);
				case Opcodes.GetDouble:				return new BasicOperation(code);
				case Opcodes.GetFloat:				return new BasicOperation(code);
				case Opcodes.GetGlobalScope:		return new BasicOperation(code);
				case Opcodes.GetGlobalSlot:			return new IntOperation(code);
				case Opcodes.GetInt:				return new BasicOperation(code);
				case Opcodes.GetLex:				return new MultinameOperation(code);
				case Opcodes.GetLocal:				return new IntOperation(code);
				case Opcodes.GetLocal0:				return new BasicOperation(code);
				case Opcodes.GetLocal1:				return new BasicOperation(code);
				case Opcodes.GetLocal2:				return new BasicOperation(code);
				case Opcodes.GetLocal3:				return new BasicOperation(code);
				case Opcodes.GetProperty:			return new MultinameOperation(code);
				case Opcodes.GetPropertyLate:		return new BasicOperation(code);
				case Opcodes.GetScopeObject:		return new IntOperation(code);
				case Opcodes.GetShort:				return new BasicOperation(code);
				case Opcodes.GetSlot:				return new IntOperation(code);
				case Opcodes.GetSuper:				return new MultinameOperation(code);
				case Opcodes.GreaterEquals:			return new BasicOperation(code);
				case Opcodes.GreaterThan:			return new BasicOperation(code);
				case Opcodes.HasNext:				return new BasicOperation(code);
				case Opcodes.HasNext2:				return new IntIntOperation(code);
				case Opcodes.IfEqual:				return new ConditionalJumpOperation(code);
				case Opcodes.IfFalse:				return new ConditionalJumpOperation(code);
				case Opcodes.IfGreaterEqual:		return new ConditionalJumpOperation(code);
				case Opcodes.IfGreaterThan:			return new ConditionalJumpOperation(code);
				case Opcodes.IfLessEqual:			return new ConditionalJumpOperation(code);
				case Opcodes.IfLessThan:			return new ConditionalJumpOperation(code);
				case Opcodes.IfNotEqual:			return new ConditionalJumpOperation(code);
				case Opcodes.IfNotGreaterEqual:		return new ConditionalJumpOperation(code);
				case Opcodes.IfNotGreaterThan:		return new ConditionalJumpOperation(code);
				case Opcodes.IfNotLessEqual:		return new ConditionalJumpOperation(code);
				case Opcodes.IfNotLessThan:			return new ConditionalJumpOperation(code);
				case Opcodes.IfStrictEqual:			return new ConditionalJumpOperation(code);
				case Opcodes.IfStrictNotEqual:		return new ConditionalJumpOperation(code);
				case Opcodes.IfTrue:				return new ConditionalJumpOperation(code);
				case Opcodes.In:					return new BasicOperation(code);
				case Opcodes.IncLocal:				return new IntOperation(code);
				case Opcodes.IncLocalInt:			return new IntOperation(code);
				case Opcodes.Increment:				return new BasicOperation(code);
				case Opcodes.IncrementInt:			return new BasicOperation(code);
				case Opcodes.InitProperty:			return new MultinameOperation(code);
				case Opcodes.InstanceOf:			return new BasicOperation(code);
				case Opcodes.IsType:				return new MultinameOperation(code);
				case Opcodes.IsTypeLate:			return new BasicOperation(code);
				case Opcodes.Jump:					return new JumpOperation(code);
				case Opcodes.Kill:					return new IntOperation(code);
				case Opcodes.Label:					return new LabelOperation(code);
				case Opcodes.LessEquals:			return new BasicOperation(code);
				case Opcodes.LessThan:				return new BasicOperation(code);
				case Opcodes.LookupSwitch:			return new LookupSwitchOperation(code);
				case Opcodes.Modulo:				return new BasicOperation(code);
				case Opcodes.Multiply:				return new BasicOperation(code);
				case Opcodes.MultiplyInt:			return new BasicOperation(code);
				case Opcodes.Negate:				return new BasicOperation(code);
				case Opcodes.NegateInt:				return new BasicOperation(code);
				case Opcodes.NewActivation:			return new BasicOperation(code);
				case Opcodes.NewArray:				return new IntOperation(code);
				case Opcodes.NewCatch:				return new NewCatchOperation(code);
				case Opcodes.NewClass:				return new NewClassOperation(code);
				case Opcodes.NewFunction:			return new NewFunctionOperation(code);
				case Opcodes.NewObject:				return new IntOperation(code);
				case Opcodes.NextName:				return new BasicOperation(code);
				case Opcodes.NextValue:				return new BasicOperation(code);
				case Opcodes.Not:					return new BasicOperation(code);
				case Opcodes.Nop:					return new BasicOperation(code);
				case Opcodes.Pop:					return new BasicOperation(code);
				case Opcodes.PopScope:				return new BasicOperation(code);
				case Opcodes.PushByte:				return new IntOperation(code);
				case Opcodes.PushDouble:			return new NumberOperation(code);
				case Opcodes.PushFalse:				return new BasicOperation(code);
				case Opcodes.PushInt:				return new IntOperation(code);
				case Opcodes.PushNamespace:			return new NamespaceOperation(code);
				case Opcodes.PushNaN:				return new BasicOperation(code);
				case Opcodes.PushNull:				return new BasicOperation(code);
				case Opcodes.PushScope:				return new BasicOperation(code);
				case Opcodes.PushShort:				return new IntOperation(code);
				case Opcodes.PushString:			return new StringOperation(code);
				case Opcodes.PushTrue:				return new BasicOperation(code);
				case Opcodes.PushUInt:				return new UIntOperation(code);
				case Opcodes.PushUndefined:			return new BasicOperation(code);
				case Opcodes.PushWith:				return new BasicOperation(code);
				case Opcodes.ReturnValue:			return new BasicOperation(code);
				case Opcodes.ReturnVoid:			return new BasicOperation(code);
				case Opcodes.SetByte:				return new BasicOperation(code);
				case Opcodes.SetDouble:				return new BasicOperation(code);
				case Opcodes.SetFloat:				return new BasicOperation(code);
				case Opcodes.SetGlobalSlot:			return new IntOperation(code);
				case Opcodes.SetInt:				return new BasicOperation(code);
				case Opcodes.SetLocal:				return new IntOperation(code);
				case Opcodes.SetLocal0:				return new BasicOperation(code);
				case Opcodes.SetLocal1:				return new BasicOperation(code);
				case Opcodes.SetLocal2:				return new BasicOperation(code);
				case Opcodes.SetLocal3:				return new BasicOperation(code);
				case Opcodes.SetProperty:			return new MultinameOperation(code);
				case Opcodes.SetPropertyLate:		return new BasicOperation(code);
				case Opcodes.SetShort:				return new BasicOperation(code);
				case Opcodes.SetSlot:				return new IntOperation(code);
				case Opcodes.SetSuper:				return new MultinameOperation(code);
				case Opcodes.ShiftLeft:				return new BasicOperation(code);
				case Opcodes.ShiftRight:			return new BasicOperation(code);
				case Opcodes.ShiftRightUnsigned:	return new BasicOperation(code);
				case Opcodes.Sign1:					return new BasicOperation(code);
				case Opcodes.Sign16:				return new BasicOperation(code);
				case Opcodes.Sign8:					return new BasicOperation(code);
				case Opcodes.StrictEquals:			return new BasicOperation(code);
				case Opcodes.Subtract:				return new BasicOperation(code);
				case Opcodes.SubtractInt:			return new BasicOperation(code);
				case Opcodes.Swap:					return new BasicOperation(code);
				case Opcodes.Throw:					return new BasicOperation(code);
				case Opcodes.TypeOf:				return new BasicOperation(code);
"""
