package com.soywiz.korio.serialization.binary

import com.soywiz.kmem.*
import com.soywiz.korio.lang.*
import com.soywiz.korio.stream.*
import com.soywiz.korio.util.*
import java.lang.reflect.*
import java.nio.*

interface Struct {
	sealed class Type(val size: Int) {
		object S1 : Type(1)
		object S2 : Type(2)
		object S4 : Type(4)
		object S8 : Type(8)
		object F4 : Type(4)
		object F8 : Type(8)
		class CUSTOM(val elementClazz: Class<Struct>) : Type(StructReflect[elementClazz].size)
		class ARRAY(val elementType: Type, val count: Int) : Type(elementType.size * count)
		class STRING(val charset: Charset, val count: Int) : Type(count)
	}

	//enum class Type(val size: Int) {
	//	S1(1), S2(2), S4(4), S8(8),
	//	F4(4), F8(8), CUSTOM(-1)
	//}

}

@Target(AnnotationTarget.FIELD, AnnotationTarget.CLASS)
annotation class LE

@Target(AnnotationTarget.FIELD, AnnotationTarget.CLASS)
annotation class BE

annotation class Size(val size: Int)
@Target(AnnotationTarget.FIELD)
annotation class Offset(val offset: Int)

@Target(AnnotationTarget.FIELD)
annotation class Count(val count: Int)

@Target(AnnotationTarget.FIELD)
annotation class DynamicCount(val fieldname: String)

@Target(AnnotationTarget.FIELD)
annotation class Encoding(val name: String)

@Target(AnnotationTarget.FIELD)
annotation class Order(val order: Int)
//@Target(AnnotationTarget.FIELD) annotation class U1
//@Target(AnnotationTarget.FIELD) annotation class U2
//@Target(AnnotationTarget.FIELD) annotation class U4

class StructReflect<T>(val clazz: Class<T>) {
	data class FieldInfo(
		val field: Field,
		val offset: Int,
		val type: Struct.Type,
		val bigEndian: Boolean
	)

	val cf = ClassFactory(clazz)
	val constructor = clazz.declaredConstructors.firstOrNull()
		?: throw IllegalArgumentException("Class $clazz doesn't have constructors")
	val fields = clazz.declaredFields
	val globalBo = if (clazz.getAnnotation(LE::class.java) != null) {
		ByteOrder.LITTLE_ENDIAN
	} else if (clazz.getAnnotation(BE::class.java) != null) {
		ByteOrder.BIG_ENDIAN
	} else {
		null
	}

	fun decodeType(field: Field, clazz: Class<*>): Struct.Type {
		return when (clazz) {
			java.lang.Byte.TYPE -> Struct.Type.S1
			java.lang.Short.TYPE -> Struct.Type.S2
			java.lang.Integer.TYPE -> Struct.Type.S4
			java.lang.Long.TYPE -> Struct.Type.S8
			java.lang.Float.TYPE -> Struct.Type.F4
			java.lang.Double.TYPE -> Struct.Type.F8
			java.lang.String::class.java -> {
				val encoding = field.getAnnotation(Encoding::class.java)
				val count = field.getAnnotation(Count::class.java)
				val charset = Charset.forName(encoding.name)
				Struct.Type.STRING(charset, count.count)
			}
			else -> {
				if (clazz.isArray) {
					val count = field.getAnnotation(Count::class.java)
					Struct.Type.ARRAY(decodeType(field, clazz.componentType), count.count)
				} else {
					Struct.Type.CUSTOM(clazz as Class<Struct>)
				}
			}
		}
	}

	var lastOffset = 0

	val fieldsWithAnnotation = fields
		.filter { (it.getAnnotation(Offset::class.java) != null) || (it.getAnnotation(Order::class.java) != null) }
		.sortedBy { it.getAnnotation(Offset::class.java)?.offset ?: it.getAnnotation(Order::class.java)?.order ?: 0 }
		.map {
			val bo = if (it.getAnnotation(LE::class.java) != null) {
				ByteOrder.LITTLE_ENDIAN
			} else if (it.getAnnotation(BE::class.java) != null) {
				ByteOrder.BIG_ENDIAN
			} else {
				null
			}

			val ab = bo ?: globalBo ?: ByteOrder.LITTLE_ENDIAN
			val littleEndian = (ab == ByteOrder.LITTLE_ENDIAN)

			val type = decodeType(it, it.type)

			val offset = it.getAnnotation(Offset::class.java)?.offset ?: lastOffset

			lastOffset = offset + type.size

			FieldInfo(it, offset, type, littleEndian)
		}
		.sortedBy { it.offset }

	val specifiedSize = clazz.getAnnotation(Size::class.java)?.size
	val calculatedSize = fieldsWithAnnotation.map { it.offset + it.type.size }.max()
	val size = specifiedSize ?: calculatedSize ?: fieldsWithAnnotation.map { it.offset + it.type.size }.max()
	?: throw IllegalArgumentException("Empty struct $clazz or without @Offset")

	@Suppress("UNCHECKED_CAST")
	fun create(): T = cf.createDummy()

