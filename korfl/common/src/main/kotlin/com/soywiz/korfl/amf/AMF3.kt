package com.soywiz.korfl.amf

import com.soywiz.klock.DateTime
import com.soywiz.korio.lang.Undefined
import com.soywiz.korio.serialization.xml.Xml
import com.soywiz.korio.stream.*

// http://wwwimages.adobe.com/www.adobe.com/content/dam/Adobe/en/devnet/amf/pdf/amf-file-format-spec.pdf
object AMF3 {
	fun read(s: SyncStream): Any? {
		val out = Reader(s).read()
		return out
	}

	class Reader(val i: SyncStream) {
		fun readObject(): Map<String, Any?> {
			var len = readInt()
			val dyn = ((len ushr 3) and 0x01) == 1
			len = len ushr 4
			i.readU8()
			val h = hashMapOf<String, Any?>()
			if (dyn) {
				while (true) {
					val s = readString()
					if (s == "") break
					h[s] = read()
				}
			} else {
				val keys = (0 until len).map { readString() }
				for (n in 0 until len) h[keys[n]] = read()
			}
			return h
		}

		fun readMap(len: Int): Map<Any?, Any?> {
			val h = hashMapOf<Any?, Any?>()
			i.readU8()
			for (i in 0 until len) h[read()] = read()
			return h
		}

		fun readArray(n: Int): List<Any?> {
			val a = arrayListOf<Any?>()
			read()
			for (i in 0 until n) a.add(read())
			return a
		}

		fun readBytes(size: Int): ByteArray = i.readBytes(size)

		fun readInt(preShift: Int = 0): Int {
			var ret = 0
			var c = i.readU8()
			if (c > 0xbf) ret = 0x380
			var n = 0
			while (++n < 4 && c > 0x7f) {
				ret = ret or (c and 0x7f)
				ret = ret shl 7
				c = i.readU8()
			}
			if (n > 3) ret = ret shl 1
			ret = ret or c
			return ret ushr preShift
		}

		fun readString(): String = i.readString(readInt(1))

		fun readWithCode(id: Int): Any? = when (id) {
			0x00 -> Undefined
			0x01 -> null
			0x02 -> false
			0x03 -> true
			0x04 -> readInt()
			0x05 -> i.readF64_be()
			0x06 -> readString()
			0x07 -> throw Error("XMLDocument unsupported")
			0x08 -> run { i.readU8(); DateTime(i.readF64_be().toLong()) }
			0x09 -> readArray(readInt(1))
			0x0a -> readObject()
			0x0b -> Xml(readString())
			0x0c -> readBytes(readInt(1))
			0x0d, 0x0e, 0x0f -> readArray(readInt(1))
			0x10 -> run { val len = readInt(1); readString(); readArray(len) }
			0x11 -> readMap(readInt(1))
			else -> throw Error("Unknown AMF " + id)
		}

		fun read() = readWithCode(i.readU8())
	}


}
