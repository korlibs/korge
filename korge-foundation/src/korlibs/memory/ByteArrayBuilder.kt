package korlibs.memory

import korlibs.number.Half

/**
 * Analogous to [StringBuilder] but for [ByteArray]. Allows to [append] values to end calling [toByteArray].
 * Provides some methods like [s16LE] or [f32BE] to append specific bit representations easily.
 */
fun ByteArrayBuilder.f16(v: Half, little: Boolean): ByteArrayBuilder = this.apply { prepare(2) { data.setF16(_size, v, little) } }
fun ByteArrayBuilder.f16LE(v: Half): ByteArrayBuilder = this.apply { prepare(2) { data.setF16LE(_size, v) } }
fun ByteArrayBuilder.f16BE(v: Half): ByteArrayBuilder = this.apply { prepare(2) { data.setF16BE(_size, v) } }

fun ByteArrayBuilderLE.f16(v: Half): ByteArrayBuilder = bab.f16LE(v)
fun ByteArrayBuilderBE.f16(v: Half): ByteArrayBuilder = bab.f16BE(v)
