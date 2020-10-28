package com.soywiz.korio.util

import com.soywiz.kmem.*
import com.soywiz.korio.lang.*
import com.soywiz.krypto.encoding.*
import kotlin.random.*

@Suppress("EXPERIMENTAL_API_USAGE")
class UUID(val data: UByteArrayInt) {
	companion object {
		private val regex =
			Regex("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}", RegexOption.IGNORE_CASE)

		private fun fix(data: UByteArrayInt, version: Int, variant: Int): UByteArrayInt {
			data[6] = ((data[6] and 0b0000_1111) or (version shl 4))
			data[8] = ((data[8] and 0x00_111111) or (variant shl 6))
			return data
		}

		// @TODO: Use SecureRandom from Krypto
		fun randomUUID(random: Random = Random): UUID = UUID(fix(UByteArrayInt(16).apply {
			random.nextBytes(this.asByteArray())
		}, version = 4, variant = 1))

		operator fun invoke(str: String): UUID {
			if (regex.matchEntire(str) == null) invalidArg("Invalid UUID")
			return UUID(Hex.decode(str.replace("-", "")).asUByteArrayInt())
		}
	}

	val version: Int get() = (data[6] ushr 4) and 0b1111
	val variant: Int get() = (data[8] ushr 6) and 0b11

	override fun toString() = "%02x%02x%02x%02x-%02x%02x-%02x%02x-%02x%02x-%02x%02x%02x%02x%02x%02x".format(
		data[0], data[1], data[2], data[3], data[4], data[5], data[6], data[7],
		data[8], data[9], data[10], data[11], data[12], data[13], data[14], data[15]
	)
}
