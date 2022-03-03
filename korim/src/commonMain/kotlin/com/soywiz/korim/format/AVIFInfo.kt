package com.soywiz.korim.format

import com.soywiz.korio.lang.*
import com.soywiz.korio.stream.*

// AVIF & HEIC metadata extractor
object AVIFInfo : BaseAVIFInfo("avif")
object HEICInfo : BaseAVIFInfo("heic")

open class BaseAVIFInfo(vararg exts: String) : ImageFormatSuspend(*exts) {
    override suspend fun decodeHeaderSuspend(s: AsyncStream, props: ImageDecodingProps): ImageInfo? {
        val ss = s.slice(4 until 8).readString(4, LATIN1)
        if (ss != "ftyp") return null
        return StreamParser(props).also { it.decodeLevel(s, 0) }.info
    }

    class StreamParser(val props: ImageDecodingProps) {
        var info = ImageInfo()

        suspend fun decodeLevel(s: AsyncStream, level: Int) {
            while (!s.eof()) {
                val blockSize = s.readS32BE()
                val blockType = s.readStringz(4, LATIN1)
                //val blockSubtype = s.readStringz(4, LATIN1)
                //val blockStream = s.readStream(blockSize - 12)
                val blockStream = s.readStream(blockSize - 8)
                //if (blockSize)
                //Console.error("${"  ".repeat(level)}blockSize=$blockSize, blockType=$blockType")
                when (blockType) {
                    "ftyp" -> Unit
                    "meta" -> {
                        blockStream.skip(4)
                        decodeLevel(blockStream, level + 1)
                    }
                    "iprp" -> decodeLevel(blockStream, level + 1)
                    "ipco" -> decodeLevel(blockStream, level + 1)
                    "ispe" -> {
                        blockStream.skip(4)
                        info.width = blockStream.readS32BE()
                        info.height = blockStream.readS32BE()
                    }
                    "mdat" -> {
                        blockStream.skip(4)
                        if (blockStream.sliceHere().readStringz(4, LATIN1) == "Exif") {
                            val exif = EXIF.readExif(blockStream.sliceHere())
                            info.orientation = exif.orientation
                        }
                    }
                }
            }
        }
    }
}
