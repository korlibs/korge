package com.soywiz.korim.format

import com.soywiz.kmem.UByteArrayInt
import com.soywiz.kmem.extract2
import com.soywiz.kmem.extract4
import com.soywiz.kmem.extract6
import com.soywiz.kmem.write32BE
import com.soywiz.korim.bitmap.Bitmap32
import com.soywiz.korim.color.RGBA
import com.soywiz.korim.color.RgbaArray
import com.soywiz.korio.lang.ASCII
import com.soywiz.korio.lang.LATIN1
import com.soywiz.korio.stream.SyncStream
import com.soywiz.korio.stream.readAvailable
import com.soywiz.korio.stream.readS32BE
import com.soywiz.korio.stream.readStringz
import com.soywiz.korio.stream.readU8
import com.soywiz.korio.stream.writeBytes

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

    override fun readImage(s: SyncStream, props: ImageDecodingProps): ImageData {
        val header = decodeHeader(s, props) ?: error("Not a QOI image")
        val bytes = UByteArrayInt(s.readAvailable())
        val index = RgbaArray(64)
        val out = Bitmap32(header.width, header.height, premultiplied = false)
        val outp = RgbaArray(out.ints)
        val totalPixels = out.area
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
        return ImageData(out)
    }

    override fun writeImage(image: ImageData, s: SyncStream, props: ImageEncodingProps) {
        val bitmap = image.mainBitmap.toBMP32IfRequired()
        val pixels = RgbaArray(bitmap.ints)
        val index = RgbaArray(64)
        val maxSize = QOI_HEADER_SIZE + (bitmap.width * bitmap.height * (4 + 1)) + QOI_PADDING_SIZE
        val bytes = UByteArrayInt(maxSize)
        val sbytes = bytes.bytes
        var o = 0
        var p = 0
        var run = 0

        bytes[p++] = 'q'.code
        bytes[p++] = 'o'.code
        bytes[p++] = 'i'.code
        bytes[p++] = 'f'.code
        sbytes.write32BE(p, bitmap.width); p += 4
        sbytes.write32BE(p, bitmap.height); p += 4
        bytes[p++] = 4
        bytes[p++] = QOI_LINEAR

        var px_prev = RGBA(0, 0, 0, 0xFF)
        var pr = 0
        var pg = 0
        var pb = 0
        var pa = 0xFF

        while (o < pixels.size) {
            val px = pixels[o++]
            val cr = px.r
            val cg = px.g
            val cb = px.b
            val ca = px.a

            if (px == px_prev) {
                run++
                if (run == 62 || o >= pixels.size) {
                    bytes[p++] = QUI_SOP(QOI_SOP_RUN) or (run - 1)
                    run = 0
                }
            } else {
                if (run > 0) {
                    bytes[p++] = QUI_SOP(QOI_SOP_RUN) or (run - 1)
                    run = 0
                }

                val index_pos = QOI_COLOR_HASH(cr, cg, cb, ca) % 64

                if (index[index_pos] == px) {
                    bytes[p++] = QUI_SOP(QOI_SOP_INDEX) or index_pos
                } else {
                    index[index_pos] = px

                    if (ca == pa) {
                        val vr = cr - pr
                        val vg = cg - pg
                        val vb = cb - pb

                        val vg_r = vr - vg
                        val vg_b = vb - vg

                        when {
                            vr > -3 && vr < 2 && vg > -3 && vg < 2 && vb > -3 && vb < 2 -> {
                                bytes[p++] = QUI_SOP(QOI_SOP_DIFF) or ((vr + 2) shl 4) or ((vg + 2) shl 2) or (vb + 2)
                            }
                            vg_r > -9 && vg_r < 8 && vg > -33 && vg < 32 && vg_b > -9 && vg_b < 8 -> {
                                bytes[p++] = QUI_SOP(QOI_SOP_LUMA) or (vg + 32)
                                bytes[p++] = ((vg_r + 8) shl 4) or (vg_b + 8)
                            }
                            else -> {
                                bytes[p++] = QOI_OP_RGB
                                bytes[p++] = cr
                                bytes[p++] = cg
                                bytes[p++] = cb
                            }
                        }
                    } else {
                        bytes[p++] = QOI_OP_RGBA
                        bytes[p++] = cr
                        bytes[p++] = cg
                        bytes[p++] = cb
                        bytes[p++] = ca
                    }
                }
            }

            px_prev = px
            pr = cr
            pg = cg
            pb = cb
            pa = ca
        }

        for (n in 0 until QOI_PADDING.size) sbytes[p++] = QOI_PADDING[n]

        s.writeBytes(sbytes, 0, p)
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

    private fun QOI_COLOR_HASH(r: Int, g: Int, b: Int, a: Int): Int = (r * 3 + g * 5 + b * 7 + a * 11)
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
