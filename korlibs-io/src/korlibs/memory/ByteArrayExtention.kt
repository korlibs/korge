package korlibs.memory

import korlibs.number.Half

private fun ByteArray.u8(offset: Int): Int = this[offset].toInt() and 0xFF
private inline fun ByteArray.get16LE(offset: Int): Int = (u8(offset + 0) shl 0) or (u8(offset + 1) shl 8)
private inline fun ByteArray.get16BE(offset: Int): Int = (u8(offset + 1) shl 0) or (u8(offset + 0) shl 8)

// Signed
fun ByteArray.getF16LE(offset: Int): Half = Half.fromBits(get16LE(offset))
fun ByteArray.getF16BE(offset: Int): Half = Half.fromBits(get16BE(offset))

// Custom Endian
fun ByteArray.getF16(offset: Int, littleEndian: Boolean): Half = if (littleEndian) getF16LE(offset) else getF16BE(offset)
fun ByteArray.setF16(offset: Int, value: Half, littleEndian: Boolean) { if (littleEndian) setF16LE(offset, value) else setF16BE(offset, value) }
fun ByteArray.setF16LE(offset: Int, value: Half) { set16LE(offset + 0, value.toRawBits().toInt()) }
fun ByteArray.setF16BE(offset: Int, value: Half) { set16BE(offset + 0, value.toRawBits().toInt()) }
