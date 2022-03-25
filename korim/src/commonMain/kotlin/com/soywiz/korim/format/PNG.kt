package com.soywiz.korim.format

import com.soywiz.kmem.*
import com.soywiz.korim.bitmap.*
import com.soywiz.korim.color.*
import com.soywiz.korim.internal.*
import com.soywiz.korio.async.*

import com.soywiz.korio.compression.*
import com.soywiz.korio.compression.deflate.*
import com.soywiz.korio.util.checksum.*
import com.soywiz.korio.lang.*
import com.soywiz.korio.stream.*
import com.soywiz.krypto.encoding.*
import kotlin.math.*

@Suppress("MemberVisibilityCanBePrivate")
object PNG : ImageFormat("png") {
	const val MAGIC1 = 0x89504E47.toInt()
	const val MAGIC2 = 0x0D0A1A0A

	data class InterlacedPass(
		val startingRow: Int, val startingCol: Int,
		val rowIncrement: Int, val colIncrement: Int,
		val blockHeight: Int, val blockWidth: Int
	) {
		val colIncrementShift = log2(colIncrement.toDouble()).toInt()
	}

	val InterlacedPasses = listOf(
		InterlacedPass(0, 0, 8, 8, 8, 8),
		InterlacedPass(0, 4, 8, 8, 8, 4),
		InterlacedPass(4, 0, 8, 4, 4, 4),
		InterlacedPass(0, 2, 4, 4, 4, 2),
		InterlacedPass(2, 0, 4, 2, 2, 2),
		InterlacedPass(0, 1, 2, 2, 2, 1),
		InterlacedPass(1, 0, 2, 1, 1, 1)
	)

	val NormalPasses = listOf(
		InterlacedPass(0, 0, 1, 1, 1, 1)
	)

	enum class Colorspace(val id: Int, val nchannels: Int) {
		GRAYSCALE(0, 1),
		RGB(2, 3),
		INDEXED(3, 1),
		GRAYSCALE_ALPHA(4, 2),
		RGBA(6, 4);

		companion object {
			val BY_ID = values().associateBy { it.id }
		}
	}

	class Header(
		val width: Int,
		val height: Int,
		val bitsPerChannel: Int,
		val colorspace: Colorspace, // 0=grayscale, 2=RGB, 3=Indexed, 4=grayscale+alpha, 6=RGBA
		@Suppress("unused") val compressionmethod: Int, // 0
		@Suppress("unused") val filtermethod: Int,
		val interlacemethod: Int
	) {
		val components = when (colorspace) {
			Colorspace.GRAYSCALE -> 1
			Colorspace.INDEXED -> 1
			Colorspace.GRAYSCALE_ALPHA -> 2
			Colorspace.RGB -> 3
			Colorspace.RGBA -> 4
		}
		val stride = (width * components * bitsPerChannel) / 8
	}

	override fun decodeHeader(s: SyncStream, props: ImageDecodingProps): ImageInfo? = try {
        val info = ImageInfo()
		val header = readCommon(s, readHeader = true, info = info) as Header
        info
	} catch (t: Throwable) {
		null
	}

