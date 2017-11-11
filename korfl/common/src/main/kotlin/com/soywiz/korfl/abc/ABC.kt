package com.soywiz.korfl.abc

import com.soywiz.kds.ext.mapWhile
import com.soywiz.korio.error.invalidOp
import com.soywiz.korio.stream.*

// http://wwwimages.adobe.com/content/dam/Adobe/en/devnet/actionscript/articles/avm2overview.pdf
// https://github.com/imcj/as3abc/blob/master/src/com/codeazur/as3abc/ABC.as
class ABC {
	data class Namespace(val kind: Int, val name: String) {
		companion object {
			const val NAMESPACE = 0x08
			const val PACKAGE_NAMESPACE = 0x16
			const val PACKAGE_INTERNAL_NAMESPACE = 0x17
			const val PROTECTED_NAMESPACE = 0x18
			const val EXPLICIT_NAMESPACE = 0x19
			const val STATIC_PROTECTED_NAMESPACE = 0x1A
			const val PRIVATE_NAMESPACE = 0x05
			val EMPTY = Namespace(0, "")
		}

		override fun toString(): String = "$name"
	}

	interface AbstractMultiname {
		val simpleName: String
	}

	object EmptyMultiname : AbstractMultiname {
		override val simpleName: String = ""
	}

	data class ABCQName(val namespace: Namespace, val name: String) : AbstractMultiname {
		override fun toString(): String = "$namespace.$name"
		override val simpleName: String = name
	}

	data class QNameA(val namespace: Namespace, val name: String) : AbstractMultiname {
		override val simpleName: String = name
	}

	data class RTQName(val name: String) : AbstractMultiname {
		override val simpleName: String = name
	}

	data class RTQNameA(val name: String) : AbstractMultiname {
		override val simpleName: String = name
	}

	object RTQNameL : AbstractMultiname {
		override val simpleName: String = ""
	}

	object RTQNameLA : AbstractMultiname {
		override val simpleName: String = ""
	}

	data class Multiname(val name: String, val namespaceSet: List<Namespace>) : AbstractMultiname {
		override val simpleName: String = name
	}

	data class MultinameA(val name: String, val namespaceSet: List<Namespace>) : AbstractMultiname {
		override val simpleName: String = name
	}

	data class MultinameL(val namespaceSet: List<Namespace>) : AbstractMultiname {
		override val simpleName: String = ""
	}

	data class MultinameLA(val namespaceSet: List<Namespace>) : AbstractMultiname {
		override val simpleName: String = ""
	}

	data class TypeName(val qname: Int, val parameters: List<Int>) : AbstractMultiname {
		override val simpleName: String = "TypeName($qname)"
	}

	var methodsDesc = listOf<MethodDesc>()
	var instancesInfo = listOf<InstanceInfo>()
	var classesInfo = listOf<ClassInfo>()
	var typesInfo = listOf<TypeInfo>()
	var scriptsInfo = listOf<ScriptInfo>()
	var methodsBodies = listOf<MethodBody>()
	var metadatas = listOf<Metadata>()
	val cpool = AbcConstantPool()
	val ints: List<Int> get() = cpool.ints
	val uints: List<Int> get() = cpool.uints
	val doubles: List<Double> get() = cpool.doubles
	val strings: List<String> get() = cpool.strings
	val namespaces: List<ABC.Namespace> get() = cpool.namespaces
	val namespaceSets: List<List<ABC.Namespace>> get() = cpool.namespaceSets
	val multinames: List<ABC.AbstractMultiname> get() = cpool.multinames

	data class Metadata(val name: String, val values: Map<String, String>)

