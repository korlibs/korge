package korlibs.io.compression.deflate

import korlibs.memory.FixedSizeByteArrayBuilder
import korlibs.math.unsigned
import korlibs.io.compression.CompressionContext
import korlibs.io.compression.CompressionMethod
import korlibs.io.compression.util.BitReader
import korlibs.io.compression.util.HuffmanTree
import korlibs.io.compression.util.SlidingWindow
import korlibs.io.experimental.KorioExperimentalApi
import korlibs.io.stream.AsyncOutputStream
import korlibs.io.stream.getAvailable
import korlibs.io.stream.hasAvailable
import korlibs.io.stream.readBytesExact
import korlibs.io.stream.write16LE
import korlibs.io.stream.write8
import korlibs.io.stream.writeBytes
import kotlin.math.min

expect fun Deflate(windowBits: Int): CompressionMethod

val Deflate: CompressionMethod by lazy { Deflate(15) }

@OptIn(KorioExperimentalApi::class)
open class DeflatePortable(val windowBits: Int) : CompressionMethod {
    override val name: String get() = "DEFLATE"

    override suspend fun compress(
		i: BitReader,
		o: AsyncOutputStream,
		context: CompressionContext
	) {
		while (i.hasAvailable()) {
			val available = i.getAvailable()
			val chunkSize = min(available, 0xFFFFL).toInt()
			o.write8(if (chunkSize >= available) 1 else 0)
			o.write16LE(chunkSize)
			o.write16LE(chunkSize.inv())
			//for (n in 0 until chunkSize) o.write8(i.readU8())
			o.writeBytes(i.readBytesExact(chunkSize))
		}
	}

	override suspend fun uncompress(reader: BitReader, out: AsyncOutputStream) {
        //println("reader.bigChunkSize=${reader.bigChunkSize}, reader.readWithSize=${reader.readWithSize}")
		val sout = SlidingWindowWithOutput(SlidingWindow(windowBits), out, reader.bigChunkSize, reader.readWithSize)
		var lastBlock = false
		val tempTree = HuffmanTree()
		val tempDist = HuffmanTree()
		val codeLenCodeLen = IntArray(32)
		val lengths = IntArray(512)
		//println("uncompress[0]")
		while (!lastBlock) {
            sout.flushIfRequired()
			reader.prepareBigChunkIfRequired()
			//println("uncompress[1]")

			lastBlock = reader.sreadBit()
			val blockType = reader.readBits(2)
			if (blockType !in 0..2) error("invalid bit")

			//println("lastBlock=$lastBlock, btype=$blockType")

			if (blockType == 0) {
				//println("uncompress[2]")
				val len = reader.su16LE()
				val nlen = reader.su16LE()
				val nnlen = nlen.inv() and 0xFFFF
				if (len != nnlen) error("Invalid deflate stream: len($len) != ~nlen($nnlen) :: nlen=$nlen")
				val bytes = reader.abytes(len)
				sout.putOut(bytes, 0, len)
                sout.flushIfRequired()
			} else {
				//println("uncompress[3]")
				val tree: HuffmanTree
				val dist: HuffmanTree
                if (blockType == 1) {
                    tree = FIXED_TREE
                    dist = FIXED_DIST
                }
                else {
                    val hlit = reader.readBits(5) + 257
                    val hdist = reader.readBits(5) + 1
                    val hclen = reader.readBits(4) + 4
                    codeLenCodeLen.fill(0)
                    for (i in 0 until hclen) codeLenCodeLen[HCLENPOS[i]] = reader.readBits(3)
                    //console.info(codeLenCodeLen);
                    tempTree.setFromLengths(codeLenCodeLen)
                    val codeLen = tempTree
                    val hlithdist = hlit + hdist
                    var n = 0
                    lengths.fill(0)
                    while (n < hlithdist) {
                        val value = reader.read(codeLen)
                        if (value !in 0..18) error("Invalid")

                        val len = when (value) {
                            16 -> reader.readBits(2) + 3
                            17 -> reader.readBits(3) + 3
                            18 -> reader.readBits(7) + 11
                            else -> 1
                        }
                        val vv = when (value) {
                            16 -> lengths[n - 1]
                            17 -> 0
                            18 -> 0
                            else -> value
                        }

                        lengths.fill(vv, n, n + len)
                        n += len
                    }
                    tempTree.setFromLengths(lengths, 0, hlit)
                    tempDist.setFromLengths(lengths, hlit, hlithdist)
                    tree = tempTree
                    dist = tempDist
                }
				while (true) {
				    // @TODO: Read in parallel while decoding
					reader.prepareBigChunkIfRequired()
					val value = tree.read(reader)
					if (value == 256) break
					if (value < 256) {
						sout.putOut(value.toByte())
					} else {
						val zlenof = value - 257
						val lengthExtra = reader.readBits(LEN_EXTRA[zlenof])
						val distanceData = reader.read(dist)
						val distanceExtra = reader.readBits(DIST_EXTRA[distanceData])
						val distance = DIST_BASE[distanceData] + distanceExtra
						val length = LEN_BASE[zlenof] + lengthExtra
						sout.getPutCopyOut(distance, length)
					}
					sout.flushIfRequired()
				}
			}
		}
		//println("uncompress[4]")
		sout.flushIfRequired(finish = true)
		//println("uncompress[5]")
	}

