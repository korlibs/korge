package korlibs.image.format

import korlibs.datastructure.*
import korlibs.image.bitmap.Bitmap
import korlibs.image.bitmap.Bitmap32
import korlibs.image.color.RGBA
import korlibs.image.color.RgbaArray
import korlibs.io.lang.LATIN1
import korlibs.io.stream.SyncStream
import korlibs.io.stream.readAvailable
import korlibs.io.stream.readS32BE
import korlibs.io.stream.readStringz
import korlibs.io.stream.readU8
import korlibs.io.stream.writeBytes
import korlibs.memory.*

// You may provide a pre-allocated array as an optimization for QOI encoding.
// Useful if you know you will be encoding a lot of times, so you can just re-use the same
// array to avoid allocating a new array for each computation.
// A requirement is that the provided array must be at least `QOI.calculateMaxSize` in size,
// for the bitmap that you'll be encoding.
var ImageEncodingProps.preAllocatedArrayForQOI: UByteArrayInt? by extraProperty { null }

object QOI : ImageFormat("qoi") {
    override fun decodeHeader(s: SyncStream, props: ImageDecodingProps): ImageInfo? {
        if (s.readStringz(4, LATIN1) != "qoif") return null
        val width = s.readS32BE()
        val height = s.readS32BE()
        val channels = s.readU8()
        val colorspace = s.readU8()
        return ImageInfo {
            this.width = width
            this.height = height
            this.bitsPerPixel = channels * 8
        }
    }

    /*
     * QOI supports outputting to the `out` bitmap, but certain conditions must be met.
     * 1. The width and height of the output Bitmap must match the header width and height.
     * 2. The out Bitmap must be a Bitmap32.
     */
    override fun readImage(s: SyncStream, props: ImageDecodingProps): ImageData {
        val header = decodeHeader(s, props)
            ?: error("Not a QOI image")
        val bytes = UByteArrayInt(s.readAvailable())
        val index = RgbaArray(64)
        val out = props.out
        val outBmp =
            if (out != null && out.width == header.width && out.height == header.height && out is Bitmap32) {
                out.premultiplied = false
                out
            } else {
                Bitmap32(header.width, header.height, premultiplied = false)
            }
        val outp = RgbaArray(outBmp.ints)
        val totalPixels = outBmp.area
        var o = 0
        var p = 0

        var r = 0
        var g = 0
        var b = 0
        var a = 0xFF
        var lastCol = RGBA(0, 0, 0, 0xFF)

        while (o < totalPixels && p < bytes.size) {
            val b1 = bytes[p++]

            when (b1) {
                QOI_OP_RGB -> {
                    r = bytes[p++]
                    g = bytes[p++]
                    b = bytes[p++]
                }

                QOI_OP_RGBA -> {
                    r = bytes[p++]
                    g = bytes[p++]
                    b = bytes[p++]
                    a = bytes[p++]
                }

                else -> {
                    when (b1.extract2(6)) {
                        QOI_SOP_INDEX -> {
                            val col = index[b1]
                            r = col.r
                            g = col.g
                            b = col.b
                            a = col.a
                        }

                        QOI_SOP_DIFF -> {
                            r = (r + (b1.extract2(4) - 2)) and 0xFF
                            g = (g + (b1.extract2(2) - 2)) and 0xFF
                            b = (b + (b1.extract2(0) - 2)) and 0xFF
                        }

                        QOI_SOP_LUMA -> {
                            val b2 = bytes[p++]
                            val vg = (b1.extract6(0)) - 32
                            r = (r + (vg - 8 + b2.extract4(4))) and 0xFF
                            g = (g + (vg)) and 0xFF
                            b = (b + (vg - 8 + b2.extract4(0))) and 0xFF
                        }

                        QOI_SOP_RUN -> {
                            val np = b1.extract6(0) + 1
                            for (n in 0 until np) outp[o++] = lastCol
                            continue
                        }
                    }
                }
            }

            lastCol = RGBA.packUnsafe(r, g, b, a)
            index[QOI_COLOR_HASH(r, g, b, a) % 64] = lastCol
            outp[o++] = lastCol
        }
        return ImageData(outBmp)
    }