	override fun writeImage(image: ImageData, s: SyncStream, props: ImageEncodingProps) {
		val bitmap = image.mainBitmap
		val width = bitmap.width
		val height = bitmap.height
		s.write32BE(MAGIC1)
		s.write32BE(MAGIC2)

		fun writeChunk(name: String, data: ByteArray) {
			val nameBytes = name.toByteArray().copyOf(4)

			var crc = CRC32.initialValue
			crc = CRC32.update(crc, nameBytes)
			crc = CRC32.update(crc, data)

			s.write32BE(data.size)
			s.writeBytes(nameBytes)
			s.writeBytes(data)
			s.write32BE(crc) // crc32!
		}

		val level = props.quality.convertRangeClamped(0.0, 1.0, 0.0, 9.0).toInt()

		fun compress(data: ByteArray): ByteArray {
			return data.compress(ZLib, CompressionContext(level = level))
		}

		fun writeChunk(name: String, initialCapacity: Int = 4096, callback: SyncStream.() -> Unit) {
			return writeChunk(name, MemorySyncStreamToByteArray(initialCapacity) { callback() })
		}

		@Suppress("unused")
		fun writeChunkCompressed(name: String, initialCapacity: Int = 4096, callback: SyncStream.() -> Unit) {
			return writeChunk(name, compress(MemorySyncStreamToByteArray(initialCapacity) { callback() }))
		}

		fun writeHeader(colorspace: Colorspace) {
			writeChunk("IHDR", initialCapacity = 13) {
				write32BE(width)
				write32BE(height)
				write8(8) // bits
				write8(colorspace.id) // colorspace
				write8(0) // compressionmethod
				write8(0) // filtermethod
				write8(0) // interlacemethod
			}
		}

		when (bitmap) {
			is Bitmap8 -> {
				writeHeader(Colorspace.INDEXED)
				writeChunk("PLTE", initialCapacity = bitmap.palette.size * 3) {
					for (c in bitmap.palette) {
						write8(c.r)
						write8(c.g)
						write8(c.b)
					}
				}
				writeChunk("tRNS", initialCapacity = bitmap.palette.size * 1) {
					for (c in bitmap.palette) {
						write8(c.a)
					}
				}

				val out = ByteArray(height + width * height)
				var pos = 0
				for (y in 0 until height) {
					out.write8(pos++, 0)
					val index = bitmap.index(0, y)
					arraycopy(bitmap.data, index, out, pos, width)
					pos += width
				}
				writeChunk("IDAT", compress(out))
			}
			else -> {
                val bmp = bitmap.toBMP32()
				writeHeader(Colorspace.RGBA)

				val out = ByteArray(height + width * height * 4)
				var pos = 0
				for (y in 0 until height) {
					out.write8(pos++, 0) // no filter
					val index = bmp.index(0, y)
                    for (x in 0 until width) {
                        val c = if (bmp.premultiplied) bmp.data[index + x].asPremultiplied().depremultiplied else bmp.data[index + x]
                        out.write8(pos++, c.r)
                        out.write8(pos++, c.g)
                        out.write8(pos++, c.b)
                        out.write8(pos++, c.a)
                    }
				}

				writeChunk("IDAT", compress(out))
			}
		}

		writeChunk("IEND", initialCapacity = 0) {
		}
	}

