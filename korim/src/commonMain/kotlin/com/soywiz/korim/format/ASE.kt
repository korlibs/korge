package com.soywiz.korim.format

import com.soywiz.kds.*
import com.soywiz.klock.*
import com.soywiz.kmem.*
import com.soywiz.korim.bitmap.*
import com.soywiz.korim.color.*
import com.soywiz.korim.vector.*
import com.soywiz.korio.compression.*
import com.soywiz.korio.compression.deflate.*
import com.soywiz.korio.stream.*
import com.soywiz.korma.geom.*
import com.soywiz.krypto.encoding.*

// Aseprite: https://github.com/aseprite/aseprite/blob/main/docs/ase-file-specs.md
object ASE : ImageFormatWithContainer("ase") {
    override fun decodeHeader(s: SyncStream, props: ImageDecodingProps): ImageInfo? {
        val ss = s.clone()
        ss.readBytesExact(4)
        if (ss.readU16LE() != 0xA5E0) return null
        val frames = ss.readU16LE()
        val width = ss.readU16LE()
        val height = ss.readU16LE()
        val bitsPerPixel = ss.readU16LE()
        return ImageInfo().also {
            it.bitsPerPixel = bitsPerPixel
            it.width = width
            it.height = height
        }
    }

    interface AseEntity {
        var userData: String?
        var userDataColor: RGBA
        companion object {
            operator fun invoke() = Mixin()
        }
        open class Mixin : AseEntity {
            override var userData: String? = null
            override var userDataColor: RGBA = Colors.TRANSPARENT_BLACK
        }
    }

    open class AseCell(val bmp: Bitmap, val x: Int, val y: Int, val opacity: Int, val linkedCell: AseCell? = null) : AseEntity by AseEntity()
    open class AseLayer(
        index: Int,
        name: String,
        val flags: Int,
        val type: Int,
        val childLevel: Int,
        val blendModeInt: Int,
        val opacity: Int
    ) : ImageLayer(index, name), AseEntity by AseEntity() {
        val isVisible = flags.hasBitSet(0)
        val isEditable = flags.hasBitSet(1)
        val lockMovement = flags.hasBitSet(2)
        val background = flags.hasBitSet(3)
        val preferredLinkedCels = flags.hasBitSet(4)
        val collapsed = flags.hasBitSet(5)
        val isReferenceLayer = flags.hasBitSet(6)
        val isGroup = type == 1
        val blendMode = when (blendModeInt) {
            0 -> BlendMode.NORMAL
            1 -> BlendMode.MULTIPLY
            2 -> BlendMode.SCREEN
            3 -> BlendMode.OVERLAY
            4 -> BlendMode.DARKEN
            5 -> BlendMode.LIGHTEN
            6 -> BlendMode.COLOR_DODGE
            7 -> BlendMode.COLOR_BURN
            8 -> BlendMode.HARD_LIGHT
            9 -> BlendMode.SOFT_LIGHT
            10 -> BlendMode.DIFFERENCE
            11 -> BlendMode.EXCLUSION
            12 -> BlendMode.HUE
            13 -> BlendMode.SATURATION
            14 -> BlendMode.COLOR
            15 -> BlendMode.LUMINOSITY
            16 -> BlendMode.ADDITION
            17 -> BlendMode.SUBTRACT
            18 -> BlendMode.DIVIDE
            else -> BlendMode.NORMAL
        }
    }
    open class AseLayerCell(val frameIndex: Int, val layer: AseLayer, val cell: AseCell) : AseEntity by AseEntity()
    open class AseFrame(val index: Int) : AseEntity by AseEntity() {
        var time = 0
        val celsByLayer = FastIntMap<AseCell>()
    }
    open class AsePalette(
        colors: RgbaArray,
        names: Array<String?>? = null,
        changeStart: Int = 0,
        changeEnd: Int = 0
    ) : Palette(colors, names, changeStart, changeEnd), AseEntity by AseEntity()

    open class AseSliceKey(
        val frameIndex: Int,
        val sliceXOrigin: Int,
        val sliceYOrigin: Int,
        val sliceWidth: Int,
        val sliceHeight: Int,
        // 9-patch
        val centerXPos: Int,
        val centerYPos: Int,
        val centerWidth: Int,
        val centerHeight: Int,
        // Pivot
        val pivotX: Int,
        val pivotY: Int,
    ) {
        val sliceFrame = RectangleInt(sliceXOrigin, sliceYOrigin, sliceWidth, sliceHeight)
    }

