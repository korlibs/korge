package korlibs.korge.gradle.util

data class ASEInfo(
    val pixelWidth: Int = 0,
    val pixelHeight: Int = 0,
    val slices: List<AseSlice> = emptyList(),
    val tags: List<AseTag> = emptyList(),
    val layers: List<AseLayer> = emptyList(),
    val frames: List<Frame> = emptyList(),
) {

    val tagsByName: Map<String, AseTag> by lazy { tags.associateBy { it.tagName } }
    val layersByName: Map<String, AseLayer> by lazy { layers.associateBy { it.layerName } }

    data class AseSlice(
        val sliceName: String,
        val hasNinePatch: Boolean,
        val hasPivotInfo: Boolean,
        val keys: List<SliceKey>
    ) {
        data class SliceKey(
            val frameNumber: Int,
            val x: Int,
            val y: Int,
            val width: Int,
            val height: Int,
            val ninePatch: NinePatchInfo? = null,
            val pivot: PivotInfo? = null
        )
        data class NinePatchInfo(
            val centerX: Int,
            val centerY: Int,
            val centerWidth: Int,
            val centerHeight: Int
        )
        data class PivotInfo(
            val pivotX: Int,
            val pivotY: Int
        )
    }

    data class AseTag(
        val fromFrame: Int,
        val toFrame: Int,
        val direction: Int,
        val tagColor: Int,
        val tagName: String
    )

    data class AseLayer(
        val layerName: String,
        val visible: Boolean,
        val type: AseLayerType,
        val blendMode: AseBlendMode,
        val opacity: Int
    ) {
        enum class AseLayerType(val value: Int) {
            NORMAL(0),
            GROUP(1),
            TILEMAP(2);

            companion object {
                private val map = values().associateBy(AseLayerType::value)
                fun fromInt(value: Int): AseLayerType = map[value] ?: NORMAL
            }
        }

        enum class AseBlendMode(val value: Int) {
            NORMAL(0),
            MULTIPLY(1),
            SCREEN(2),
            OVERLAY(3),
            DARKEN(4),
            LIGHTEN(5),
            COLOR_DODGE(6),
            COLOR_BURN(7),
            HARD_LIGHT(8),
            SOFT_LIGHT(9),
            DIFFERENCE(10),
            EXCLUSION(11),
            HUE(12),
            SATURATION(13),
            COLOR(14),
            LUMINOSITY(15),
            ADDITION(16),
            SUBTRACT(17),
            DIVIDE(18);

            companion object {
                private val map = values().associateBy(AseBlendMode::value)
                fun fromInt(value: Int): AseBlendMode = map[value] ?: NORMAL
            }
        }
    }

    data class Frame(
        val index: Int,
        val duration: Int
    )

    companion object {
        fun getAseInfo(file: SFile): ASEInfo {
            return getAseInfo(file.readBytes())
        }

        fun getAseInfo(data: ByteArray): ASEInfo {
            return getAseInfo(ByteArraySimpleInputStream(ByteArraySlice(data)))
        }

        fun getAseInfo(s: ByteArraySimpleInputStream): ASEInfo {
            if (s.length == 0) return ASEInfo()

            val slices = arrayListOf<AseSlice>()
            val tags = arrayListOf<AseTag>()
            val layers = arrayListOf<AseLayer>()
            val frames = arrayListOf<Frame>()

            val fileSize = s.readS32LE()
            if (s.length < fileSize) error("File too short s.length=${s.length} < fileSize=${fileSize}")
            val headerMagic = s.readU16LE()
            if (headerMagic != 0xA5E0) error("Not an Aseprite file : headerMagic=$headerMagic")
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

            //println("ASE fileSize=$fileSize, headerMagic=$headerMagic, numFrames=$numFrames, $imageWidth x $imageHeight, bitsPerPixel=$bitsPerPixel, numColors=$numColors, gridWidth=$gridWidth, gridHeight=$gridHeight")

            for (frameIndex in 0 until numFrames) {
                //println("FRAME: $frameIndex")
                val bytesInFrame = s.readS32LE()
                val fs = s.readStream(bytesInFrame - 4)
                val frameMagic = fs.readU16LE()
                //println("  bytesInFrame=$bytesInFrame, frameMagic=$frameMagic, fs=$fs")
                if (frameMagic != 0xF1FA) error("Invalid ASE sprite file or error parsing : frameMagic=$frameMagic")
                fs.readU16LE()
                val frameDuration = fs.readU16LE()
                fs.skip(2)
                val numChunks = fs.readS32LE()
                frames += Frame(frameIndex, frameDuration)

                //println("   - $numChunks")

                for (nc in 0 until numChunks) {
                    val chunkSize = fs.readS32LE()
                    val chunkType = fs.readU16LE()
                    val cs = fs.readStream(chunkSize - 6)

                    //println(" chunkType=$chunkType, chunkSize=$chunkSize")

                    when (chunkType) {
                        0x2004 -> { // LAYER
                            // Layer chunk
                            val flags = cs.readU16LE()
                            val type = cs.readU16LE()
                            val layerChildLevel = cs.readU16LE()
                            val defaultLayerWidth = cs.readU16LE()  // ignore default width
                            val defaultLayerHeight = cs.readU16LE()  // ignore default height
                            val blendMode = cs.readU16LE()
                            val opacity = cs.readU8()
                            cs.skip(3)
                            val layerName = cs.readAseString()
                            layers += AseLayer(
                                layerName = layerName,
                                visible = !flags.hasBitSet(0),
                                type = AseLayer.AseLayerType.fromInt(type),
                                blendMode = AseLayer.AseBlendMode.fromInt(blendMode),
                                opacity = opacity
                            )

                            //println(" Layer: name='$layerName', type=$type, blendMode=$blendMode, opacity=$opacity, flags=$flags")
                        }
                        0x2022 -> { // SLICE KEYS
                            val numSliceKeys = cs.readS32LE()
                            val sliceFlags = cs.readS32LE()
                            cs.skip(4)
                            val sliceName = cs.readAseString()
                            val hasNinePatch = sliceFlags.hasBitSet(0)
                            val hasPivotInfo = sliceFlags.hasBitSet(1)

                            // Read 9-patch and pivot info
                            val keys = mutableListOf<AseSlice.SliceKey>()
                            for (key in 0 until numSliceKeys) {
                                val frameNumber = cs.readS32LE()
                                val x = cs.readU32LE()
                                val y = cs.readU32LE()
                                val width = cs.readS32LE()
                                val height = cs.readS32LE()

                                var ninePatch: AseSlice.NinePatchInfo? = null
                                var pivot: AseSlice.PivotInfo? = null

                                if (hasNinePatch) {
                                    val centerX = cs.readU32LE()
                                    val centerY = cs.readU32LE()
                                    val centerWidth = cs.readS32LE()
                                    val centerHeight = cs.readS32LE()
                                    ninePatch = AseSlice.NinePatchInfo(centerX.toInt(), centerY.toInt(), centerWidth, centerHeight)
                                }
                                if (hasPivotInfo) {
                                    val pivotX = cs.readU32LE()
                                    val pivotY = cs.readU32LE()
                                    pivot = AseSlice.PivotInfo(pivotX.toInt(), pivotY.toInt())
                                }

                                keys += AseSlice.SliceKey(
                                    frameNumber, x.toInt(), y.toInt(), width, height, ninePatch, pivot
                                )
                            }

                            val aslice = AseSlice(sliceName, hasNinePatch, hasPivotInfo, keys)
                            slices += aslice
                        }
                        0x2018 -> { // TAGS
                            // Tags
                            val numTags = cs.readU16LE()
                            cs.skip(8)
                            //println(" tags: numTags=$numTags")

                            for (tag in 0 until numTags) {
                                val fromFrame = cs.readU16LE()
                                val toFrame = cs.readU16LE()
                                val direction = cs.readU8()
                                cs.skip(8)
                                val tagColor = cs.readS32LE()
                                val tagName = cs.readAseString()
                                val atag = AseTag(fromFrame, toFrame, direction, tagColor, tagName)
                                tags += atag
                                //println(" tag[$tag]=$atag")
                            }
                        }
                        // Unsupported tag
                        else -> {

                        }
                    }
                }
            }

            return ASEInfo(
                pixelWidth = imageWidth,
                pixelHeight = imageHeight,
                slices = slices,
                tags = tags,
                layers = layers,
                frames = frames
            )
        }

        fun ByteArraySimpleInputStream.readAseString(): String = readBytes(readU16LE()).toString(Charsets.UTF_8)
        public infix fun Int.hasBitSet(index: Int): Boolean = ((this ushr index) and 1) != 0
    }
}
