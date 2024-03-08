package korlibs.memory

import korlibs.number.*


fun ByteArrayReader.f16(little: Boolean): Half = Half.fromBits(s16(little))
fun ByteArrayReader.f16LE(): Half = Half.fromBits(s16LE())
fun ByteArrayReader.f16BE(): Half = Half.fromBits(s16BE())
fun ByteArrayReaderLE.f16(): Half = bar.f16LE()
fun ByteArrayReaderBE.f16(): Half = bar.f16BE()
