package com.soywiz.korim.format

import com.soywiz.kmem.*
import com.soywiz.korim.bitmap.*
import com.soywiz.korim.color.*
import com.soywiz.korio.lang.*
import com.soywiz.korio.stream.*

// https://www.adobe.com/devnet-apps/photoshop/fileformatashtml/
object PSD : ImageFormat("psd") {
    override fun readImage(s: SyncStream, props: ImageDecodingProps): ImageData = s.run {
        val header = decodeHeader(this, props) ?: invalidOp("Not a PSD file")
        @Suppress("UNUSED_VARIABLE") val colorMode = readStream(readS32BE())
        @Suppress("UNUSED_VARIABLE") val imageResources = readStream(readS32BE())
        @Suppress("UNUSED_VARIABLE") val layerAndMask = readStream(readS32BE())
        val imageData = readAvailable().openFastStream().readImageData(header)
        //println(colorMode.length)
        //println(imageResources.length)
        //println(layerAndMask.length)
        return ImageData(listOf(ImageFrame(imageData)))
    }

    private fun packChannels(width: Int, height: Int, channels: Array<ByteArray>): Bitmap32 {
        var pos = 0
        val out = Bitmap32(width, height)

        val dummyChannel = ByteArray(width * height) { -1 }
        val rchannel = channels.getOrNull(0) ?: dummyChannel
        val gchannel = channels.getOrNull(1) ?: dummyChannel
        val bchannel = channels.getOrNull(2) ?: dummyChannel
        val achannel = channels.getOrNull(3) ?: dummyChannel

        for (y in 0 until height) {
            for (x in 0 until width) {
                val r = rchannel[pos].unsigned
                val g = gchannel[pos].unsigned
                val b = bchannel[pos].unsigned
                val a = achannel[pos].unsigned
                out.data[pos] = RGBA(r, g, b, a)
                pos++
            }
        }
        //println(channels.toList().map { it.toList() })
        return out
    }

    private fun FastByteArrayInputStream.readImageData(header: PsdImageInfo): Bitmap32 {
        val compressionMethod = readU16BE()
        val width = header.width
        val height = header.height
        val cchannels = header.channels
        val channels = Array(cchannels) { ByteArray(width * height) }

        when (compressionMethod) {
            0 -> { // RAW
                for (n in 0 until cchannels) {
                    channels[n] = this.readBytes(width * height)
                }
            }
            1 -> { // RLE
                val sizes = (0 until cchannels).map { readShortArrayBE(height) }
                //println("PSD: channels=${header.channels}, bitsPerChannel=${header.bitsPerChannel}, colorMode=${header.colorMode}")
                //println(sizes)

                for (cindex in 0 until cchannels) {
                    val carray = channels[cindex]
                    var cpos = 0
                    for (size in sizes[cindex]) {
                        val end = offset + size
                        while (offset < end) {
                            val len = readU8()
                            if (len >= 128) {
                                val byte = readU8()
                                for (j in 0..(256 - len)) carray[cpos++] = byte.toByte()
                            } else {
                                for (j in 0..len) carray[cpos++] = readU8().toByte()
                            }
                        }
                    }
                }
            }
            else -> invalidOp("Unsupported compression method $compressionMethod")
        }
        return packChannels(width, height, channels)
    }

    class PsdImageInfo : ImageInfo() {
        var channels = 0
        var bitsPerChannel = 0
        var colorMode = 0
    }

    override fun decodeHeader(s: SyncStream, props: ImageDecodingProps): PsdImageInfo? = s.run {
        if (readStringz(4) != "8BPS") return null
        val version = readU16BE()
        when (version) {
            1 -> Unit
            2 -> return null // PSB file not supported yet!
            else -> return null
        }
        @Suppress("UNUSED_VARIABLE")
        val reserved = readBytes(6)
        val channels = readU16BE()
        val height = readS32BE()
        val width = readS32BE()
        val bitsPerChannel = readU16BE()
        val colorMode = readU16BE()
        return PsdImageInfo().apply {
            this.width = width
            this.height = height
            this.bitsPerPixel = bitsPerChannel * channels
            this.channels = channels
            this.bitsPerChannel = bitsPerChannel
            this.colorMode = colorMode
        }
    }
}