    open class AseSlice(val name: String, val hasNinePatch: Boolean, val hasPivotInfo: Boolean) : AseEntity by AseEntity() {
        val keys = FastArrayList<AseSliceKey>()

        fun sortKeys() {
            keys.sortBy { it.frameIndex }
        }

        // @TODO: Optimize performance of this
        fun getSliceForFrame(index: Int): AseSliceKey {
            //keys.binarySearch()
            keys.fastForEachReverse {
                if (index >= it.frameIndex) return it
            }
            return keys.firstOrNull() ?: error("No frames key frames for slice!")
        }
    }

    open class AseTag(
        val fromFrame: Int,
        val toFrame: Int,
        val directionByte: Int,
        val tagColor: RGBA,
        val tagName: String
    ) : AseEntity by AseEntity() {
        val direction = when (directionByte) {
            0 -> ImageAnimation.Direction.FORWARD
            1 -> ImageAnimation.Direction.REVERSE
            2 -> ImageAnimation.Direction.PING_PONG
            else -> ImageAnimation.Direction.FORWARD
        }
    }

    open class AseImage : AseEntity by AseEntity() {
        var palette: AsePalette? = null
        val layers = FastArrayList<AseLayer>()
        val frames = FastArrayList<AseFrame>()
        val tags = FastArrayList<AseTag>()
        val slices = FastArrayList<AseSlice>()
    }

