package com.soywiz.korge.gradle.targets.windows

import com.soywiz.korge.gradle.util.*
import java.awt.image.*
import java.io.*
import kotlin.math.*

@Suppress("UNUSED_VARIABLE")
object ICO2 {
    // https://en.wikipedia.org/wiki/ICO_(file_format)
    fun encode(images: List<BufferedImage>): ByteArray {
        val s = ByteArrayOutputStream()
        // 6
        s.write16LE(0)
        s.write16LE(1) // ICO
        s.write16LE(images.size)

        val payloadStart = 6 + 16 * images.size
        val payloadData = ByteArrayOutputStream()

        // 16 per entry
        for (frame in images) {
            val bitmap = frame
            val width = bitmap.width
            val height = bitmap.height
            if (width > 256 || height > 256) error("Size too big for ICO image: ${bitmap.width}x${bitmap.height}")

            s.write8(width)
            s.write8(height)
            s.write8(0) // Palette size
            s.write8(0) // Reserved
            s.write16LE(1) // Color planes
            s.write16LE(32) // Bits per pixel

            val start = payloadData.size().toInt()
            if (width == 32 && height == 32) {
                val bmp = BMP2.encode(bitmap)
                payloadData.writeBytes(bmp.sliceArray(14 until bmp.size))
                payloadData.writeBytes(ByteArray(width * height / 8))
            } else {
                payloadData.writeBytes(bitmap.encodePNG())
            }
            val size = payloadData.size().toInt() - start

            s.write32LE(size)
            s.write32LE(payloadStart + start)
        }

        s.writeBytes(payloadData.toByteArray())
        return s.toByteArray()
    }
}

@Suppress("UNUSED_VARIABLE")
object BMP2 {
    fun encode(image: BufferedImage): ByteArray {
        val bmp = image
        val s = ByteArrayOutputStream(64 + bmp.area * 4)

        //
        s.write8('B'.toInt())
        s.write8('M'.toInt())
        s.write32LE(4 * bmp.area)
        s.write32LE(0) // Reserved
        s.write32LE(54) // Offset to data

        s.write32LE(40)
        s.write32LE(bmp.width)
        s.write32LE(bmp.height * 2)
        s.write16LE(1) // Planes
        s.write16LE(32) // Bit count
        s.write32LE(0) // Compression
        s.write32LE(4 * bmp.area) // Size
        s.write32LE(2834) // Pels per meter
        s.write32LE(2834) // Pels per meter
        s.write32LE(0) // Clr used
        s.write32LE(0) // Important
        //s.writeBytes(BGRA.encode(bmp.data))

        val ints = (image.data.dataBuffer as DataBufferInt).data

        for (n in 0 until bmp.height) {
            val y = bmp.height - 1 - n
            for (x in 0 until bmp.width) {
                // @TODO: Check this
                s.write32LE(ints[y * bmp.width + x])
            }
            //s.writeBytes(BGRA.encode(bmp.data, y * bmp.width, bmp.width, littleEndian = true))
        }
        return s.toByteArray()
    }
}
