package korlibs.io.compression

import korlibs.datastructure.*
import korlibs.io.async.*
import korlibs.io.compression.util.BitReader
import korlibs.io.experimental.KorioExperimentalApi
import korlibs.io.lang.unsupported
import korlibs.io.stream.AsyncInputStream
import korlibs.io.stream.AsyncOutputStream
import korlibs.io.stream.MemorySyncStreamToByteArray
import korlibs.io.stream.SyncInputStream
import korlibs.io.stream.SyncOutputStream
import korlibs.io.stream.asyncStreamWriter
import korlibs.io.stream.openSync
import korlibs.io.stream.toAsyncInputStream
import korlibs.io.stream.toAsyncOutputStream

open class CompressionContext(var level: Int = 6) {
	var name: String? = null
	var custom: Any? = null
}

data class CompressionMethodWithConfig(val method: CompressionMethod, override val level: Int) : CompressionMethod by method, Extra by Extra.Mixin()

fun CompressionMethod.withLevel(level: Int): CompressionMethodWithConfig = CompressionMethodWithConfig(this, level)

interface CompressionMethod {
    val name: String get() = "UNKNOWN"

    val level: Int get() = 6

	@KorioExperimentalApi
	suspend fun uncompress(reader: BitReader, out: AsyncOutputStream): Unit = unsupported()

	@KorioExperimentalApi
	suspend fun compress(
		i: BitReader,
		o: AsyncOutputStream,
		context: CompressionContext = CompressionContext(level = this.level)
	): Unit = unsupported()

	object Uncompressed : CompressionMethod {
        override val name: String get() = "STORE"

		@OptIn(KorioExperimentalApi::class)
		override suspend fun uncompress(reader: BitReader, out: AsyncOutputStream) { reader.copyTo(out) }
		@OptIn(KorioExperimentalApi::class)
		override suspend fun compress(i: BitReader, o: AsyncOutputStream, context: CompressionContext) { i.copyTo(o) }
	}
}

@OptIn(KorioExperimentalApi::class)
suspend fun CompressionMethod.uncompress(i: AsyncInputStream, o: AsyncOutputStream): Unit = uncompress(BitReader.forInput(i), o)
@OptIn(KorioExperimentalApi::class)
suspend fun CompressionMethod.compress(i: AsyncInputStream, o: AsyncOutputStream, context: CompressionContext = CompressionContext(level = this.level)): Unit = compress(BitReader.forInput(i), o, context)

suspend fun CompressionMethod.uncompressStream(input: AsyncInputStream, bufferSize: Int = AsyncRingBufferChunked.DEFAULT_MAX_SIZE): AsyncInputStream =
    asyncStreamWriter(bufferSize, name = "uncompress:$this") { output -> uncompress(input, output) }
suspend fun CompressionMethod.compressStream(
    input: AsyncInputStream,
    context: CompressionContext = CompressionContext(),
    bufferSize: Int = AsyncRingBufferChunked.DEFAULT_MAX_SIZE
): AsyncInputStream = asyncStreamWriter(bufferSize, name = "compress:$this") { output -> compress(input, output, context) }

fun CompressionMethod.uncompress(i: SyncInputStream, o: SyncOutputStream) = runBlockingNoSuspensions {
	uncompress(i.toAsyncInputStream(), o.toAsyncOutputStream())
}

fun CompressionMethod.compress(i: SyncInputStream, o: SyncOutputStream, context: CompressionContext = CompressionContext(level = this.level)) = runBlockingNoSuspensions {
	compress(i.toAsyncInputStream(), o.toAsyncOutputStream(), context)
}

fun CompressionMethod.compress(bytes: ByteArray, context: CompressionContext = CompressionContext(level = this.level), outputSizeHint: Int = (bytes.size * 1.1).toInt()): ByteArray =
    MemorySyncStreamToByteArray(outputSizeHint) { this@compress.compress(bytes.openSync(), this, context) }
fun CompressionMethod.uncompress(bytes: ByteArray, outputSizeHint: Int = bytes.size * 2): ByteArray =
    MemorySyncStreamToByteArray(outputSizeHint) { this@uncompress.uncompress(bytes.openSync(), this) }

fun ByteArray.uncompress(method: CompressionMethod, outputSizeHint: Int = this.size * 2): ByteArray =
    method.uncompress(this, outputSizeHint)
fun ByteArray.compress(method: CompressionMethod, context: CompressionContext = CompressionContext(level = method.level), outputSizeHint: Int = (this.size * 1.1).toInt()): ByteArray =
	method.compress(this, context, outputSizeHint)

suspend fun AsyncInputStream.uncompressed(method: CompressionMethod, bufferSize: Int = AsyncRingBufferChunked.DEFAULT_MAX_SIZE): AsyncInputStream = method.uncompressStream(this, bufferSize)
suspend fun AsyncInputStream.compressed(method: CompressionMethod, context: CompressionContext = CompressionContext(level = method.level), bufferSize: Int = AsyncByteArrayDequeChunked.DEFAULT_MAX_SIZE): AsyncInputStream = method.compressStream(this, context, bufferSize)