    override fun writeImage(image: ImageData, s: SyncStream, props: ImageEncodingProps) {
        val bitmap = image.mainBitmap.toBMP32IfRequired()
        val pixels = RgbaArray(bitmap.ints)
        val index = RgbaArray(64)
        val maxSize = calculateMaxSize(bitmap)
        val bytes = if (props.preAllocatedArrayForQOI == null) {
            UByteArrayInt(maxSize)
        } else {
            require(props.preAllocatedArrayForQOI!!.size >= maxSize) {
                """
                   Requires a pre-allocated array of at least $maxSize bytes.
                   You provided a pre-allocated array with ${props.preAllocatedArrayForQOI!!.size} bytes.
                """.trimIndent()
            }
            props.preAllocatedArrayForQOI!!
        }
        val sbytes = bytes.bytes
        var currentIndex = 0
        var bytesUsed = 0

        bytes[bytesUsed++] = 'q'.code
        bytes[bytesUsed++] = 'o'.code
        bytes[bytesUsed++] = 'i'.code
        bytes[bytesUsed++] = 'f'.code
        sbytes.set32BE(bytesUsed, bitmap.width); bytesUsed += 4
        sbytes.set32BE(bytesUsed, bitmap.height); bytesUsed += 4
        bytes[bytesUsed++] = 4
        bytes[bytesUsed++] = QOI_LINEAR

        var previousPixel = RGBA(0, 0, 0, 0xFF)
        var previousR = 0
        var previousG = 0
        var previousB = 0
        var previousA = 0xFF

        var run = 0
        while (currentIndex < pixels.size) {
            val currentPixel = pixels[currentIndex++]
            val currentR = currentPixel.r
            val currentG = currentPixel.g
            val currentB = currentPixel.b
            val currentA = currentPixel.a

            if (currentPixel == previousPixel) {
                run++
                if (run == 62 || currentIndex >= pixels.size) {
                    bytes[bytesUsed++] = QUI_SOP(QOI_SOP_RUN) or (run - 1)
                    run = 0
                }
            } else {
                if (run > 0) {
                    bytes[bytesUsed++] = QUI_SOP(QOI_SOP_RUN) or (run - 1)
                    run = 0
                }

                val index_pos = QOI_COLOR_HASH(currentR, currentG, currentB, currentA) % 64

                if (index[index_pos] == currentPixel) {
                    bytes[bytesUsed++] = QUI_SOP(QOI_SOP_INDEX) or index_pos
                } else {
                    index[index_pos] = currentPixel

                    if (currentA == previousA) {
                        val vr = currentR - previousR
                        val vg = currentG - previousG
                        val vb = currentB - previousB

                        val vg_r = vr - vg
                        val vg_b = vb - vg

                        when {
                            vr > -3 && vr < 2 && vg > -3 && vg < 2 && vb > -3 && vb < 2 -> {
                                bytes[bytesUsed++] =
                                    QUI_SOP(QOI_SOP_DIFF) or ((vr + 2) shl 4) or ((vg + 2) shl 2) or (vb + 2)
                            }

                            vg_r > -9 && vg_r < 8 && vg > -33 && vg < 32 && vg_b > -9 && vg_b < 8 -> {
                                bytes[bytesUsed++] = QUI_SOP(QOI_SOP_LUMA) or (vg + 32)
                                bytes[bytesUsed++] = ((vg_r + 8) shl 4) or (vg_b + 8)
                            }

                            else -> {
                                bytes[bytesUsed++] = QOI_OP_RGB
                                bytes[bytesUsed++] = currentR
                                bytes[bytesUsed++] = currentG
                                bytes[bytesUsed++] = currentB
                            }
                        }
                    } else {
                        bytes[bytesUsed++] = QOI_OP_RGBA
                        bytes[bytesUsed++] = currentR
                        bytes[bytesUsed++] = currentG
                        bytes[bytesUsed++] = currentB
                        bytes[bytesUsed++] = currentA
                    }
                }
            }

            previousPixel = currentPixel
            previousR = currentR
            previousG = currentG
            previousB = currentB
            previousA = currentA
        }

        for (n in 0 until QOI_PADDING.size) sbytes[bytesUsed++] = QOI_PADDING[n]

        s.writeBytes(sbytes, 0, bytesUsed)
    }

    fun calculateMaxSize(bitmap: Bitmap): Int {
        return calculateMaxSize(bitmap.width, bitmap.height)
    }

    /**
     * Calculates the maximum encoding size (# bytes) of image with width and height when using QOI.
     */
    fun calculateMaxSize(width: Int, height: Int): Int {
        return QOI_HEADER_SIZE + (width * height * (4 + 1)) + QOI_PADDING_SIZE
    }

    private const val QOI_SRGB = 0
    private const val QOI_LINEAR = 1

    private fun QUI_SOP(op: Int): Int = (op shl 6)

    private const val QOI_SOP_INDEX = 0b00 /* 00xxxxxx */
    private const val QOI_SOP_DIFF = 0b01 /* 01xxxxxx */
    private const val QOI_SOP_LUMA = 0b10 /* 10xxxxxx */
    private const val QOI_SOP_RUN = 0b11 /* 11xxxxxx */

    private const val QOI_OP_RGB = 0xfe /* 11111110 */
    private const val QOI_OP_RGBA = 0xff /* 11111111 */

    private const val QOI_MASK_2 = 0xc0 /* 11000000 */

    private fun QOI_COLOR_HASH(r: Int, g: Int, b: Int, a: Int): Int =
        (r * 3 + g * 5 + b * 7 + a * 11)

    private fun QOI_COLOR_HASH(C: RGBA): Int = QOI_COLOR_HASH(C.r, C.g, C.b, C.a)
    val QOI_PADDING = byteArrayOf(0, 0, 0, 0, 0, 0, 0, 1)
    private const val QOI_HEADER_SIZE = 14
    private const val QOI_PADDING_SIZE = 8

    /* 2GB is the max file size that this implementation can safely handle. We guard
    against anything larger than that, assuming the worst case with 5 bytes per
    pixel, rounded down to a nice clean value. 400 million pixels ought to be
    enough for anybody. */
    private const val QOI_PIXELS_MAX = 400_000_000
}
