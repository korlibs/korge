package korlibs.memory

import korlibs.number.Half

@Deprecated("", ReplaceWith("getF16LE(o)"))
fun ByteArray.readF16LE(o: Int): Half = getF16LE(o)
@Deprecated("", ReplaceWith("getF16BE(o)"))
fun ByteArray.readF16BE(o: Int): Half = getF16BE(o)
@Deprecated("", ReplaceWith("getF16(o, little)"))
fun ByteArray.readF16(o: Int, little: Boolean): Half = getF16(o, little)
@Deprecated("", ReplaceWith("setF16(o, v, little)"))
fun ByteArray.writeF16(o: Int, v: Half, little: Boolean) = setF16(o, v, little)
@Deprecated("", ReplaceWith("setF16LE(o, v)"))
fun ByteArray.writeF16LE(o: Int, v: Half) = setF16LE(o, v)
@Deprecated("", ReplaceWith("setF16BE(o, v)"))
fun ByteArray.writeF16BE(o: Int, v: Half) = setF16BE(o, v)
