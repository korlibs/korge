package korlibs.image.format

import korlibs.datastructure.*
import korlibs.time.milliseconds
import korlibs.math.clamp
import korlibs.image.bitmap.*
import korlibs.image.color.Colors
import korlibs.image.color.RGBA
import korlibs.image.color.RgbaArray
import korlibs.image.vector.BlendMode
import korlibs.io.compression.deflate.ZLib
import korlibs.io.compression.uncompress
import korlibs.io.stream.SyncStream
import korlibs.io.stream.readAvailable
import korlibs.io.stream.readBytesExact
import korlibs.io.stream.readS16LE
import korlibs.io.stream.readS32LE
import korlibs.io.stream.readStream
import korlibs.io.stream.readString
import korlibs.io.stream.readU16LE
import korlibs.io.stream.readU32LE
import korlibs.io.stream.readU8
import korlibs.math.geom.slice.*
import korlibs.encoding.hex
import korlibs.image.tiles.*
import korlibs.math.geom.*
import korlibs.memory.*

// If this is true, only processes visible layers from the ASE file.
// Otherwise, will process invisible layers as well.
var ImageDecodingProps.onlyReadVisibleLayers: Boolean by extraProperty { true }

// Aseprite: https://github.com/aseprite/aseprite/blob/main/docs/ase-file-specs.md
// Aseprite 1.3: https://github.com/aseprite/aseprite/blob/4f2eae6b7754f432f960bc6cbacc4e6d26914abf/docs/ase-file-specs.md
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
            override var userDataColor: RGBA = Colors.TRANSPARENT
        }
    }

    interface AseCell : AseEntity {
        val bmp: Bitmap
        val x: Int
        val y: Int
        val opacity: Int
        fun resolve(): AseCell = this
    }

    open class AseBitmapCell(
        override val bmp: Bitmap,
        override val x: Int, override val y: Int, override val opacity: Int,
    ) : AseEntity by AseEntity(), AseCell

    open class AseLinkedCell(
        val linkedCell: AseCell,
        override val x: Int, override val y: Int, override val opacity: Int,
    ) : AseEntity by AseEntity(), AseCell {
        override val bmp: Bitmap get() = linkedCell.bmp
        override fun resolve(): AseCell = linkedCell.resolve()
    }

    open class AseTilemapCell(
        val data: IntArray2,
        val tileBitmask: Int,
        val bitmaskXFlip: Int,
        val bitmaskYFlip: Int,
        val bitmask90CWFlip: Int,
        override val x: Int, override val y: Int, override val opacity: Int,
    ) : AseEntity by AseEntity(), AseCell {
        override val bmp: Bitmap = Bitmap32(1, 1, premultiplied = true)
    }

    open class AseLayer(
        val originalAseIndex: Int,
        index: Int,
        name: String,
        val flags: Int,
        type: ImageLayer.Type,
        val childLevel: Int,
        val blendModeInt: Int,
        val opacity: Int,
        val tilesetIndex: Int,
    ) : ImageLayer(index, name, type), AseEntity by AseEntity() {
        val isVisible = flags.hasBitSet(0)
        val isEditable = flags.hasBitSet(1)
        val lockMovement = flags.hasBitSet(2)
        val background = flags.hasBitSet(3)
        val preferredLinkedCels = flags.hasBitSet(4)
        val collapsed = flags.hasBitSet(5)
        val isReferenceLayer = flags.hasBitSet(6)
        val isGroup get() = type.isGroup
        val isTilemap get() = type.isTilemap
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

    open class AseLayerCell(val frameIndex: Int, val layer: AseLayer, val cell: AseCell) :
        AseEntity by AseEntity()

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

    data class AseTileset(
        val tilesetId: Int,
        val ntiles: Int,
        val tileWidth: Int,
        val tileHeight: Int,
        val baseIndex: Int,
        val tilesetName: String,
        val tiles: List<BmpSlice>,
    ) {
        val tileSet = TileSet(IntMap<TileSetTileInfo>().also { map ->
            for (n in tiles.indices) {
                //val id = baseIndex + n
                val id = n
                map[id] = TileSetTileInfo(id, tiles[n])
            }
        })
    }

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
        var sliceFrame = RectangleInt(sliceXOrigin, sliceYOrigin, sliceWidth, sliceHeight)
    }

    open class AseSlice(val name: String, val hasNinePatch: Boolean, val hasPivotInfo: Boolean) :
        AseEntity by AseEntity() {
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
            return keys.firstOrNull()
                ?: error("No frames key frames for slice!")
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
        val externalFiles = IntMap<String>()
        val tilesets = IntMap<AseTileset>()
    }

    /**
     * This function reads the Aseprite image file, decodes its content and stores selected details into an
     * [ImageDataContainer] object.
     *
     * With the [props] parameter it is possible to selectively load data from an Aseprite image file. If props are
     * not defined than the whole image will be read with all layers and slices.
     *
     * [props] can be defined as: (name, type)
     * - "layers", String:
     *   With this property it is possible to define layer names (comma separated string) which shall be read from the
     *   Aseprite file. Other layers will be ignored.
     *
     * - "disableSlicing", Boolean:
     *   With this it is possible to switch of slicing of the Aseprite image even if slices are defined in
     *   the image. This can be used to selectively slice only specific layers. While all other layers are not sliced.
     *
     * - "useSlicePosition", Boolean:
     *   With this it is possible to set the position of a sliced image object relative to the original image. This is
     *   useful if the sliced image should be placed relative to another sliced image in the view which uses these images.
     *   This property has only an effect if "disableSlicing" is set to "false".
     *
     * Example for [props]:
     *
     *     val props = ImageDecodingProps(this.baseName, extra = ExtraTypeCreate())
     *     props.setExtra("layers", "layer_1,layer_2,layer_3")
     *     props.setExtra("disableSlicing", false)
     *     props.setExtra("useSlicePosition", true)
     */
    override fun readImageContainer(s: SyncStream, props: ImageDecodingProps): ImageDataContainer {
        val fileSize = s.readS32LE()
        if (s.length < fileSize) error("File too short")
        if (s.readU16LE() != 0xA5E0) error("Not an Aseprite file")
        val numFrames = s.readU16LE()
        val imageWidth = s.readU16LE()
        val imageHeight = s.readU16LE()
        val bitsPerPixel = s.readU16LE()
        val bytesPerPixel = bitsPerPixel / 8
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
                        val type = ImageLayer.Type.BY_ORDINAL[cs.readU16LE()]
                        val childLevel = cs.readU16LE()
                        val defaultLayerWidth = cs.readU16LE()
                        val defaultLayerHeight = cs.readU16LE()
                        val blendModeInt = cs.readU16LE()
                        val opacity = cs.readU8()
                        cs.skip(3)
                        val name = cs.readAseString()
                        val tilesetIndex = when {
                            type.isTilemap -> cs.readS32LE()
                            else -> 0
                        }
                        //println("- layer = $name, childLevel = $childLevel, blendMode = $blendMode")
                        val layer = AseLayer(
                            image.layers.size,
                            image.layers.size,
                            name,
                            flags,
                            type,
                            childLevel,
                            blendModeInt,
                            opacity,
                            tilesetIndex
                        )
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
                            0, 2 -> { // 0=Raw Image Data, 2=Compressed Image
                                val width = cs.readU16LE()
                                val height = cs.readU16LE()
                                //cs.skip(6)
                                val data = cs.readAvailable()
                                //println("size=${width * height * bytesPerPixel} width=$width, height=$height, bytesPerPixel=$bytesPerPixel, props=$props")
                                val udata = if (celType == 2) data.uncompress(
                                    ZLib,
                                    width * height * bytesPerPixel
                                ) else data
                                //val udata = if (celType == 2) data.uncompress(ZLib.Portable) else data
                                //println("celType = $celType, width=$width, height=$height, data=${data.size}, udata=${udata.size}")
                                val bmp = when (bitsPerPixel) {
                                    32 -> {
                                        Bitmap32(
                                            width,
                                            height,
                                            RgbaArray(udata.getS32Array(0, width * height, true))
                                        ).also {
                                            if (props.premultipliedSure) it.premultiplyInplaceIfRequired()
                                        }
                                    }

                                    8 -> Bitmap8(width, height, udata, image.palette!!.colors)
                                    else -> error("Unsupported ASE mode")
                                }
                                AseBitmapCell(bmp, x, y, opacity)
                            }

                            1 -> { // 1=Linked Cel
                                val linkFrame = cs.readU16LE()
                                val aseCell: AseCell =
                                    image.frames[linkFrame].celsByLayer[layerIndex]!!
                                AseLinkedCell(aseCell, x, y, opacity)
                            }

                            3 -> { // 3=Compressed Tilemap
                                val tilesWidth = cs.readU16LE()
                                val tilesHeight = cs.readU16LE()
                                val bitsPerTile = cs.readU16LE()
                                val tileBitmask =
                                    cs.readS32LE() // Bitmask for tile ID (e.g. 0x1fffffff for 32-bit tiles)
                                val bitmaskXFlip = cs.readS32LE()
                                val bitmaskYFlip = cs.readS32LE()
                                val bitmask90CWFlip = cs.readS32LE()
                                val bytesPerTile = bitsPerTile / 8
                                cs.skip(10) // Reserved
                                val compressedData = cs.readAvailable()
                                    .uncompress(ZLib, tilesWidth * tilesHeight * bytesPerTile)
                                val data = when (bitsPerTile) {
                                    32 -> compressedData.getS32ArrayLE(0, tilesWidth * tilesHeight)
                                    else -> TODO("Only supported 32-bits per tile")
                                }
                                AseTilemapCell(
                                    IntArray2(tilesWidth, tilesHeight, data),
                                    tileBitmask,
                                    bitmaskXFlip,
                                    bitmaskYFlip,
                                    bitmask90CWFlip,
                                    x, y, opacity
                                )
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

                    0x2008 -> { // External Files Chunk
                        val nentries = cs.readS32LE()
                        cs.skip(8) // Reserved
                        for (n in 0 until nentries) {
                            val entryId = cs.readS32LE()
                            cs.skip(8) // Reserved
                            val externalFileName = cs.readAseString()
                            image.externalFiles[entryId] = externalFileName
                        }
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

                            slice.keys.add(
                                AseSliceKey(
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
                                )
                            )
                        }
                    }

                    0x2023 -> { // Tileset Chunk
                        val tilesetId = cs.readS32LE()
                        val tilesetFlags = cs.readS32LE()
                        val includeLinkToExternalFile = tilesetFlags.extract(0)
                        val includeTilesInsideThisFile = tilesetFlags.extract(1)
                        val tile0IsEmpty = tilesetFlags.extract(2) // Usually always true
                        val ntiles = cs.readS32LE()
                        val tileWidth = cs.readU16LE()
                        val tileHeight = cs.readU16LE()
                        val baseIndex = cs.readS16LE()
                        cs.skip(14) // Reserved
                        val tilesetName = cs.readAseString()
                        var tiles = listOf<BmpSlice>()
                        if (includeLinkToExternalFile) {
                            val externalFileId = cs.readS32LE()
                            val externalTilesetId = cs.readS32LE()
                        }
                        if (includeTilesInsideThisFile) {
                            val compressedDataLength = cs.readS32LE()
                            val compressedData =
                                cs.readBytesExact(compressedDataLength) // (Tile Width) x (Tile Height x Number of Tiles)
                            val data = compressedData.uncompress(ZLib)
                            val ints = data.getS32ArrayLE(0, tileWidth * tileHeight * ntiles)
                            val bitmap =
                                Bitmap32(tileWidth, tileHeight * ntiles, RgbaArray(ints)).also {
                                    if (props.premultipliedSure) it.premultiplyInplaceIfRequired()
                                }
                            tiles = bitmap.slice().splitInRows(tileWidth, tileHeight)
                        }
                        image.tilesets[tilesetId] = AseTileset(
                            tilesetId,
                            ntiles,
                            tileWidth,
                            tileHeight,
                            baseIndex,
                            tilesetName,
                            tiles
                        )
                    }

                    else -> println("WARNING: Aseprite: Not implemented chunkType=${chunkType.hex}")
                }
            }
        }

        val imageLayers = image.layers
        val imageLayersToProcess = imageLayers.filter {
            val shouldProcess = if (props.onlyReadVisibleLayers) it.isVisible else true
            shouldProcess && props.takeLayersByName(it.name as String)
        }

        fun createImageFrameLayer(key: Int, value: AseCell): ImageFrameLayer {
            val resolved = value.resolve()
            val layer = imageLayers[key]
            var tilemap: TileMapData? = null
            if (resolved is AseTilemapCell) {
                val tileset = image.tilesets[layer.tilesetIndex]
                tilemap = TileMapData(
                    resolved.data,
                    tileset?.tileSet,
                    maskData = resolved.tileBitmask,
                    maskFlipX = resolved.bitmaskXFlip,
                    maskFlipY = resolved.bitmaskYFlip,
                    maskRotate = resolved.bitmask90CWFlip,
                )
            }
            return ImageFrameLayer(
                layer,
                resolved.bmp.slice(),
                resolved.x,
                resolved.y,
                main = false,
                includeInAtlas = true,
                tilemap = tilemap
            )
        }

        val frames = image.frames.mapNotNull { frame ->
            val cells = frame.celsByLayer.toLinkedMap().filter {
                val shouldProcess = if (props.onlyReadVisibleLayers) imageLayers[it.key].isVisible else true
                shouldProcess && props.takeLayersByName(imageLayers[it.key].name as String)
            }
            // Then create list of layer data from those cells and their corresponding visible layers
            val layerData = cells.map { (key, value) ->
                createImageFrameLayer(key, value)
            }
            if (layerData.isNotEmpty()) {
                ImageFrame(frame.index, frame.time.milliseconds, layerData)
            } else {
                ImageFrame(frame.index)
            }
        }

        val animations = image.tags.map {
            val framesRange = frames.slice(
                it.fromFrame.clamp(0, frames.size)..it.toFrame.clamp(
                    0,
                    frames.size - 1
                )
            )
            ImageAnimation(framesRange, it.direction, it.tagName)
        }

        val defaultData = ImageData(
            frames = frames,
            width = imageWidth,
            height = imageHeight,
            layers = imageLayersToProcess,
            animations = animations
        )

        // Create image data for all slices of frames but take only layers which are defined in "props"
        val datas = image.slices.mapNotNull { slice ->
            slice.sortKeys()
            val maxWidth = slice.keys.maxOfOrNull { it.sliceWidth } ?: 0
            val maxHeight = slice.keys.maxOfOrNull { it.sliceHeight } ?: 0

            val frames = image.frames.mapNotNull { frame ->
                val sliceKey = slice.getSliceForFrame(frame.index)
                val cells = frame.celsByLayer.toLinkedMap().filter {
                    val shouldProcess = if (props.onlyReadVisibleLayers) imageLayers[it.key].isVisible else true
                    shouldProcess && props.takeLayersByName(imageLayers[it.key].name as String)
                }
                // Create list of layer data from found cells and slice them
                val layerData = cells.map { (key, value) ->
                    createImageFrameLayer(key, value)
                }.map {
                    val sliceKeyFrame = sliceKey.sliceFrame
                    val sliceFrame = RectangleInt(
                        sliceKeyFrame.x - it.targetX, sliceKeyFrame.y - it.targetY,
                        sliceKeyFrame.width, sliceKeyFrame.height
                    )
                    val sslice = it.slice.slice(sliceFrame, it.slice.name)
                    val ninePatchSlice = when {
                        slice.hasNinePatch -> sslice.asNinePatchSimple(
                            sliceKey.centerXPos, sliceKey.centerYPos,
                            sliceKey.centerXPos + sliceKey.centerWidth, sliceKey.centerYPos + sliceKey.centerHeight,
                        )
                        else -> null
                    }
                    ImageFrameLayer(
                        it.layer, sslice,
                        (if (props.useSlicePosition()) sliceKeyFrame.x else it.targetX) - sliceKey.pivotX,
                        (if (props.useSlicePosition()) sliceKeyFrame.y else it.targetY) - sliceKey.pivotY,
                        it.main, it.includeInAtlas,
                        ninePatchSlice = ninePatchSlice
                    )
                }
                if (layerData.isNotEmpty()) {
                    ImageFrame(frame.index, frame.time.milliseconds, layerData = layerData)
                } else null
            }
            if (frames.isNotEmpty() && props.slicingEnabled()) {
                val animations = image.tags.map {
                    val framesRange =
                        frames.slice(
                            it.fromFrame.clamp(0, frames.size)..it.toFrame.clamp(
                                0,
                                frames.size - 1
                            )
                        )
                    ImageAnimation(framesRange, it.direction, it.tagName)
                }
                //println("Add sliced layer: ${slice.name}")
                ImageData(
                    frames = frames,
                    width = maxWidth,
                    height = maxHeight,
                    layers = imageLayersToProcess,
                    animations = animations,
                    name = slice.name
                )
            } else null
        }

        // Finally the index in the layer details needs to be adjusted because the invisible layers do not exist
        // in ImageAnimationView objects. The index would be out of bounds otherwise.
        imageLayersToProcess.forEachIndexed { index, element -> element.index = index }

        return ImageDataContainer(datas.ifEmpty { listOf(defaultData) })
    }

    private fun ImageDecodingProps.takeLayersByName(name: String): Boolean =
        getExtra("layers") == null || (getExtra("layers") as String).contains(name)

    private fun ImageDecodingProps.slicingEnabled(): Boolean =
        getExtraTyped<Boolean>("disableSlicing") == null || !(getExtraTyped<Boolean>("disableSlicing")!!)

    private fun ImageDecodingProps.useSlicePosition(): Boolean =
        getExtraTyped<Boolean>("useSlicePosition") != null && getExtraTyped<Boolean>("useSlicePosition")!!

    fun SyncStream.readRGB(): RGBA = RGBA(readU8(), readU8(), readU8())
    fun SyncStream.readRGBA(): RGBA = RGBA(readS32LE())
    internal fun SyncStream.readFixedLE(): Fixed32 = Fixed32.fromRaw(readS32LE())
    fun SyncStream.readAseString(): String = readString(readU16LE())
}

// https://github.com/aseprite/aseprite/blob/50d4f9d8028dc56686b7f0720ef4775db7b2f782/src/fixmath/fixmath.h
internal inline class Fixed32 private constructor(val raw: Int) {
    val double: Double get() = raw.toDouble() / 65536.0

    companion object {
        fun fromRaw(raw: Int): Fixed32 = Fixed32(raw)
        operator fun invoke(x: Double): Fixed32 {
            if (x !in -32767.0 .. 32767.0) error("x=$x is outside Fixed32 range")
            return Fixed32((x * 65536.0 + (if (x < 0) -0.5 else 0.5)).toInt())
        }
    }
}
