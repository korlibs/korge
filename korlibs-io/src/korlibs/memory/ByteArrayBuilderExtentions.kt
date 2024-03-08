package korlibs.memory

import korlibs.number.*

/**
 * Analogous to [StringBuilder] but for [ByteArray]. Allows to [append] values to end calling [toByteArray].
 * Provides some methods like [s16LE] or [f32BE] to append specific bit representations easily.
 */
fun ByteArrayBuilder.f16(v: Half, little: Boolean): ByteArrayBuilder = s16(v.rawBits.toInt(), little)
fun ByteArrayBuilder.f16LE(v: Half): ByteArrayBuilder = s16LE(v.rawBits.toInt())
fun ByteArrayBuilder.f16BE(v: Half): ByteArrayBuilder = s16BE(v.rawBits.toInt())

fun ByteArrayBuilderLE.f16(v: Half): ByteArrayBuilder = bab.f16LE(v)
fun ByteArrayBuilderBE.f16(v: Half): ByteArrayBuilder = bab.f16BE(v)