    override fun readImageContainer(s: SyncStream, props: ImageDecodingProps): ImageDataContainer {
        val fileSize = s.readS32LE()
        if (s.length < fileSize) error("File too short")
        if (s.readU16LE() != 0xA5E0) error("Not an Aseprite file")
        val numFrames = s.readU16LE()
        val imageWidth = s.readU16LE()
        val imageHeight = s.readU16LE()
        val bitsPerPixel = s.readU16LE()
        val flags = s.readU32LE()
        val speed = s.readU16LE()
        s.skip(4)
        s.skip(4)
        val transparentIndex = s.readU8()
        s.skip(3)
        val numColors = s.readU16LE()
        val pixelWidth = s.readU8()
        val pixelHeight = s.readU8()
        val gridX = s.readS16LE()
        val gridY = s.readS16LE()
        val gridWidth = s.readU16LE()
        val gridHeight = s.readU16LE()
        s.skip(84)

        val image = AseImage()

        for (frameIndex in 0 until numFrames) {
            //println("FRAME: $n")
            val bytesInFrame = s.readS32LE()
            val fs = s.readStream(bytesInFrame - 4)
            if (fs.readU16LE() != 0xF1FA) error("Invalid ASE sprite file or error parsing")
            fs.readU16LE()
            val frameDuration = fs.readU16LE()
            fs.skip(2)
            val numChunks = fs.readS32LE()

            val currentFrame = AseFrame(frameIndex)
            image.frames.add(currentFrame)
            currentFrame.time = frameDuration
            var lastEntity: AseEntity? = null
            var lastCell: AseCell? = null

            for (nc in 0 until numChunks) {
                val chunkSize = fs.readS32LE()
                val chunkType = fs.readU16LE()
                //println("chunkSize=$chunkSize, chunkType=${chunkType.hex}")
                val cs = fs.readStream(chunkSize - 6)

                when (chunkType) {
                    0x0004 -> { // PALETTE4 (deprecated)
                        if (image.palette == null) {
                            // Only read if not set already
                        }
                    }
                    0x0011 -> { // _PALETTE11 (deprecated)
                        if (image.palette == null) {
                            // Only read if not set already
                        }
                    }
                    0x2004 -> { // LAYER
                        val flags = cs.readU16LE()
                        val type = cs.readU16LE()
                        val childLevel = cs.readU16LE()
                        val defaultLayerWidth = cs.readU16LE()
                        val defaultLayerHeight = cs.readU16LE()
                        val blendModeInt = cs.readU16LE()
                        val opacity = cs.readU8()
                        cs.skip(3)
                        val name = cs.readAseString()
                        //println("- layer = $name, childLevel = $childLevel, blendMode = $blendMode")
                        val layer = AseLayer(image.layers.size, name, flags, type, childLevel, blendModeInt, opacity)
                        image.layers.add(layer)
                        lastEntity = layer
                    }
                    0x2005 -> { // CEL
                        val layerIndex = cs.readU16LE()
                        val x = cs.readS16LE()
                        val y = cs.readS16LE()
                        val opacity = cs.readU8()
                        val celType = cs.readU16LE()
                        cs.skip(7)
                        val cel: AseCell? = when (celType) {
                            0, 2 -> {
                                val width = cs.readU16LE()
                                val height = cs.readU16LE()
                                //cs.skip(6)
                                val data = cs.readAvailable()
                                val udata = if (celType == 2) data.uncompress(ZLib) else data
                                //println("celType = $celType, width=$width, height=$height, data=${data.size}, udata=${udata.size}")
                                val bmp = when (bitsPerPixel) {
                                    32 -> Bitmap32(width, height, RgbaArray(udata.readIntArray(0, width * height, true)))
                                    8 -> Bitmap8(width, height, udata, image.palette!!.colors)
                                    else -> error("Unsupported ASE mode")
                                }
                                AseCell(bmp, x, y, opacity, null)
                            }
                            1 -> {
                                val linkFrame = cs.readU16LE()
                                val aseCell = image.frames[linkFrame].celsByLayer[layerIndex]!!
                                AseCell(aseCell.bmp, x, y, opacity, aseCell)
                            }
                            else -> error("Aseprite: Unknown celType=$celType")
                        }
                        lastEntity = cel
                        lastCell = cel
                        if (cel != null) {
                            currentFrame.celsByLayer[layerIndex] = cel
                        }
                    }
                    0x2006 -> { // CEL_EXTRA
                        val flags = cs.readS32LE()
                        val x = cs.readFixedLE().double
                        val y = cs.readFixedLE().double
                        val width = cs.readFixedLE().double
                        val height = cs.readFixedLE().double
                        cs.skip(16)
                        //lastCell!!.x
                    }
                    0x2007 -> { // COLOR_PROFILE
                        val type = cs.readU16LE()
                        val flags = cs.readU16LE()
                        val gamma = cs.readFixedLE()
                        cs.skip(8)
                    }
                    0x2016 -> { // MASK (deprecated)
                        // Ignored
                    }
                    0x2017 -> { // PATH
                        // Not used
                    }
                    0x2018 -> { // TAGS
                        // Tags
                        val numTags = cs.readU16LE()
                        cs.skip(8)
                        for (tag in 0 until numTags) {
                            val fromFrame = cs.readU16LE()
                            val toFrame = cs.readU16LE()
                            val direction = cs.readU8()
                            cs.skip(8)
                            val tagColor = cs.readRGBA().withA(0xFF)
                            val tagName = cs.readAseString()
                            val tag = AseTag(fromFrame, toFrame, direction, tagColor, tagName)
                            image.tags.add(tag)
                        }
                    }
                    0x2019 -> { // PALETTE
                        val numEntries = cs.readS32LE()
                        val changeStart = cs.readS32LE()
                        val changeEnd = cs.readS32LE()
                        val colors = RgbaArray(numEntries)
                        val colorNames = arrayOfNulls<String>(numEntries)
                        cs.skip(8)
                        for (ncol in 0 until numEntries) {
                            val flags = cs.readU16LE()
                            val rgba = cs.readRGBA()
                            val name = if (flags.hasBitSet(0)) cs.readAseString() else null
                            colors[ncol] = rgba
                            colorNames[ncol] = name
                            //println("PALETTE: $n, rgba=$rgba, name=$name")
                        }
                        image.palette = AsePalette(colors, colorNames, changeStart, changeEnd)
                        lastEntity = image.palette
                    }
                    0x2020 -> { // USERDATA
                        val flags = cs.readS32LE()
                        if (flags.hasBitSet(0)) {
                            lastEntity?.userData = cs.readAseString()
                        }
                        if (flags.hasBitSet(1)) {
                            lastEntity?.userDataColor = cs.readRGBA()
                        }
                    }
                    0x2022 -> { // SLICE KEYS
                        val numSliceKeys = cs.readS32LE()
                        val sliceFlags = cs.readS32LE()
                        cs.skip(4)
                        val sliceName = cs.readAseString()
                        val hasNinePatch = sliceFlags.hasBitSet(0)
                        val hasPivotInfo = sliceFlags.hasBitSet(1)

                        val slice = AseSlice(sliceName, hasNinePatch, hasPivotInfo)
                        image.slices.add(slice)
                        lastEntity = slice

                        for (nsk in 0 until numSliceKeys) {
                            val sliceFrameIndex = cs.readS32LE()
                            val sliceXOrigin = cs.readS32LE()
                            val sliceYOrigin = cs.readS32LE()
                            val sliceWidth = cs.readS32LE()
                            val sliceHeight = cs.readS32LE()
                            val centerXPos = if (hasNinePatch) cs.readS32LE() else 0
                            val centerYPos = if (hasNinePatch) cs.readS32LE() else 0
                            val centerWidth = if (hasNinePatch) cs.readS32LE() else sliceWidth
                            val centerHeight = if (hasNinePatch) cs.readS32LE() else sliceHeight
                            val pivotX = if (hasPivotInfo) cs.readS32LE() else 0
                            val pivotY = if (hasPivotInfo) cs.readS32LE() else 0

                            slice.keys.add(AseSliceKey(
                                sliceFrameIndex,
                                sliceXOrigin,
                                sliceYOrigin,
                                sliceWidth,
                                sliceHeight,
                                centerXPos,
                                centerYPos,
                                centerWidth,
                                centerHeight,
                                pivotX,
                                pivotY,
                            ))
                        }
                    }
                    else -> println("WARNING: Aseprite: Not implemented chunkType=${chunkType.hex}")
                }
            }
        }
        val imageLayers = image.layers

        val frames = image.frames.map { frame ->
            val cells = frame.celsByLayer.toLinkedMap()
            val layerData = cells.map { (key, value) -> ImageFrameLayer(imageLayers[key], value.bmp.slice(), value.x, value.y, main = false, includeInAtlas = true) }
            ImageFrame(frame.index, frame.time.milliseconds, layerData)
        }

        val animations = image.tags.map {
            val framesRange = frames.slice(it.fromFrame.clamp(0, frames.size) .. it.toFrame.clamp(0, frames.size - 1))

            ImageAnimation(framesRange, it.direction, it.tagName)
        }

        val defaultData = ImageData(
            frames = frames,
            width = imageWidth,
            height = imageHeight,
            layers = imageLayers,
            animations = animations,
        )

        val datas = image.slices.map { slice ->
            slice.sortKeys()
            val maxWidth = slice.keys.map { it.sliceWidth }.maxOrNull() ?: 0
            val maxHeight = slice.keys.map { it.sliceHeight }.maxOrNull() ?: 0
            val frames = defaultData.frames.map {
                val sliceKey = slice.getSliceForFrame(it.index)
                ImageFrame(it.index, it.time, layerData = it.layerData.map {
                    // @TODO: What should we do with layers here?
                    // @TODO: Linked frame?
                    //ImageFrameLayer(it.layer, it.slice.bmp.slice(sliceKey.sliceFrame, it.slice.name), it.targetX, it.targetY, it.main, it.includeInAtlas, null)
                    //println("name=${slice.name}: it.slice=${it.slice.bounds}, sliceKey.sliceFrame=${sliceKey.sliceFrame}")
                    ImageFrameLayer(it.layer, it.slice.slice(sliceKey.sliceFrame, it.slice.name), it.targetX, it.targetY, it.main, it.includeInAtlas, null)
                })
            }
            val animations = image.tags.map {
                val framesRange = frames.slice(it.fromFrame.clamp(0, frames.size) .. it.toFrame.clamp(0, frames.size - 1))
                ImageAnimation(framesRange, it.direction, it.tagName)
            }
            ImageData(
                frames = frames,
                width = maxWidth,
                height = maxHeight,
                layers = imageLayers,
                animations = animations,
                name = slice.name
            )
        }

        return ImageDataContainer(listOf(defaultData) + datas)
    }

    fun SyncStream.readRGB(): RGBA = RGBA(readU8(), readU8(), readU8())
    fun SyncStream.readRGBA(): RGBA = RGBA(readS32LE())
    fun SyncStream.readFixedLE(): Fixed32 = Fixed32(readS32LE())
    fun SyncStream.readAseString(): String = readString(readU16LE())
}

inline class Fixed32(val value: Int) {
    val integral: Int get() = ((value ushr 0) and 0xFFFF)
    val decimal: Int get() = ((value ushr 16) and 0xFFFF)
    val double: Double get() = TODO()
}
