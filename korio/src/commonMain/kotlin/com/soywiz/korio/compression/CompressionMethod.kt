package com.soywiz.korio.compression

import com.soywiz.korio.async.*
import com.soywiz.korio.compression.util.*
import com.soywiz.korio.experimental.*
import com.soywiz.korio.lang.*
import com.soywiz.korio.stream.*

open class CompressionContext(var level: Int = 6) {
	var name: String? = null
	var custom: Any? = null
}

interface CompressionMethod {
	@KorioExperimentalApi
	suspend fun uncompress(reader: BitReader, out: AsyncOutputStream): Unit = unsupported()

	@KorioExperimentalApi
	suspend fun compress(
		i: BitReader,
		o: AsyncOutputStream,
		context: CompressionContext = CompressionContext()
	): Unit = unsupported()

	object Uncompressed : CompressionMethod {
		@OptIn(KorioExperimentalApi::class)
		override suspend fun uncompress(reader: BitReader, out: AsyncOutputStream): Unit { reader.copyTo(out) }
		@OptIn(KorioExperimentalApi::class)
		override suspend fun compress(i: BitReader, o: AsyncOutputStream, context: CompressionContext): Unit { i.copyTo(o) }
	}
}

@OptIn(KorioExperimentalApi::class)
suspend fun CompressionMethod.uncompress(i: AsyncInputStream, o: AsyncOutputStream): Unit = uncompress(BitReader.forInput(i), o)
@OptIn(KorioExperimentalApi::class)
suspend fun CompressionMethod.compress(i: AsyncInputStream, o: AsyncOutputStream, context: CompressionContext = CompressionContext()): Unit = compress(BitReader.forInput(i), o, context)

suspend fun CompressionMethod.uncompressStream(input: AsyncInputStream, bufferSize: Int = AsyncByteArrayDequeChunked.DEFAULT_MAX_SIZE): AsyncInputStream =
    asyncStreamWriter(bufferSize, name = "uncompress:$this") { output -> uncompress(input, output) }
suspend fun CompressionMethod.compressStream(
    input: AsyncInputStream,
    context: CompressionContext = CompressionContext(),
    bufferSize: Int = AsyncByteArrayDequeChunked.DEFAULT_MAX_SIZE
): AsyncInputStream = asyncStreamWriter(bufferSize, name = "compress:$this") { output -> compress(input, output, context) }

fun CompressionMethod.uncompress(i: SyncInputStream, o: SyncOutputStream) = runBlockingNoSuspensions {
	uncompress(i.toAsyncInputStream(), o.toAsyncOutputStream())
}

fun CompressionMethod.compress(i: SyncInputStream, o: SyncOutputStream, context: CompressionContext = CompressionContext()) = runBlockingNoSuspensions {
	compress(i.toAsyncInputStream(), o.toAsyncOutputStream(), context)
}

fun ByteArray.uncompress(method: CompressionMethod, outputSizeHint: Int = this.size * 2): ByteArray = MemorySyncStreamToByteArray(outputSizeHint) { method.uncompress(this@uncompress.openSync(), this) }
fun ByteArray.compress(method: CompressionMethod, context: CompressionContext = CompressionContext(), outputSizeHint: Int = (this.size * 1.1).toInt()): ByteArray =
	MemorySyncStreamToByteArray(outputSizeHint) { method.compress(this@compress.openSync(), this, context) }

suspend fun AsyncInputStream.uncompressed(method: CompressionMethod, bufferSize: Int = AsyncByteArrayDequeChunked.DEFAULT_MAX_SIZE): AsyncInputStream = method.uncompressStream(this, bufferSize)
suspend fun AsyncInputStream.compressed(method: CompressionMethod, context: CompressionContext = CompressionContext(), bufferSize: Int = AsyncByteArrayDequeChunked.DEFAULT_MAX_SIZE): AsyncInputStream = method.compressStream(this, context, bufferSize)
