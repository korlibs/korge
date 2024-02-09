package korlibs.memory

import korlibs.number.Half


fun ByteArrayReader.f16(little: Boolean): Half = move(2) { getF16(it, little) }
fun ByteArrayReader.f16LE(): Half = move(2) { getF16LE(it) }
fun ByteArrayReader.f16BE(): Half = move(2) { getF16BE(it) }
fun ByteArrayReaderLE.f16(): Half = bar.f16LE()
fun ByteArrayReaderBE.f16(): Half = bar.f16BE()