	private fun readCommon(s: SyncStream, readHeader: Boolean, info: ImageInfo = ImageInfo()): Any? {
		val magic = s.readS32BE()
		if (magic != MAGIC1) throw IllegalArgumentException("Invalid PNG file magic: ${magic.hex}!=${MAGIC1.hex}")
		s.readS32BE() // magic continuation

		var pheader: Header? = null
		val pngdata = ByteArrayBuilder()
		val rgbPalette = UByteArrayInt(3 * 0x100)
		val aPalette = UByteArrayInt(ByteArray(0x100) { -1 })
		var paletteCount = 0

		fun SyncStream.readChunk(): Boolean {
            if (eof) return false
			val length = readS32BE()
			val type = readStringz(4)
			val data = readStream(length.toLong())
			@Suppress("UNUSED_VARIABLE")
			val crc = readS32BE()

			when (type) {
				"IHDR" -> {
					pheader = data.run {
						Header(
							width = readS32BE(),
							height = readS32BE(),
							bitsPerChannel = readU8(),
							colorspace = Colorspace.BY_ID[readU8()] ?: Colorspace.RGBA,
							compressionmethod = readU8(),
							filtermethod = readU8(),
							interlacemethod = readU8()
						).also {
                            info.width = it.width
                            info.height = it.height
                            info.bitsPerPixel = it.bitsPerChannel * it.colorspace.nchannels
                        }
					}
				}
				"PLTE" -> {
					paletteCount = max(paletteCount, data.length.toInt() / 3)
					data.read(rgbPalette.asByteArray(), 0, data.length.toInt())
				}
				"tRNS" -> {
					paletteCount = max(paletteCount, data.length.toInt())
					data.read(aPalette.asByteArray(), 0, data.length.toInt())
				}
				"IDAT" -> {
                    if (readHeader) {
                        return false
                    }
					pngdata.append(data.readAll())
				}
                "eXIf" -> {
                    runBlockingNoSuspensions { EXIF.readExifBase(data.readAll().openAsync(), info) }
                }
				"IEND" -> {
                    return false
				}
			}
			//println(type)
            return true
		}

		while (!s.eof && s.readChunk()) Unit
        if (readHeader) return pheader

		val header = pheader ?: throw IllegalArgumentException("PNG without header!")
		val width = header.width
		val height = header.height

		//val databb = ByteArrayBuffer((1 + width) * height * header.bytes)

        val outputSizeHint = (1 + width) * height * header.components
		val databb = pngdata.toByteArray().uncompress(ZLib)

		//method.syncUncompress(pngdata.toByteArray().openSync(), MemorySyncStreamBase(databb).toSyncStream(0L))
		var databbp = 0

		val context = DecodingContext(header)
		val bpp = context.header.components
		val row32 = context.row32

		val bmp = when {
			header.components == 1 -> Bitmap8(
				width,
				height,
				palette = RgbaArray((0 until paletteCount).map {
					RGBA(
						rgbPalette[it * 3 + 0],
						rgbPalette[it * 3 + 1],
						rgbPalette[it * 3 + 2],
						aPalette[it]
					).value
				}.toIntArray())
			)
			else -> Bitmap32(width, height)
		}
		val bmp8 = bmp as? Bitmap8?
		val bmp32 = bmp as? Bitmap32?
		val passes = when (header.interlacemethod) {
			1 -> InterlacedPasses
			else -> NormalPasses
		}

		for (pass in passes) {
			for (row in pass.startingRow until height step pass.rowIncrement) {
				val col = pass.startingCol
				val colIncrement = pass.colIncrement
				val pixelsInThisRow = width ushr pass.colIncrementShift
				val bytesInThisRow = (pixelsInThisRow * header.components * header.bitsPerChannel / 8)
				val filter = databb[databbp++].toInt() and 0xFF
				val currentRow = context.currentRow
				val lastRow = context.lastRow
				arraycopy(databb, databbp, currentRow.asByteArray(), 0, bytesInThisRow)
				databbp += bytesInThisRow
				when {
					bmp8 != null -> {
						applyFilter(filter, lastRow, currentRow, header.components)
                        when (header.bitsPerChannel) {
                            1 -> for (n in 0 until width) bmp[col + n, row] = currentRow[n / 8].extract(7 - (n % 8) * 1, 1)
                            2 -> for (n in 0 until width) bmp[col + n, row] = currentRow[n / 4].extract(6 - (n % 4) * 2, 2)
                            4 -> for (n in 0 until width) bmp[col + n, row] = currentRow[n / 2].extract(4 - (n % 2) * 4, 4)
                            8 -> bmp8.setRowChunk(col, row, currentRow.asByteArray(), width, colIncrement)
                            else -> error("Unsupported header.bitsPerChannel=${header.bitsPerChannel}")
                        }
					}
					bmp32 != null -> {
                        when (header.bitsPerChannel) {
                            8 -> {
                                applyFilter(filter, lastRow, currentRow, bpp, bytesInThisRow)
                                when (bpp) {
                                    3 -> RGB.decode(currentRow.asByteArray(), 0, row32, 0, pixelsInThisRow)
                                    4 -> RGBA.decode(currentRow.asByteArray(), 0, row32, 0, pixelsInThisRow)
                                    else -> TODO("Bytes: $bpp")
                                }
                                bmp32.setRowChunk(col, row, row32, width, colIncrement)
                            }
                            else -> error("Unsupported header.bitsPerChannel=${header.bitsPerChannel}")
                        }
					}
				}
				context.swapRows()
			}
		}

		return bmp
	}

	class DecodingContext(val header: Header) {
		var lastRow = UByteArrayInt(header.stride)
		var currentRow = UByteArrayInt(header.stride)
		val row32 = RgbaArray(header.width)

		fun swapRows() {
			val temp = currentRow
			currentRow = lastRow
			lastRow = temp
		}
	}

	override fun readImage(s: SyncStream, props: ImageDecodingProps): ImageData =
		ImageData(readCommon(s, readHeader = false) as Bitmap)

	fun paethPredictor(a: Int, b: Int, c: Int): Int {
		val p = a + b - c
		val pa = abs(p - a)
		val pb = abs(p - b)
		val pc = abs(p - c)
		return if ((pa <= pb) && (pa <= pc)) a else if (pb <= pc) b else c
	}

	fun applyFilter(filter: Int, p: UByteArrayInt, c: UByteArrayInt, bpp: Int, size: Int = c.size) {
		when (filter) {
			0 -> Unit
			1 -> for (n in bpp until size) c[n] += c[n - bpp]
			2 -> for (n in 0 until size) c[n] += p[n]
			3 -> {
				for (n in 0 until bpp) c[n] += p[n] / 2
				for (n in bpp until size) c[n] += (c[n - bpp] + p[n]) / 2
			}
			4 -> {
				for (n in 0 until bpp) c[n] += p[n]
				for (n in bpp until size) c[n] += paethPredictor(c[n - bpp], p[n], p[n - bpp])
			}
			else -> TODO("Filter: $filter")
		}
	}
}
