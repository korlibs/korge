package com.soywiz.korio.compression.deflate

import com.soywiz.korio.compression.*

actual fun Deflate(windowBits: Int): CompressionMethod = DeflatePortable(windowBits)
