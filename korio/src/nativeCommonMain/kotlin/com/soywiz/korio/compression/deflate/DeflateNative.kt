package com.soywiz.korio.compression.deflate

import kotlinx.cinterop.*
import platform.posix.*
import platform.zlib.*
import kotlin.math.*
import com.soywiz.kmem.*
import com.soywiz.kmem.*
import com.soywiz.korio.compression.*
import com.soywiz.korio.compression.util.*
import com.soywiz.korio.stream.*
import com.soywiz.korio.experimental.*
import kotlin.math.*

//actual fun Deflate(windowBits: Int): CompressionMethod = DeflatePortable(windowBits)
actual fun Deflate(windowBits: Int): CompressionMethod = DeflateNative(windowBits)

private const val CHUNK = 64 * 1024

@UseExperimental(KorioExperimentalApi::class)
fun DeflateNative(windowBits: Int): CompressionMethod = object : CompressionMethod {
	override suspend fun uncompress(input: BitReader, output: AsyncOutputStream) {
		memScoped {
			val strm: z_stream = alloc()

			var ret: Int

			val inpArray = ByteArray(CHUNK)
			val outArray = ByteArray(CHUNK)

			try {
				inpArray.usePinned { _inp ->
					outArray.usePinned { _out ->
						val inp = _inp.addressOf(0)
						val out = _out.addressOf(0)
						var tempInputSize = 0
						strm.zalloc = null
						strm.zfree = null
						strm.opaque = null
						strm.avail_in = 0u
						strm.next_in = null
						ret = inflateInit2_(strm.ptr, -windowBits, zlibVersion()?.toKString(), sizeOf<z_stream>().toInt());
						if (ret != Z_OK) error("Invalid inflateInit2_")

						do {
							//println("strm.avail_in: ${strm.avail_in}")
							strm.avail_in = input.read(inpArray, 0, CHUNK).convert()
							if (strm.avail_in == 0u || strm.avail_in > CHUNK.convert()) break
							tempInputSize = strm.avail_in.convert()
							strm.next_in = inp.reinterpret()

							do {
								strm.avail_out = CHUNK.convert()
								strm.next_out = out.reinterpret()
								ret = inflate(strm.ptr, Z_NO_FLUSH)
								assert(ret != Z_STREAM_ERROR)
								when (ret) {
									Z_NEED_DICT -> ret = Z_DATA_ERROR
									Z_DATA_ERROR -> error("data error")
									Z_MEM_ERROR  -> error("mem error")
								}
								val have = CHUNK - strm.avail_out.toInt()
								output.write(outArray, 0, have)
							} while (strm.avail_out == 0u)
						} while (ret != Z_STREAM_END)

						// Return read bytes that were not consumed
						val remaining = strm.avail_in.toInt()
						if (remaining > 0) {
							input.returnToBuffer(inpArray, tempInputSize - remaining, remaining)
							//error("too much data in DeflateNative stream")
						}
					}
				}
			} finally {
				inflateEnd(strm.ptr)
			}
		}
	}

	override suspend fun compress(
		input: BitReader,
		output: AsyncOutputStream,
		context: CompressionContext
	) {
		memScoped {
			val strm: z_stream = alloc()

			var ret: Int

			val inpArray = ByteArray(CHUNK)
			val outArray = ByteArray(CHUNK)

			try {
				inpArray.usePinned { _inp ->
					outArray.usePinned { _out ->
						val inp = _inp.addressOf(0)
						val out = _out.addressOf(0)
						strm.zalloc = null
						strm.zfree = null
						strm.opaque = null
						strm.avail_in = 0u
						strm.next_in = null
						val Z_DEFLATED = 8
						val MAX_MEM_LEVEL = 9
						val Z_DEFAULT_STRATEGY = 0
						ret = deflateInit2_(strm.ptr, context.level, Z_DEFLATED, -windowBits, MAX_MEM_LEVEL, Z_DEFAULT_STRATEGY, zlibVersion()?.toKString(), sizeOf<z_stream>().toInt());
						if (ret != Z_OK) error("Invalid deflateInit2_")

						var finalize = false
						do {
							val read = input.read(inpArray, 0, CHUNK)
							if (read <= 0 || read > CHUNK) {
								finalize = true
							}
							strm.avail_in = read.convert()
							strm.next_in = inp.reinterpret()

							do {
								strm.avail_out = CHUNK.convert()
								strm.next_out = out.reinterpret()
								ret = deflate(strm.ptr, if (finalize) Z_FINISH else Z_NO_FLUSH)
								assert(ret != Z_STREAM_ERROR)
								when (ret) {
									Z_NEED_DICT -> ret = Z_DATA_ERROR
									Z_DATA_ERROR -> error("data error")
									Z_MEM_ERROR  -> error("mem error")
								}
								val have = CHUNK - strm.avail_out.toInt()
								output.write(outArray, 0, have)
							} while (strm.avail_out == 0u)

							assert(strm.avail_in == 0u)

						} while (!finalize && ret != Z_STREAM_END)
					}
				}
			} finally {
				deflateEnd(strm.ptr)
			}
		}
	}
}
