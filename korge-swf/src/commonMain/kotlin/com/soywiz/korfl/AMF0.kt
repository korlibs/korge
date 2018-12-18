package com.soywiz.korfl

import com.soywiz.klock.*
import com.soywiz.korio.lang.*
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
				val len = i.readU16BE()
				val name = i.readString(len)
				val k = i.readU8()
				if (k == 0x09) break
				h[name] = readWithCode(k)
			}
			return h
		}

		fun readWithCode(id: Int): Any? = when (id) {
			0x00 -> i.readF64BE()
			0x01 -> when (i.readU8()) {
				0 -> false
				1 -> true
				else -> error("Invalid AMF")
			}
			0x02 -> i.readStringz(i.readU16BE())
			0x03 -> readObject()
			0x08 -> {
				var size = i.readS32BE()
				TODO()
				readObject()
			}
			0x05 -> null
			0x06 -> Undefined
			0x07 -> error("Not supported : Reference")
			0x0A -> {
				val count = i.readS32BE()
				(0 until count).map { read() }
			}
			0x0B -> {
				val time_ms = i.readF64BE()
				val tz_min = i.readU16BE()
				DateTime(time_ms.toLong() + tz_min * 60 * 1000L)
			}
			0x0C -> i.readString(i.readS32BE())
			else -> error("Unknown AMF $id")
		}

		fun read(): Any? = readWithCode(i.readU8())
	}
}

