package com.soywiz.korfl.abc

import com.soywiz.korio.error.invalidOp
import com.soywiz.korio.stream.*

class AbcConstantPool {
	var ints = listOf<Int>()
	var uints = listOf<Int>()
	var doubles = listOf<Double>()
	var strings = listOf<String>()
	var namespaces = listOf<ABC.Namespace>()
	var namespaceSets = listOf<List<ABC.Namespace>>()
	var multinames = listOf<ABC.AbstractMultiname>()

	fun readConstantPool(s: SyncStream) {
		val intCount = s.readU30()
		ints = listOf(0) + (1 until intCount).map { s.readU30() }
		val uintCount = s.readU30()
		uints = listOf(0) + (1 until uintCount).map { s.readU30() }
		val doubleCount = s.readU30()
		doubles = listOf(0.0) + (1 until doubleCount).map { s.readF64_le() }
		val stringCount = s.readU30()
		strings = listOf("") + (1 until stringCount).map { s.readStringz(s.readU30()) }
		namespaces = listOf(ABC.Namespace.EMPTY) + (1 until s.readU30()).map {
			val kind = s.readU8()
			val name = strings[s.readU30()]
			ABC.Namespace(kind, name)
		}
		namespaceSets = listOf(listOf<ABC.Namespace>()) + (1 until s.readU30()).map {
			(0 until s.readU30()).map { namespaces[s.readU30()] }
		}
		multinames = listOf(ABC.EmptyMultiname) + (1 until s.readU30()).map {
			val kind = s.readU8()

			when (kind) {
				0x07 -> ABC.ABCQName(namespaces[s.readU30()], strings[s.readU30()])
				0x0D -> ABC.QNameA(namespaces[s.readU30()], strings[s.readU30()])
				0x0F -> ABC.RTQName(strings[s.readU30()])
				0x10 -> ABC.RTQNameA(strings[s.readU30()])
				0x11 -> ABC.RTQNameL
				0x12 -> ABC.RTQNameLA
				0x09 -> ABC.Multiname(strings[s.readU30()], namespaceSets[s.readU30()])
				0x0E -> ABC.MultinameA(strings[s.readU30()], namespaceSets[s.readU30()])
				0x1B -> ABC.MultinameL(namespaceSets[s.readU30()])
				0x1C -> ABC.MultinameLA(namespaceSets[s.readU30()])
				0x1D -> ABC.TypeName(s.readU30(), (0 until s.readU30()).map { s.readU30() })
				else -> invalidOp("Unsupported $kind")
			}
		}
		//println(ints)
		//println(uints)
		//println(doubles)
		//println(strings)
		//println(namespaces)
		//println(namespaceSets)
		//println(multinames)
	}
}
