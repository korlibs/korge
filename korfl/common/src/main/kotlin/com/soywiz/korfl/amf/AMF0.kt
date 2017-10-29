package com.soywiz.korfl.amf

import com.soywiz.klock.DateTime
import com.soywiz.korio.lang.Undefined
import com.soywiz.korio.stream.*

object AMF0 {
	fun decode(s: SyncStream): Any? {
		//val chunk = s.sliceWithStart(s.position).readBytes(120)
		//println(chunk.toHexString())
		//println(chunk.toString(Charsets.UTF_8))
		return Reader(s).read()
	}

	class Reader(val i: SyncStream) {
		fun readObject(): Map<String, Any?> {
			val h = hashMapOf<String, Any?>()
			while (true) {
				val len = i.readU16_be()
				val name = i.readString(len)
				val k = i.readU8()
				if (k == 0x09) break
				h[name] = readWithCode(k)
			}
			return h
		}

		fun readWithCode(id: Int): Any? = when (id) {
			0x00 -> i.readF64_be()
			0x01 -> when (i.readU8()) {
				0 -> false
				1 -> true
				else -> throw Error("Invalid AMF")
			}
			0x02 -> i.readStringz(i.readU16_be())
			0x03 -> readObject()
			0x08 -> {
				var size = i.readS32_be()
				TODO()
				readObject()
			}
			0x05 -> null
			0x06 -> Undefined
			0x07 -> throw Error("Not supported : Reference")
			0x0A -> {
				val count = i.readS32_be()
				(0 until count).map { read() }
			}
			0x0B -> {
				val time_ms = i.readF64_be()
				val tz_min = i.readU16_be()
				DateTime(time_ms.toLong() + tz_min * 60 * 1000L)
			}
			0x0C -> i.readString(i.readS32_be())
			else -> throw Error("Unknown AMF " + id)
		}

		fun read(): Any? = readWithCode(i.readU8())
	}
}

