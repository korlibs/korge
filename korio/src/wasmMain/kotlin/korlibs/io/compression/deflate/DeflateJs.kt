package korlibs.io.compression.deflate

import korlibs.io.compression.*

actual fun Deflate(windowBits: Int): CompressionMethod = DeflatePortable(windowBits)