	fun readFile(s: SyncStream) = this.apply {
		//syncTest {
		//	s.slice().readAll().writeToFile(File("c:/temp/demo.abc"))
		//}
		val minor = s.readU16_le()
		val major = s.readU16_le()
		//println("version: major=$major, minor=$minor")
		cpool.readConstantPool(s)

		// readMethods
		methodsDesc = (0 until s.readU30()).map {
			readMethod(s)
		}

		//println("Methods: $methodsDesc")

		// readMetadata
		metadatas = (0 until s.readU30()).map {
			val name = strings[s.readU30()]
			val items = (0 until s.readU30()).map { strings[s.readU30()] to strings[s.readU30()] }
			Metadata(name, items.toMap())
		}

		//println("Metadatas: $metadatas")

		val typeCount = s.readU30()

		// readInstances
		instancesInfo = (0 until typeCount).map {
			readInstance(s)
		}

		// readClasses
		classesInfo = (0 until typeCount).map {
			readClass(s)
		}

		typesInfo = instancesInfo.zip(classesInfo).map { TypeInfo(this, it.first, it.second) }

		//println("Classes: $classesInfo")

		// readScripts
		scriptsInfo = (0 until s.readU30()).map {
			ScriptInfo(methodsDesc[s.readU30()], readTraits(s))
		}

		//println("Scripts: $scripts")

		// readScripts
		methodsBodies = (0 until s.readU30()).map {
			val method = methodsDesc[s.readU30()]
			val maxStack = s.readU30()
			val localCount = s.readU30()
			val initScopeDepth = s.readU30()
			val maxScopeDepth = s.readU30()
			val opcodes = s.readBytes(s.readU30())
			val exceptions = (0 until s.readU30()).map {
				ExceptionInfo(
					from = s.readU30(),
					to = s.readU30(),
					target = s.readU30(),
					type = multinames[s.readU30()],
					variableName = multinames[s.readU30()]
				)
			}
			val traits = readTraits(s)

			MethodBody(
				method = method,
				maxStack = maxStack,
				localCount = localCount,
				initScopeDepth = initScopeDepth,
				maxScopeDepth = maxScopeDepth,
				opcodes = opcodes,
				cpool = cpool,
				exceptions = exceptions,
				traits = traits
			)
		}

		//println("MethodBodies: $methodBodies")

		//println("Available: ${s.available}")
	}

	class MethodBody(
		val method: MethodDesc,
		val maxStack: Int,
		val localCount: Int,
		val initScopeDepth: Int,
		val maxScopeDepth: Int,
		val opcodes: ByteArray,
		val cpool: AbcConstantPool,
		val exceptions: List<ExceptionInfo>,
		val traits: List<Trait>
	) {
		val ops by lazy {
			opcodes.openSync().run {
				mapWhile(cond = { !this.eof }, gen = { AbcOperation.read(cpool, this) })
			}
		}

		init {
			method.body = this
		}
	}

	data class ExceptionInfo(val from: Int, val to: Int, val target: Int, val type: AbstractMultiname, val variableName: AbstractMultiname)

	interface Trait {
		val name: AbstractMultiname
	}

	data class TraitSlot(override val name: AbstractMultiname, val slotIndex: Int, val type: AbstractMultiname, val value: Any?) : Trait
	data class TraitMethod(override val name: AbstractMultiname, val dispIndex: Int, val methodIndex: Int) : Trait
	data class TraitClass(override val name: AbstractMultiname, val slotIndex: Int, val classIndex: Int) : Trait
	data class TraitFunction(override val name: AbstractMultiname, val slotIndex: Int, val functionIndex: Int) : Trait

	data class InstanceInfo(val name: ABCQName, val base: AbstractMultiname, val interfaces: List<AbstractMultiname>, val instanceInitializer: MethodDesc, val traits: List<Trait>)
	data class ClassInfo(val initializer: MethodDesc, val traits: List<Trait>)

	class TypeInfo(val abc: ABC, val instanceInfo: InstanceInfo, val classInfo: ClassInfo) {
		val name get() = instanceInfo.name
		val instanceTraits get() = instanceInfo.traits
		val classTraits get() = classInfo.traits
	}

	data class ScriptInfo(val initializer: MethodDesc, val traits: List<Trait>)

	fun readClass(s: SyncStream): ClassInfo {
		return ClassInfo(methodsDesc[s.readU30()], readTraits(s))
	}

	fun readInstance(s: SyncStream): InstanceInfo {
		val name = multinames[s.readU30()] as ABCQName
		val base = multinames[s.readU30()]
		val flags = s.readU8()
		val isSealed = (flags and 0x01) != 0
		val isFinal = (flags and 0x02) != 0
		val isInterface = (flags and 0x04) != 0

		if ((flags and 0x08) != 0) {
			val protectedNamespace = namespaces[s.readU30()]
		}

		val interfaces = (0 until s.readU30()).map {
			multinames[s.readU30()]
		}
		val instanceInitializerIndex = s.readU30()
		val traits = readTraits(s)
		return InstanceInfo(
			name = name,
			base = base,
			interfaces = interfaces,
			instanceInitializer = methodsDesc[instanceInitializerIndex],
			traits = traits
		)
	}

