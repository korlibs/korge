package com.soywiz.korim.format

import com.soywiz.kds.FastArrayList
import com.soywiz.kds.extraProperty
import com.soywiz.kds.fastArrayListOf
import com.soywiz.kmem.UByteArrayInt
import com.soywiz.korim.bitmap.Bitmap
import com.soywiz.korim.bitmap.Bitmap32
import com.soywiz.korim.bitmap.Bitmap8
import com.soywiz.korim.bitmap.BitmapChannel
import com.soywiz.korio.async.runBlockingNoSuspensions
import com.soywiz.korio.file.std.ZipVfs
import com.soywiz.korio.lang.UTF8
import com.soywiz.korio.lang.toString
import com.soywiz.korio.serialization.xml.descendants
import com.soywiz.korio.serialization.xml.firstDescendant
import com.soywiz.korio.serialization.xml.readXml
import com.soywiz.korio.stream.SyncStream
import com.soywiz.korio.stream.hasMore
import com.soywiz.korio.stream.openSync
import com.soywiz.korio.stream.readBytes
import com.soywiz.korio.stream.readBytesExact
import com.soywiz.korio.stream.readStringz
import com.soywiz.korio.stream.toAsync
import kotlin.math.max
import kotlin.native.concurrent.ThreadLocal

object KRA : ImageFormat("kra") {
    private const val mergedImagePng = "mergedimage.png"
    private const val maindocXml = "maindoc.xml"

    override fun decodeHeader(s: SyncStream, props: ImageDecodingProps): ImageInfo? {
        if (s.clone().readBytesExact(2).toString(UTF8) != "PK") return null
        var out: ImageInfo? = null
        runBlockingNoSuspensions {
            val vfs = ZipVfs(s.clone().toAsync())
            val mergedFile = vfs[mergedImagePng]
            val mergedBytes = mergedFile.readRangeBytes(0L..128)
            out = PNG.decodeHeader(mergedBytes.openSync(), props)
        }
        return out
    }

    fun SyncStream.readLine() = readStringz(zero = '\n'.code.toByte())

    fun readLayer(s: SyncStream): Bitmap32 {
        var chunkCount = 0
        var version = 0
        var tileWidth = 64
        var tileHeight = 64
        var pixelSize = 4
        while (s.hasMore) {
            val line = s.readLine().uppercase()
            val key = line.substringBefore(" ", "").trim()
            val value = line.substringAfter(" ", "").trim()
            when (key) {
                "TILEWIDTH" -> tileWidth = value.toInt()
                "TILEHEIGHT" -> tileHeight = value.toInt()
                "PIXELSIZE" -> pixelSize = value.toInt()
                "VERSION" -> version = value.toInt()
                "DATA" -> {
                    chunkCount = value.toInt()
                    break
                }
            }
        }

        if (version != 2) error("KRA: Only supported layer version=2, but was $version")
        if (pixelSize != 4) error("KRA: Only supported layer pixelSize=4, but was $pixelSize")

        var imageWidth = 0
        var imageHeight = 0

        class Tile(val x: Int, val y: Int, val data: Bitmap32)

        val tiles = FastArrayList<Tile>()

        for (chunkId in 0 until chunkCount) {
            val def = s.readLine().uppercase()
            val parts = def.split(",")
            val x = parts[0].toInt()
            val y = parts[1].toInt()
            val format = parts[2]
            val chunkByteCount = parts[3].toInt()
            val data = s.readBytes(chunkByteCount)

            imageWidth = max(imageWidth, x + tileWidth)
            imageHeight = max(imageHeight, y + tileHeight)
            when (format) {
                "LZF" -> {
                    val tileData = ByteArray(tileWidth * tileHeight * pixelSize + 1)
                    val tileUData = UByteArrayInt(tileData)
                    val result = lzfDecompress(UByteArrayInt(data), data.size, tileUData, tileData.size)
                    if (result >= 0) {
                        //println("tileData.size=${tileData.size} --- result=$result")
                        val bmp = Bitmap32(tileWidth, tileHeight, premultiplied = false)
                        run {
                            val tileOffset = 1
                            val tileSize = tileWidth * tileHeight
                            bmp.writeChannel(
                                BitmapChannel.RED,
                                Bitmap8(
                                    tileWidth,
                                    tileHeight,
                                    tileData.copyOfRange(tileOffset + tileSize * 0, tileOffset + tileSize * 1)
                                )
                            )
                            bmp.writeChannel(
                                BitmapChannel.GREEN,
                                Bitmap8(
                                    tileWidth,
                                    tileHeight,
                                    tileData.copyOfRange(tileOffset + tileSize * 1, tileOffset + tileSize * 2)
                                )
                            )
                            bmp.writeChannel(
                                BitmapChannel.BLUE,
                                Bitmap8(
                                    tileWidth,
                                    tileHeight,
                                    tileData.copyOfRange(tileOffset + tileSize * 2, tileOffset + tileSize * 3)
                                )
                            )
                            bmp.writeChannel(
                                BitmapChannel.ALPHA,
                                Bitmap8(
                                    tileWidth,
                                    tileHeight,
                                    tileData.copyOfRange(tileOffset + tileSize * 3, tileOffset + tileSize * 4)
                                )
                            )
                            tiles.add(Tile(x, y, bmp))
                        }
                    }
                    //println("result=$result")
                }
                else -> error("KRA: Unsupported encoding format '$format'")
            }
        }

        val bmp = Bitmap32(imageWidth, imageHeight, premultiplied = false)

        tiles.fastForEach {
            bmp.put(it.data, it.x, it.y)
        }

        //launchImmediately(Dispatchers.Unconfined) { bmp.showImageAndWait() }

        //println("imageWidth=$imageWidth, imageHeight=$imageHeight")
        return bmp
    }

