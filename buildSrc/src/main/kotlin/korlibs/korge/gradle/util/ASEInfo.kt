package korlibs.korge.gradle.util

data class ASEInfo(
    val slices: List<AseSlice> = emptyList(),
    val tags: List<AseTag> = emptyList(),
) {
    data class AseSlice(
        val sliceName: String,
        val hasNinePatch: Boolean,
        val hasPivotInfo: Boolean,
    )

    data class AseTag(
        val fromFrame: Int,
        val toFrame: Int,
        val direction: Int,
        val tagColor: Int,
        val tagName: String
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

                //println("   - $numChunks")

                for (nc in 0 until numChunks) {
                    val chunkSize = fs.readS32LE()
                    val chunkType = fs.readU16LE()
                    val cs = fs.readStream(chunkSize - 6)

                    //println(" chunkType=$chunkType, chunkSize=$chunkSize")

                    when (chunkType) {
                        0x2022 -> { // SLICE KEYS
                            val numSliceKeys = cs.readS32LE()
                            val sliceFlags = cs.readS32LE()
                            cs.skip(4)
                            val sliceName = cs.readAseString()
                            val hasNinePatch = sliceFlags.hasBitSet(0)
                            val hasPivotInfo = sliceFlags.hasBitSet(1)
                            val aslice = AseSlice(sliceName, hasNinePatch, hasPivotInfo)
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
                slices = slices,
                tags = tags,
            )
        }

        fun ByteArraySimpleInputStream.readAseString(): String = readBytes(readU16LE()).toString(Charsets.UTF_8)
        public infix fun Int.hasBitSet(index: Int): Boolean = ((this ushr index) and 1) != 0
    }
}