	private inline fun BitReader.read(tree: HuffmanTree): Int = tree.read(this)

	internal class SlidingWindowWithOutput(
        val sliding: SlidingWindow,
        val out: AsyncOutputStream,
        val flushSize: Int = FLUSH_SIZE,
        val extraSize: Int = EXTRA_SIZE
    ) {
        companion object {
            const val FLUSH_SIZE = 8 * 1024 * 1024
            const val EXTRA_SIZE = 128 * 1024
            //const val FLUSH_SIZE = 4 * 1024
            //const val EXTRA_SIZE = 4 * 1024
        }

		// @TODO: Optimize with buffering and copying
		val bab = FixedSizeByteArrayBuilder(flushSize + extraSize)
        //val bab = ByteArrayBuilder(flushSize + EXTRA_SIZE)

		val output get() = bab.size
		val mustFlush get() = bab.size >= flushSize

		fun getPutCopyOut(distance: Int, length: Int) {
			//print("LZ: distance=$distance, length=$length   :: ")
			for (n in 0 until length) {
				val v = sliding.getPut(distance)
				bab.appendFast(v.toByte())
				//print("$v,")
			}
			//println()
		}

		fun putOut(bytes: ByteArray, offset: Int, len: Int) {
			//print("BYTES: $len ::")
			bab.append(bytes, offset, len)
			sliding.putBytes(bytes, offset, len)
			//for (n in 0 until len) print("${bytes[offset + n].toUnsigned()},")
			//println()
		}

		fun putOut(byte: Byte) {
			//println("BYTE: $byte")
			bab.appendFast(byte)
			sliding.put(byte.unsigned)
		}

		suspend inline fun flush() {
            //print("FLUSH[$finish][${bab.size}]")
            //for (n in 0 until bab.size) print("${bab.data[n]},")
            //println()
            out.write(bab.data, 0, bab.size)
            bab.clear()
		}

		suspend inline fun flushIfRequired(finish: Boolean = false) {
			if (finish || mustFlush) flush()
		}
	}

	companion object : DeflatePortable(15) {
		private val FIXED_TREE: HuffmanTree = HuffmanTree().fromLengths(IntArray(288).apply {
			for (n in 0..143) this[n] = 8
			for (n in 144..255) this[n] = 9
			for (n in 256..279) this[n] = 7
			for (n in 280..287) this[n] = 8
		})
		private val FIXED_DIST: HuffmanTree = HuffmanTree().fromLengths(IntArray(32) { 5 })

		private val LEN_EXTRA = intArrayOf(
			0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 1, 1, 2, 2, 2, 2, 3, 3, 3, 3, 4, 4, 4, 4, 5, 5, 5, 5, 0, 0, 0
		)

		private val LEN_BASE = intArrayOf(
			3, 4, 5, 6, 7, 8, 9, 10, 11, 13,
			15, 17, 19, 23, 27, 31, 35, 43, 51, 59,
			67, 83, 99, 115, 131, 163, 195, 227, 258, 0, 0
		)

		private val DIST_EXTRA = intArrayOf(
			0, 0, 0, 0, 1, 1, 2, 2, 3, 3, 4, 4, 5, 5, 6, 6, 7, 7, 8, 8, 9, 9, 10, 10, 11, 11, 12, 12, 13, 13
		)

		private val DIST_BASE = intArrayOf(
			1, 2, 3, 4, 5, 7, 9, 13, 17, 25, 33, 49, 65, 97, 129, 193,
			257, 385, 513, 769, 1025, 1537, 2049, 3073, 4097, 6145, 8193, 12289, 16385, 24577, 0, 0
		)

		private val HCLENPOS = intArrayOf(16, 17, 18, 0, 8, 7, 9, 6, 10, 5, 11, 4, 12, 3, 13, 2, 14, 1, 15)
	}
}