    fun lzfDecompress(in_data: UByteArrayInt, in_len: Int, out_data: UByteArrayInt, out_len: Int): Int {
        var iidx = 0
        var oidx = 0

        do {
            var ctrl = in_data[iidx++]

            if (ctrl < (1 shl 5)) { // literal run
                ctrl++

                if (oidx + ctrl > out_len) return -1 //SET_ERRNO (E2BIG);

                //println("LITERAL COUNT: $ctrl")

                do {
                    out_data[oidx++] = in_data[iidx++]
                } while ((--ctrl) != 0)
            } else { // back reference
                var len = ctrl ushr 5

                var reference = (oidx - ((ctrl and 0x1f) shl 8) - 1).toInt()

                if (len == 7) len += in_data[iidx++]

                reference -= in_data[iidx++]

                //println("LZ: $reference, len=$len")

                if (oidx + len + 2 > out_len) return -1 //SET_ERRNO (E2BIG);
                if (reference < 0) return -2 //SET_ERRNO (EINVAL);

                out_data[oidx++] = out_data[reference++]
                out_data[oidx++] = out_data[reference++]

                do {
                    out_data[oidx++] = out_data[reference++]
                } while ((--len) != 0)
            }
        } while (iidx < in_len)

        return oidx.toInt()
    }

    override fun readImage(s: SyncStream, props: ImageDecodingProps, out: Bitmap?): ImageData {
        return runBlockingNoSuspensions {
            val vfs = ZipVfs(s.clone().toAsync())
            val folderVfs = vfs.listSimple().filter { it.isDirectory() }.firstOrNull() ?: error("No root folder in zip")
            //println(folderVfs)
            val mergedBytes = vfs[mergedImagePng].readAll()
            val xml = vfs[maindocXml].readXml()
            val frames = fastArrayListOf<ImageFrame>()
            val mainBitmap = PNG.readImage(mergedBytes.openSync()).mainBitmap
            frames.add(ImageFrame(mainBitmap, main = true, includeInAtlas = false))
            if (props.kritaLoadLayers) {
                val imageXml = xml.firstDescendant("image")
                val width = imageXml?.int("width") ?: 0
                val height = imageXml?.int("height") ?: 0
                //println("width=$width,height=$height")

                for (layer in xml.descendants("layer")) {
                    val layerX = layer.int("x")
                    val layerY = layer.int("y")
                    val layerName = layer.str("name")
                    val layerFile = layer.str("filename")
                    when (layer.str("nodetype").lowercase()) {
                        "paintlayer" -> {
                            val layerBytes = folderVfs["layers/$layerFile"].readBytes()
                            val layerPartialBitmap = readLayer(layerBytes.openSync())
                            if (props.kritaPartialImageLayers) {
                                frames.add(
                                    ImageFrame(
                                        layerPartialBitmap,
                                        targetX = layerX,
                                        targetY = layerY,
                                        name = layerName,
                                        main = false
                                    )
                                )
                            } else {
                                frames.add(
                                    ImageFrame(
                                        Bitmap32(width, height, premultiplied = true).also { it.put(layerPartialBitmap, layerX, layerY) },
                                        name = layerName, main = false
                                    )
                                )
                            }
                            //println("layer=$layer")


                            //println("layerName=$layerName, layerFile=$layerFile")
                        }
                    }
                }
            }
            //println(xml.descendants("layer").toList())
            //println(xml)
            ImageData(frames, width = mainBitmap.width, height = mainBitmap.height)
        }
    }

}

@ThreadLocal
var ImageDecodingProps.kritaLoadLayers by extraProperty { true }
@ThreadLocal
var ImageDecodingProps.kritaPartialImageLayers by extraProperty { false }