	init {
		for (f in fields) {
			f.isAccessible = true
		}
	}

	companion object {
		val cache = linkedMapOf<Class<*>, StructReflect<*>>()
		@Suppress("UNCHECKED_CAST")
		operator fun <T> get(clazz: Class<T>): StructReflect<T> {
			return cache.getOrPut(clazz) {
				StructReflect<T>(clazz)
			} as StructReflect<T>
		}
	}
}

fun <T : Struct> Class<T>.getStructSize(): Int = StructReflect[this].size

fun ByteArray.readStructElement(offset: Int, type: Struct.Type, littleEndian: Boolean): Any {
	val data = this
	return when (type) {
		Struct.Type.S1 -> data.readS8(offset).toByte()
		Struct.Type.S2 -> data.readS16(offset, littleEndian).toShort()
		Struct.Type.S4 -> data.readS32(offset, littleEndian)
		Struct.Type.S8 -> data.readS64(offset, littleEndian)
		Struct.Type.F4 -> data.readF32(offset, littleEndian)
		Struct.Type.F8 -> data.readF64(offset, littleEndian)
		is Struct.Type.CUSTOM -> data.readStruct(offset, type.elementClazz)
		is Struct.Type.ARRAY -> {
			val elementSize = type.elementType.size
			val elementType = type.elementType
			val count = type.count
			when (elementType) {
				Struct.Type.S1 -> readByteArray(offset, count)
				Struct.Type.S2 -> readShortArray(offset, count, littleEndian)
				Struct.Type.S4 -> readIntArray(offset, count, littleEndian)
				Struct.Type.S8 -> readLongArray(offset, count, littleEndian)
				Struct.Type.F4 -> readFloatArray(offset, count, littleEndian)
				Struct.Type.F8 -> readDoubleArray(offset, count, littleEndian)
				else -> {

					val al =
						(0 until count).map { readStructElement(offset + elementSize * it, elementType, littleEndian) }
					val out = java.lang.reflect.Array.newInstance(al.first()::class.java, al.size)
					for (n in 0 until count) java.lang.reflect.Array.set(out, n, al[n])
					out
				}
			}
		}
		is Struct.Type.STRING -> {
			val strBytes = readByteArray(offset, type.count)
			strBytes.copyOf(strBytes.indexOf(0, default = strBytes.size)).toString(type.charset)
		}
	}
}

fun <T : Struct> ByteArray.readStruct(offset: Int, clazz: Class<T>): T {
	val sr = StructReflect[clazz]
	val obj = sr.create()

	for ((field, o, type, littleEndian) in sr.fieldsWithAnnotation) {
		field.set(obj, readStructElement(offset + o, type, littleEndian))
	}

	return obj
}

inline fun <reified T : Struct> SyncStream.readStruct() = this.readStruct(T::class.java)
fun <T : Struct> SyncStream.readStruct(clazz: Class<T>): T {
	return readBytes(clazz.getStructSize()).readStruct(0, clazz)
}

fun ByteArray.writeStructElement(offset: Int, type: Struct.Type, value: Any, littleEndian: Boolean): Int {
	when (type) {
		Struct.Type.S1 -> write8(offset, (value as Byte).toInt())
		Struct.Type.S2 -> write16(offset, (value as Short).toInt(), littleEndian)
		Struct.Type.S4 -> write32(offset, (value as Int).toInt(), littleEndian)
		Struct.Type.S8 -> write64(offset, (value as Long).toLong(), littleEndian)
		Struct.Type.F4 -> writeF32(offset, (value as Float).toFloat(), littleEndian)
		Struct.Type.F8 -> writeF64(offset, (value as Double).toDouble(), littleEndian)
		is Struct.Type.CUSTOM -> writeStruct(offset, value as Struct)
		is Struct.Type.ARRAY -> {
			var co = offset
			for (n in 0 until type.count) {
				co += writeStructElement(co, type.elementType, java.lang.reflect.Array.get(value, n), littleEndian)
			}
		}
		is Struct.Type.STRING -> {
			writeBytes(offset, ((value as String).toByteArray(type.charset).copyOf(type.count)))
		}
	}
	return type.size
}

fun <T : Struct> ByteArray.writeStruct(offset: Int, obj: T): ByteArray {
	val sr = StructReflect[obj::class.java]
	val out = this

	for ((field, o, type, littleEndian) in sr.fieldsWithAnnotation) {
		out.writeStructElement(offset + o, type, field.get(obj), littleEndian)
	}

	return out
}

fun <T : Struct> T.getStructBytes(): ByteArray = ByteArray(StructReflect[this::class.java].size).writeStruct(0, this)

fun <T : Struct> SyncStream.writeStruct(obj: T) = this.writeBytes(obj.getStructBytes())

suspend inline fun <reified T : Struct> AsyncStream.readStruct() = this.readStruct(T::class.java)
suspend fun <T : Struct> AsyncStream.readStruct(clazz: Class<T>): T {
	return readBytesExact(clazz.getStructSize()).readStruct(0, clazz)
}

suspend fun <T : Struct> AsyncStream.writeStruct(obj: T) = this.writeBytes(obj.getStructBytes())

