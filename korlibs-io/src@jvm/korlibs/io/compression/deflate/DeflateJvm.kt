package korlibs.io.compression.deflate

import korlibs.io.compression.*
import korlibs.io.compression.util.*
import korlibs.io.experimental.*
import korlibs.io.stream.*
import java.util.zip.*

//actual fun Deflate(windowBits: Int): CompressionMethod = DeflatePortable(windowBits)
actual fun Deflate(windowBits: Int): CompressionMethod = DeflateNative(windowBits)

@OptIn(KorioExperimentalApi::class)
fun DeflateNative(windowBits: Int): CompressionMethod = object : CompressionMethod {
    override val name: String get() = "DEFLATE"

    override suspend fun uncompress(i: BitReader, o: AsyncOutputStream) {
		val tempInput = ByteArray(64 * 1024)
		var tempInputSize = 0
		val tempOutput = ByteArray(128 * 1024)
		val inflater = Inflater(true)
		try {
			do {
				if (inflater.needsInput()) {
					val read = i.read(tempInput, 0, tempInput.size)
					tempInputSize = read
					if (read <= 0) break
					inflater.setInput(tempInput, 0, read)
				}
				val written = inflater.inflate(tempOutput)
				if (written > 0) {
					o.write(tempOutput, 0, written)
				}
			} while (!inflater.finished())
		} finally {
			val remaining = inflater.remaining
			//println("REMAINING: tempInputSize=$tempInputSize, remaining=$remaining")
			i.returnToBuffer(tempInput, tempInputSize - remaining, remaining)
			inflater.end()
		}
	}

	override suspend fun compress(i: BitReader, o: AsyncOutputStream, context: CompressionContext) {
		val tempInput = ByteArray(1024)
		val tempOutput = ByteArray(1024)
		val deflater = Deflater(context.level, true)
		try {
			do {
				//println("DEFLATER")
				if (deflater.needsInput()) {
					val read = i.read(tempInput, 0, tempInput.size)
					if (read <= 0) {
						deflater.finish()
					} else {
						deflater.setInput(tempInput, 0, read)
					}
				}
				val written = deflater.deflate(tempOutput)
				if (written > 0) {
					o.write(tempOutput, 0, written)
				}
			} while (!deflater.finished())
		} finally {
			deflater.end()
		}
	}
}