	fun getConstantValue(type: Int, index: Int): Any? = when (type) {
		0x03 /* int */ -> ints[index]
		0x04 /* uint */ -> uints[index]
		0x06 /* double */ -> doubles[index]
		0x01 /* UTF-8 */ -> strings[index]
		0x0B /* true */ -> true
		0x0A /* false */ -> false

		0x0C /* null */,
		0x00 /* undefined */ ->
			null

		0x08 /* namespace */,
		0x16 /* package namespace */,
		0x17 /* package internal namespace */,
		0x18 /* protected namespace */,
		0x19 /* explicit namespace */,
		0x1A /* static protected namespace */,
		0x05 /* private namespace */ ->
			namespaces[index]

		else -> invalidOp("Unknown parameter type.")
	}

	fun readTraits(s: SyncStream): List<Trait> {
		return (0 until s.readU30()).map {
			val name = multinames[s.readU30()]
			val kind = s.readU8()
			val info = kind ushr 4
			val hasMetadata = (info and 0x04) != 0
			val traitKind = kind and 0x0f
			val trait: Trait = when (traitKind) {
				0x00, 0x06 -> { // TraitSlot
					val slotIndex = s.readU30()
					val type = multinames[s.readU30()]
					val valueIndex = s.readU30()
					val value = if (valueIndex != 0) {
						val valueKind = s.readU8()
						getConstantValue(valueKind, valueIndex)
					} else {
						null
					}
					TraitSlot(name, slotIndex, type, value)
				}
				0x01, 0x02, 0x03 -> { // TraitMethod, TraitGetter, TraitSetter
					val dispIndex = s.readU30()
					val methodIndex = s.readU30()
					val isFinal = (info and 0x01) != 0
					val isOverride = (info and 0x02) != 0
					TraitMethod(name, dispIndex, methodIndex)
				}
				0x04 -> TraitClass(name, s.readU30(), s.readU30())
				0x05 -> TraitFunction(name, s.readU30(), s.readU30())
				else -> invalidOp("Unknown trait kind $traitKind")
			}
			if (hasMetadata) {
				val metadatas = (0 until s.readU30()).map { metadatas[s.readU30()] }
			}
			trait
		}
	}

	data class MethodDesc(val name: String) {
		var body: MethodBody? = null
	}

	fun readMethod(s: SyncStream): MethodDesc {
		val parameterCount = s.readU30()
		val returnType = multinames[s.readU30()]
		val parameters = (0 until parameterCount).map { multinames[s.readU30()] }
		val name = strings[s.readU30()]
		val flags = s.readU8()
		val needsArguments = (flags and 0x01) != 0
		val needsActivation = (flags and 0x02) != 0
		val needsRest = (flags and 0x04) != 0
		val hasOptionalParameters = (flags and 0x08) != 0
		val setsDXNS = (flags and 0x40) != 0
		val hasParameterNames = (flags and 0x80) != 0

		if (hasOptionalParameters) {
			val optionalCount = s.readU8()
			val optionalValues = (0 until optionalCount).map {
				val valueIndex = s.readU30()
				val optionalType = s.readU8()
				val value = getConstantValue(optionalType, valueIndex)
			}
		}

		if (hasParameterNames) {
			val parameterNames = (0 until parameterCount).map { strings[s.readU30()] }
		}

		//println("$name($parameters):$returnType")
		//TODO()

		return MethodDesc(name)
	}
}

fun SyncStream.readU30(): Int {
	var result = readU8()
	if ((result and 0x80) == 0) return result
	result = (result and 0x7f) or (readU8() shl 7)
	if ((result and 0x4000) == 0) return result
	result = (result and 0x3fff) or (readU8() shl 14)
	if ((result and 0x200000) == 0) return result
	result = (result and 0x1fffff) or (readU8() shl 21)
	if ((result and 0x10000000) == 0) return result
	result = (result and 0xfffffff) or (readU8() shl 28)
	return result
}

