package com.soywiz.korim.font

import com.soywiz.kds.DoubleArrayList
import com.soywiz.kds.IntArrayList
import com.soywiz.kds.IntIntMap
import com.soywiz.kds.IntMap
import com.soywiz.kds.getCyclic
import com.soywiz.kds.iterators.fastForEach
import com.soywiz.kds.mapInt
import com.soywiz.kds.toIntArrayList
import com.soywiz.kmem.extract
import com.soywiz.kmem.extract16Signed
import com.soywiz.kmem.extractSigned
import com.soywiz.kmem.insert16
import com.soywiz.korim.annotation.KorimInternal
import com.soywiz.korim.bitmap.Bitmap
import com.soywiz.korim.bitmap.Palette
import com.soywiz.korim.color.Colors
import com.soywiz.korim.color.RGBA
import com.soywiz.korim.color.RgbaArray
import com.soywiz.korim.format.PNG
import com.soywiz.korim.paint.GradientPaint
import com.soywiz.korim.paint.LinearGradientPaint
import com.soywiz.korim.paint.RadialGradientPaint
import com.soywiz.korim.vector.CompoundShape
import com.soywiz.korim.vector.Context2d
import com.soywiz.korim.vector.CycleMethod
import com.soywiz.korim.vector.FillShape
import com.soywiz.korim.vector.Shape
import com.soywiz.korim.vector.buildShape
import com.soywiz.korim.vector.toSvgPathString
import com.soywiz.korio.file.VfsFile
import com.soywiz.korio.file.baseName
import com.soywiz.korio.lang.Charset
import com.soywiz.korio.lang.UTF16_BE
import com.soywiz.korio.lang.UTF8
import com.soywiz.korio.lang.WChar
import com.soywiz.korio.lang.WString
import com.soywiz.korio.lang.WStringReader
import com.soywiz.korio.lang.invalidOp
import com.soywiz.korio.stream.AsyncInputOpenable
import com.soywiz.korio.stream.AsyncStream
import com.soywiz.korio.stream.FastByteArrayInputStream
import com.soywiz.korio.stream.openFastStream
import com.soywiz.korio.stream.openUse
import com.soywiz.korio.stream.readBytesUpTo
import com.soywiz.korio.stream.toSyncStream
import com.soywiz.korma.geom.Matrix
import com.soywiz.korma.geom.Rectangle
import com.soywiz.korma.geom.vector.IVectorPath
import com.soywiz.korma.geom.vector.VectorPath
import com.soywiz.korma.geom.vector.lineTo
import com.soywiz.korma.geom.vector.moveTo
import com.soywiz.korma.geom.vector.quadTo
import com.soywiz.krypto.encoding.hex
import kotlin.collections.set

@Suppress("MemberVisibilityCanBePrivate", "UNUSED_VARIABLE", "LocalVariableName", "unused")
// Used information from:
// - https://www.sweetscape.com/010editor/repository/files/TTF.bt
// - http://scripts.sil.org/cms/scripts/page.php?site_id=nrsi&id=iws-chapter08
// - https://www.microsoft.com/en-us/Typography/OpenTypeSpecification.aspx
// - https://en.wikipedia.org/wiki/Em_(typography)
// - http://stevehanov.ca/blog/index.php?id=143 (Let's read a Truetype font file from scratch)
// - http://chanae.walon.org/pub/ttf/ttf_glyphs.htm
@OptIn(KorimInternal::class)
class TtfFont(
    private val s: FastByteArrayInputStream,
    private val freeze: Boolean = false,
    private val extName: String? = null,
    private val onlyReadMetadata: Boolean = false,
) : VectorFont {
    constructor(
        d: ByteArray,
        freeze: Boolean = false,
        extName: String? = null,
        onlyReadMetadata: Boolean = false,
    ) : this(d.openFastStream(), freeze, extName, onlyReadMetadata)

    fun getAllBytes() = s.getAllBytes()
    fun getAllBytesUnsafe() = s.getBackingArrayUnsafe()

    override fun getFontMetrics(size: Double, metrics: FontMetrics): FontMetrics =
        metrics.copyFromNewSize(this.fontMetrics1px, size)

    override fun getGlyphMetrics(size: Double, codePoint: Int, metrics: GlyphMetrics): GlyphMetrics =
        metrics.copyFromNewSize(getGlyphByCodePoint(codePoint)?.metrics1px ?: nonExistantGlyphMetrics1px, size, codePoint)

    override fun getKerning(
        size: Double,
        leftCodePoint: Int,
        rightCodePoint: Int
    ): Double {
        // @TODO: Kerning information not read yet. Not implemented
        return 0.0
    }

    override fun getGlyphPath(size: Double, codePoint: Int, path: GlyphPath, reader: WStringReader?): GlyphPath? {
        var g = getGlyphByCodePoint(codePoint) ?: return null

        val subs = substitutionsCodePoints[codePoint]
        if (reader != null && subs != null) {
            //for (v in subs.map) println(v.key.toCodePointIntArray().toList())
            for (count in kotlin.math.min(reader.available, subs.maxSequence) downTo 2) {
                val sub = reader.substr(0, count)
                val replacement = subs.map[sub]
                //println("sub=${sub.toCodePointIntArray().toList()}")
                if (replacement != null) {
                    //println("replacement=$replacement")
                    reader.skip(sub.length)
                    g = getGlyphByIndex(replacement.first()) ?: break
                    break
                }
            }
            //println("maxSequence: " + subs.maxSequence)
            //println("getGlyphPath: codePoint=$codePoint, glyphID=${g.index}, subs=${subs}")
        }


        val scale = getTextScale(size)
        //println("unitsPerEm = $unitsPerEm")
        path.path = g.path.path
        path.colorPaths = g.colorEntry?.getColorPaths()
        val bitmapEntry = g.bitmapEntry
        val bitmap = bitmapEntry?.getBitmap()
        path.bitmap = bitmap
        if (bitmapEntry != null) {
            //println("bitmapEntry=$bitmapEntry")
            val scaleX = unitsPerEm.toDouble() / bitmapEntry.info.ppemX.toDouble()
            val scaleY = unitsPerEm.toDouble() / bitmapEntry.info.ppemY.toDouble()
            path.bitmapOffset.setTo(
                0.0,
                ((-bitmapEntry.height - bitmapEntry.descender) * scaleY),
            )
            path.bitmapScale.setTo(
                scaleX,
                scaleY,
            )
        }
        path.transform.identity()
        //path.transform.scale(scale, -scale)
        path.transform.scale(scale, scale)
        path.scale = scale
        return path
    }

    private fun getTextScale(size: Double) = size / unitsPerEm.toDouble()

    class NamesInfo {
        internal val names = arrayOfNulls<String>(NameId.MAX_ID)
        internal val namesOffsets = IntArray(NameId.MAX_ID)
        internal val namesLengths = IntArray(NameId.MAX_ID) { -1 }
        internal val namesCharsets = Array(NameId.MAX_ID) { UTF8 }
        internal var namesS: FastByteArrayInputStream? = null

        val ttfName: String get() = getName(NameId.NAME) ?: getName(NameId.COMPLETE_NAME) ?: "TtfFont"
        val ttfCompleteName: String get() = getName(NameId.COMPLETE_NAME) ?: ttfName

        fun getName(nameId: Int): String? {
            if (names[nameId] == null) {
                val nameLength = namesLengths[nameId]
                if (nameLength < 0) return null
                val string = namesS!!.extractString(namesOffsets[nameId], nameLength, namesCharsets[nameId])
                names[nameId] = when {
                    string.isEmpty() -> string
                    string[0] == '\u0000' -> string.filter { it != '\u0000' }
                    else -> string
                }
            }
            return names[nameId]
        }

        fun getName(nameId: NameId): String? = getName(nameId.id)

        fun toMap() = NameId.values().map { it to getName(it) }.toMap()

        override fun toString(): String = "NamesInfo(${toMap()})"
    }

    private val namesi = NamesInfo()
    private val tempContours = Array(3) { Contour() }
    private val lineHeight get() = yMax - yMin

    var numGlyphs = 0; private set
    var maxPoints = 0; private set
    var maxContours = 0; private set
    var maxCompositePoints = 0; private set
    var maxCompositeContours = 0; private set
    var maxZones = 0; private set
    var maxTwilightPoints = 0; private set
    var maxStorage = 0; private set
    var maxFunctionDefs = 0; private set
    var maxInstructionDefs = 0; private set
    var maxStackElements = 0; private set
    var maxSizeOfInstructions = 0; private set
    var maxComponentElements = 0; private set
    var maxComponentDepth = 0; private set

    private var hheaVersion = Fixed(0, 0)
    var ascender = 0; private set
    var descender = 0; private set
    var lineGap = 0; private set
    var advanceWidthMax = 0; private set
    var minLeftSideBearing = 0; private set
    var minRightSideBearing = 0; private set
    var xMaxExtent = 0; private set
    var caretSlopeRise = 0; private set
    var caretSlopeRun = 0; private set
    var caretOffset = 0; private set
    var metricDataFormat = 0; private set
    var numberOfHMetrics = 0; private set

    private var locs = IntArray(0)

    private var fontRev = Fixed(0, 0)
    var unitsPerEm = 128; private set
    // Coordinates have to be divided between unitsPerEm and multiplied per font size
    private var xMin = 0
    private var yMin = 0
    private var xMax = 0
    private var yMax = 0
    private var macStyle = 0
    private var lowestRecPPEM = 0
    private var fontDirectionHint = 0

    private var indexToLocFormat = 0
    private var glyphDataFormat = 0

    private var horMetrics = listOf<HorMetric>()
    @KorimInternal
    val characterMaps = LinkedHashMap<Int, Int>()
    @KorimInternal
    val characterMapsReverse = LinkedHashMap<Int, Int>()
    private val tablesByName = LinkedHashMap<String, Table>()
    private val glyphCache = IntMap<Glyph>(512)
    private fun getCharacterMapOrNull(key: Int): Int? = characterMaps[key]

    private fun addCharacterMap(codePoint: Int, index: Int) {
        characterMaps[codePoint] = index
        characterMapsReverse[index] = codePoint
    }

    // Bitmap extension
    var bitmapGlyphInfos = LinkedHashMap<Int, BitmapGlyphInfo>()

    // Color extension
    private var colrv0LayerInfos = arrayOf<ColrLayerInfo>()
    private val colrGlyphInfos = IntMap<ColrGlyphInfo>()
    var palettes = listOf<Palette>()
    private var colrv1: COLRv1 = COLRv1()

    private var frozen = false

    class SubstitutionInfo(val maxSequence: Int, val map: Map<WString, IntArray>)

    //val substitutionsGlyphIds = IntMap<Map<List<Int>, List<Int>>>()
    val substitutionsCodePoints = IntMap<SubstitutionInfo>()

    init {
        readHeaderTables()
        readHead()
        readMaxp()
        readHhea()
        readNames()
        if (!onlyReadMetadata) {
            readLoca()
            readCmap()
            readHmtx()
            readCpal()
            readColr()
            readGsub()
            //readPost()
            readCblc()
            readCbdt()
            readSbix()
        }

        //println("tablesByName=$tablesByName")

        if (freeze) {
            getAllGlyphs(cache = true).fastForEach {
                it.metrics1px // Compute it
                it.path // Compute it
            }
        }

        frozen = true

        //substitutionsGlyphIds.fastForEach { from, subsMap ->
        //    val fromCodePoint = getCodePointFromCharIndex(from) ?: -1
        //    val map = LinkedHashMap<WString, List<Int>>()
        //    var maxSeq = 0
        //    for ((fromGlyphIds, to) in subsMap) {
        //        val fromCodePoints = fromGlyphIds.map { getCodePointFromCharIndex(it) ?: -1 }
        //        val wstr = WString((listOf(fromCodePoint) + fromCodePoints).toIntArray())
        //        map[wstr] = to
        //        maxSeq = kotlin.math.max(maxSeq, wstr.length)
        //    }
        //    //println("substitution: $fromCodePoints -> $to")
        //    substitutionsCodePoints[fromCodePoint] = SubstitutionInfo(maxSeq, map)
        //}
    }

    val ttfName: String get() = namesi.ttfName
    val ttfCompleteName: String get() = namesi.ttfCompleteName
    override val name: String get() = extName ?: ttfName

    override fun toString(): String = "TtfFont(name=$name)"

    private val fontMetrics1px = FontMetrics().also {
        val scale = getTextScale(1.0)
        it.size = 1.0
        it.top = (this.yMax) * scale
        it.ascent = this.ascender * scale
        it.baseline = 0.0 * scale
        it.descent = this.descender * scale
        it.bottom = (this.yMin) * scale
        it.leading = this.lineGap * scale
        it.maxWidth = this.advanceWidthMax *scale
    }

    private data class Table(val id: String, val checksum: Int, val offset: Int, val length: Int) {
		lateinit var s: FastByteArrayInputStream

		fun open() = s.clone()
	}

	@Suppress("unused")
	enum class NameIds(val id: Int) {
		COPYRIGHT(0), FONT_FAMILY_NAME(1), FONT_SUBFAMILY_NAME(2), UNIQUE_FONT_ID(3),
		FULL_FONT_NAME(4), VERSION_STRING(5), POSTSCRIPT_NAME(6), TRADEMARK(7),
		MANUFACTURER(8), DESIGNER(9), DESCRIPTION(10), URL_VENDOR(11),
		URL_DESIGNER(12), LICENSE_DESCRIPTION(13), LICENSE_URL(14), RESERVED_15(15),
		TYPO_FAMILY_NAME(16), TYPO_SUBFAMILY_NAME(17), COMPATIBLE_FULL(18), SAMPLE_TEXT(19),
		POSTSCRIPT_CID(20), WWS_FAMILY_NAME(21), WWS_SUBFAMILY_NAME(22), LIGHT_BACKGROUND_PALETTE(23),
		DARK_BACKGROUND_PALETTE(24), VARIATIONS_POSTSCRIPT_NAME_PREFIX(25);

		companion object {
			val names = values()
		}
	}

    internal fun FastByteArrayInputStream.readFWord(): FWord = FWord(readU16BE())
    internal fun FastByteArrayInputStream.readFixed(): Int = Fixed(readS16BE(), readS16BE())
    internal fun FastByteArrayInputStream.readFixed2(): Fixed = Fixed(readS32BE())
	data class HorMetric(val advanceWidth: Int, val lsb: Int)

    @PublishedApi
	internal fun openTable(name: String) = tablesByName[name]?.open()

    var isOpenType = false

	private fun readHeaderTables() {
        tablesByName.putAll(readHeaderTables(s.sliceStart()))
	}
    companion object {
        suspend fun readNames(s: AsyncInputOpenable): NamesInfo = s.openUse {
            readNames(it as AsyncStream)
        }

        suspend fun readNames(s: AsyncStream): NamesInfo {
            s.setPosition(0L)
            //s.readAll()
            val header = s.readBytesUpTo(0x400)
            val table = readHeaderTables(header.openFastStream())
            val tableName = table["name"]!!
            s.setPosition(tableName.offset.toLong())
            val nameBytes = s.readBytesUpTo(tableName.length)
            return NamesInfo().also { readNamesSection(nameBytes.openFastStream(), it) }
        }

        fun readNamesSection(s: FastByteArrayInputStream, info: NamesInfo) = s.run {
            val format = readU16BE()
            val count = readU16BE()
            val stringOffset = readU16BE()
            for (n in 0 until count) {
                val platformId = readU16BE()
                val encodingId = readU16BE()
                val languageId = readU16BE()
                val nameId = readU16BE()
                val length = readU16BE()
                val offset = readU16BE()
                if (nameId < 0 || nameId >= NameId.MAX_ID) continue

                val charset = when (encodingId) {
                    0 -> UTF8
                    1 -> UTF16_BE
                    else -> UTF16_BE
                }
                //if ((platformId == 0 && languageId == 0) || nameId !in namesInfo) {
                if ((platformId == 0 && languageId == 0) || info.namesLengths[nameId] == -1) {
                    info.namesOffsets[nameId] = stringOffset + offset
                    info.namesLengths[nameId] = length
                    info.namesCharsets[nameId] = charset
                    info.namesS = this
                }
                //println("p=$platformId, e=$encodingId, l=$languageId, n=$nameId, l=$length, o=$offset: $string")
            }
        }

        private fun readHeaderTables(s: FastByteArrayInputStream): Map<String, Table> = s.run {
            // https://docs.microsoft.com/en-us/typography/opentype/spec/otff#collections
            // ttcf
            if (readS32BE() == 0x74746366) {
                // Read first font
                val majorVersion = readU16BE()
                val minorVersion = readU16BE()
                val numFonts = readS32BE()
                val tableDirectoryOffsets = readIntArrayBE(numFonts)
                if (majorVersion >= 2) {
                    val dsigTag = readS32BE()
                    val dsigLength = readS32BE()
                    val dsigOffset = readS32BE()
                }
                position = tableDirectoryOffsets[0]
            } else {
                unread(4)
            }
            val majorVersion = readU16BE().apply { if (this != 1 && this != 0x4F54) invalidOp("Not a TTF/OTF file") }
            val minorVersion = readU16BE().apply { if (this != 0 && this != 0x544F) invalidOp("Not a TTF/OTF file") }
            val numTables = readU16BE()
            val searchRange = readU16BE()
            val entrySelector = readU16BE()
            val rangeShift = readU16BE()

            val tables = (0 until numTables).map {
                Table(readStringz(4), readS32BE(), readS32BE(), readS32BE())
            }

            val tablesByName = LinkedHashMap<String, Table>()

            for (table in tables) {
                table.s = sliceWithSize(table.offset, table.length)
                tablesByName[table.id] = table
            }

            return tablesByName
        }
    }

    private inline fun runTableUnit(name: String, callback: FastByteArrayInputStream.() -> Unit) {
		openTable(name)?.callback()
	}

    private inline fun <T> runTable(name: String, callback: FastByteArrayInputStream.(Table) -> T): T? = openTable(name)?.let { callback(it, tablesByName[name]!!) }

    enum class NameId(val id: Int) {
        COPYRIGHT(0),
        NAME(1),
        STYLE(2),
        UNAME(3),
        COMPLETE_NAME(4),
        RELEASE_VERSION(5),
        POSTSCRIPT_NAME(6),
        TRADEMARK(7),
        MANUFACTURER(8),
        DESIGNER(9),
        DESCRIPTION(10),
        URL_VENDOR(11),
        URL_DESIGNER(12),
        LICENSE_DESCRIPTION(13),
        LICENSE_URL(14),
        RESERVED_15(15),
        PREFERRED_FAMILY(16),
        PREFERRED_SUBFAMILY(17),
        COMPATIBLE_FULL(18),
        SAMPLE_TEXT(19),
        POSTSCRIPT_CID(20),
        WWS_FAMILY_NAME(21),
        WWS_SUBFAMILY_NAME(22),
        LIGHT_BACKGROUND(23),
        DARK_BACKGROUND(23),
        VARIATION_POSTSCRIPT_PREFIX(25);

        companion object {
            const val MAX_ID = 26
        }
    }

    fun getName(nameId: Int): String? = namesi.getName(nameId)
    fun getName(nameId: NameId): String? = getName(nameId.id)

    private fun readNames() = runTableUnit("name") {
        readNamesSection(this, namesi)
	}

    data class NameInfo(val offset: Int, val length: Int, val charset: Charset)

    private fun readLoca() = runTableUnit("loca") {
        //println("readLoca! $name")
		val bytesPerEntry = when (indexToLocFormat) {
			0 -> 2
			1 -> 4
			else -> invalidOp
		}

        //println("LOCAL: numGlyphs=$numGlyphs, indexToLocFormat=$indexToLocFormat, bytesPerEntry=$bytesPerEntry");
		val data = readBytesExact(bytesPerEntry * (numGlyphs + 1))

		locs = FastByteArrayInputStream(data).run {
			when (indexToLocFormat) {
				0 -> IntArray(numGlyphs + 1) { readU16BE() * 2 }
				1 -> IntArray(numGlyphs + 1) { readS32BE() }
				else -> invalidOp
			}
		}
        //for ((index, loc) in locs.withIndex()) println("LOC[$index] = ${(loc / 2).hex}")
		//println("locs: ${locs.toList()}")
	}

    private fun readHead() = runTableUnit("head") {
		readU16BE().apply { if (this != 1) invalidOp("Invalid TTF") }
		readU16BE().apply { if (this != 0) invalidOp("Invalid TTF") }
		fontRev = readFixed()
		val checkSumAdjustment = readS32BE()
		readS32BE().apply { if (this != 0x5F0F3CF5) invalidOp("Invalid magic ${this.hex}") }
		val flags = readU16BE()
		unitsPerEm = readU16BE()
		val created = readS64BE() * 1000L
		val modified = readS64BE() * 1000L
		xMin = readS16BE()
		yMin = readS16BE()
		xMax = readS16BE()
		yMax = readS16BE()
		macStyle = readU16BE()
		lowestRecPPEM = readU16BE()
		fontDirectionHint = readS16BE()
		indexToLocFormat = readS16BE() // 0=Int16, 1=Int32
		glyphDataFormat = readS16BE()

		//println("unitsPerEm: $unitsPerEm")
		//println("created: ${DateTime(created) - 76.years}")
		//println("modified: ${DateTime(modified) - 76.years}")
		//println("bounds: ($xMin, $yMin)-($xMax, $yMax)")
	}

    private fun readMaxp() = runTableUnit("maxp") {
		val version = readFixed()
		numGlyphs = readU16BE()
		maxPoints = readU16BE()
		maxContours = readU16BE()
		maxCompositePoints = readU16BE()
		maxCompositeContours = readU16BE()
		maxZones = readU16BE()
		maxTwilightPoints = readU16BE()
		maxStorage = readU16BE()
		maxFunctionDefs = readU16BE()
		maxInstructionDefs = readU16BE()
		maxStackElements = readU16BE()
		maxSizeOfInstructions = readU16BE()
		maxComponentElements = readU16BE()
		maxComponentDepth = readU16BE()
	}

    private fun readHhea() = runTableUnit("hhea") {
		hheaVersion = readFixed()
		ascender = readS16BE()
		descender = readS16BE()
		lineGap = readS16BE()
		advanceWidthMax = readU16BE()
		minLeftSideBearing = readS16BE()
		minRightSideBearing = readS16BE()
		xMaxExtent = readS16BE()
		caretSlopeRise = readS16BE()
		caretSlopeRun = readS16BE()
		caretOffset = readS16BE()
		readS16BE() // reserved
		readS16BE() // reserved
		readS16BE() // reserved
		readS16BE() // reserved
		metricDataFormat = readS16BE()
		numberOfHMetrics = readU16BE()
	}

    private fun readHmtx() = runTableUnit("hmtx") {
		val firstMetrics = (0 until numberOfHMetrics).map {
            HorMetric(
                readU16BE(),
                readS16BE()
            )
        }
		val lastAdvanceWidth = firstMetrics.last().advanceWidth
		val compressedMetrics =
			(0 until (numGlyphs - numberOfHMetrics)).map {
                HorMetric(
                    lastAdvanceWidth,
                    readS16BE()
                )
            }
		horMetrics = firstMetrics + compressedMetrics
	}

    private fun readCpal() = runTableUnit("CPAL") {
        val version = readU16BE()
        when (version) {
            0, 1 -> {
                val numPaletteEntries = readU16BE()
                val numPalettes = readU16BE()
                val numColorRecords = readU16BE()
                val colorRecordsArrayOffset = readS32BE()
                val colorRecordIndices = readShortArrayBE(numPalettes)
                val paletteTypesArrayOffset = if (version == 1) readS32BE() else -1
                val paletteLabelsArrayOffset = if (version == 1) readS32BE() else -1
                val paletteEntryLabelsArrayOffset = if (version == 1) readS32BE() else -1
                position = colorRecordsArrayOffset
                val colorInts = readIntArrayLE(numColorRecords)
                for (n in 0 until colorInts.size) {
                    val c = RGBA(colorInts[n])
                    colorInts[n] = RGBA(c.b, c.g, c.r, c.a).value
                }
                palettes = colorRecordIndices.map {
                    Palette(RgbaArray(colorInts.copyOfRange(it.toInt(), it.toInt() + numPaletteEntries)))
                }
                // @TODO: Handle CPAL v1 info
            }
            else -> {
                println("TTF WARNING CPAL version != 0,1")
            }
        }
    }

    //private fun readPost() = runTableUnit("post") {
    //    val versionHi = readU16BE()
    //    val versionLo = readU16BE()
    //    when (versionHi) {
    //        1, 2, 3 -> {
    //            val italicAngle = readFixed()
    //            val underlinePosition = readFWord()
    //            val underlineThickness = readFWord()
    //            val isFixedPitch = readS32BE()
    //            val minMemType42 = readS32BE()
    //            val maxMemType42 = readS32BE()
    //            val minMemType1 = readS32BE()
    //            val maxMemType1 = readS32BE()
    //            if (versionHi < 3) {
    //                val numGlyphs = readU16LE()
    //                val glyphNameIndex = readCharArrayBE(numGlyphs)
    //                //println("isFixedPitch=$isFixedPitch")
    //                //println("numGlyphs=$numGlyphs")
    //                //println("glyphNameIndex=${glyphNameIndex.map { it.toInt() }.toList()}")
    //            }
    //        }
    //    }
    //}

    data class ColorBitmapInfo(
        val indexSubTableArrayOffset: Int = 0,
        val indexTablesSize: Int = 0,
        val numberofIndexSubTables: Int = 0,
        val colorRef: Int = 0,
        val hori: SbitLineMetrics = SbitLineMetrics(),
        val vert: SbitLineMetrics = SbitLineMetrics(),
        val startGlyphIndex: Int = 0,
        val endGlyphIndex: Int = 0,
        val ppemX: Int = 0,
        val ppemY: Int = 0,
        val bitDepth: Int = 0,
        val flags: Int = 0,
    )

    //var colorBitmapInfos = listOf<ColorBitmapInfo>()

    data class SbitLineMetrics(
        val ascender: Int = 0,
        val descender: Int = 0,
        val widthMax: Int = 0,
        val caretSlopeNumerator: Int = 0,
        val caretSlopeDenominator: Int = 0,
        val caretOffset: Int = 0,
        val minOriginSB: Int = 0,
        val minAdvanceSB: Int = 0,
        val maxBeforeBL: Int = 0,
        val minAfterBL: Int = 0,
        val pad1: Int = 0,
        val pad2: Int = 0,
    )

    private fun FastByteArrayInputStream.readSibLineMetrics(): SbitLineMetrics = SbitLineMetrics(
        readS8(), readS8(), readU8(), readS8(), readS8(), readS8(),
        readS8(), readS8(), readS8(), readS8(), readS8(), readS8()
    )

    data class BitmapGlyphInfo(
        val glyphID: Int,
        val imageFormat: Int = 0,
        val offset: Int = 0,
        val size: Int = 0,
        val info: ColorBitmapInfo,
        var height: Int = 0,
        var width: Int = 0,
        var horiBearingX: Int = 0,
        var horiBearingY: Int = 0,
        var horiAdvance: Int = 0,
        var vertBearingX: Int = 0,
        var vertBearingY: Int = 0,
        var vertAdvance: Int = 0,
    ) {
        var s: FastByteArrayInputStream? = null
        var bitmap: Bitmap? = null

        val descender get() = info.hori.descender
        val ascender get() = info.hori.ascender

        fun getBitmap(cache: Boolean = false): Bitmap {
            val bmp = bitmap ?: PNG.decode(s!!.toSyncStream())
            if (cache) bitmap = bmp
            return bmp
        }
    }

    //fun ColorGlyphInfo.getBytes(): FastByteArrayInputStream = tablesByName["CBDT"]!!.open().sliceWithSize(offset, size)

    // https://docs.microsoft.com/en-us/typography/opentype/spec/eblc
    private fun readCblc() = runTableUnit("CBLC") { // Color Bitmap Location Table
        val majorVersion = readU16BE()
        val minorVersion = readU16BE()
        when (majorVersion) {
            3 -> {
                val numSizes = readS32BE()
                val colorBitmapInfos = Array(numSizes) {
                    ColorBitmapInfo(
                        readS32BE(),
                        readS32BE(),
                        readS32BE(),
                        readS32BE(),
                        readSibLineMetrics(),
                        readSibLineMetrics(),
                        readU16BE(),
                        readU16BE(),
                        readU8(),
                        readU8(),
                        readU8(),
                        readS8(),
                    )
                }.toList()

                //println("------------------")
                for (cbi in colorBitmapInfos) {
                    val glyphCount = cbi.endGlyphIndex - cbi.startGlyphIndex

                    data class Subtable(val firstGlyphIndex: Int, val lastGlyphIndex: Int, val additionalOffsetToIndexSubtable: Int) {
                        val numGlyphs get() = lastGlyphIndex - firstGlyphIndex
                    }

                    position = cbi.indexSubTableArrayOffset
                    val subtables = Array(cbi.numberofIndexSubTables) { Subtable(readU16BE(), readU16BE(), readS32BE()) }

                    for (subtable in subtables) {
                        position = cbi.indexSubTableArrayOffset + subtable.additionalOffsetToIndexSubtable
                        val indexFormat = readU16BE()
                        val imageFormat = readU16BE()
                        val imageDataOffset = readS32BE()
                        val offsets = when (indexFormat) {
                            1 -> Array(subtable.numGlyphs + 1) { imageDataOffset + readS32BE() }
                            else -> emptyArray()
                        }
                        for (n in 0 until offsets.size - 1) {
                            val offset = offsets[n]
                            val size = offsets[n + 1] - offset
                            val glyphID = subtable.firstGlyphIndex + n - 1
                            bitmapGlyphInfos[glyphID] = BitmapGlyphInfo(glyphID, imageFormat, offset, size, cbi)
                        }

                        //for (g in colorGlyphInfos.values) println("g=${g.glyphID}, ${g.offset}")
                        //for (n in 0 until )
                        //ColorGlyphInfo()
                        //println("offsets=${offsets.toList()}")
                    }
                    //println(subtables.map { it.numGlyphs })
                    //for (n in 0 until glyphCount) println(readS32BE())
                    //println("glyphCount=$glyphCount, bitDepth=${cbi.bitDepth}")
                }

                //println("CBLC, pos=$position, len=$length")
            }
            else -> {
                println("Unsupported CBLC $majorVersion.$minorVersion")
            }
        }
    }

    private fun readCbdt() = runTableUnit("CBDT") { // Color Bitmap Data Table
        //println("readCbdt")
        val majorVersion = readU16BE()
        val minorVersion = readU16BE()
        when (majorVersion) {
            3 -> {
                for (i in bitmapGlyphInfos.values) {
                    val format = i.imageFormat
                    when (format) {
                        17, 18 -> {
                            i.height = readU8()
                            i.width = readU8()
                            i.horiBearingX = readS8()
                            i.horiBearingY = readS8()
                            i.horiAdvance = readU8()
                            i.vertBearingX = if (format == 18) readS8() else i.horiBearingX
                            i.vertBearingY = if (format == 18) readS8() else i.horiBearingY
                            i.vertAdvance  = if (format == 18) readS8() else i.horiAdvance
                        }
                    }
                    val dataLen = readS32BE()
                    i.s = readSlice(dataLen)
                    //println(i.bytes!!.getAllBytes().hex)
                    //break
                }
                //println("bitmapGlyphInfos=$bitmapGlyphInfos")
            }
            else -> {
                println("Unsupported CBDT $majorVersion.$minorVersion")
            }
        }
    }

    // https://docs.microsoft.com/en-us/typography/opentype/spec/sbix
    private fun readSbix() = runTableUnit("sbix") { // Standard Bitmap Graphics Table
        val version = readU16BE()
        val flags = readU16BE()
        val numStrikes = readS32BE()
        val strikeOffsets = readIntArrayBE(numStrikes)
        data class StrikeInfo(val ppem: Int, val ppi: Int, val strikeOffset: Int, val offsets: IntArrayList)
        val strikes = strikeOffsets.map { strikeOffset ->
            position = strikeOffset
            StrikeInfo(ppem = readU16BE(), ppi = readU16BE(), strikeOffset = strikeOffset, offsets = readIntArrayBE(numGlyphs + 1).toIntArrayList())
        }
        // @TODO: Read all strikes and select right one
        val strike = strikes.maxByOrNull { it.ppem }!!
        for (n in 0 until numGlyphs) {
            val glyphID = n
            val offset = strike.offsets[n]
            val len = strike.offsets[n + 1] - offset
            val start = strike.strikeOffset + offset
            val end = start + len
            position = start
            val originOffsetX = readS16BE()
            val originOffsetY = readS16BE()
            val graphicType = readStringz(4)
            val bytes = readSlice(len - 8)
            //println("$n: originOffset=$originOffsetX,$originOffsetY, graphicType=$graphicType")

            // @TODO: Proper metrics
            bitmapGlyphInfos[glyphID] = BitmapGlyphInfo(glyphID, info = ColorBitmapInfo(
                ppemX = strike.ppem, ppemY = strike.ppem,
            )).also {
                it.s = bytes
            }
        }
    }

    fun FastByteArrayInputStream.readVarIdxBase(): Int = readS32BE()
    fun FastByteArrayInputStream.readUFWORD(): Int = readU16BE()
    fun FastByteArrayInputStream.readFWORD(): Int = readS16BE()
    fun FastByteArrayInputStream.readOffset16(): Int = readU16BE()
    fun FastByteArrayInputStream.readOffset24(): Int = readU24BE()
    fun FastByteArrayInputStream.readOffset32(): Int = readS32BE()
    fun FastByteArrayInputStream.readAffine2x3(isVar: Boolean, out: Matrix = Matrix()): Matrix {
        val xx = readFIXED3()
        val yx = readFIXED3()
        val xy = readFIXED3()
        val yy = readFIXED3()
        val dx = readFIXED3()
        val dy = readFIXED3()
        //println("readAffine2x3: $xx, $yx, $xy, $yy, $dx, $dy")
        out.setTo(
            xx.toDouble(),
            yx.toDouble(),
            xy.toDouble(),
            yy.toDouble(),
            dx.toDouble(),
            -dy.toDouble(),
        )
        //out.scale(1.0, -1.0)
        return out
    }
    fun FastByteArrayInputStream.readColorStop(out: ColorStop = ColorStop(0.0, 0, 0.0)): ColorStop {
        out.stopOffset = readF2DOT14().toDouble()
        out.paletteIndex = readU16BE()
        out.alpha = readF2DOT14().toDouble()
        return out
    }
    fun FastByteArrayInputStream.readClipBox(doVar: Boolean = false): Rectangle {
        val format = readU8()
        val xMin: Int = readFWORD()
        val yMin: Int = readFWORD()
        val xMax: Int = readFWORD()
        val yMax: Int = readFWORD()
        if (format == 2) {
            val varIndexBase = readVarIdxBase()
        }
        return Rectangle.fromBounds(xMin, yMin, xMax, yMax)
    }
    fun FastByteArrayInputStream.readBaseGlyphPaintRecord() {
        val glyphID = s.readU16BE()
        val paint = s.readOffset24() // <Paint>
        TODO()
    }
    fun FastByteArrayInputStream.readColorLine(): ColorLine {
        val cycle = when (readU8()) {
            0 -> CycleMethod.NO_CYCLE
            1 -> CycleMethod.REPEAT
            2 -> CycleMethod.REFLECT
            else -> CycleMethod.NO_CYCLE
        }
        return ColorLine(cycle, readArrayOf16 { readColorStop() }.toList())
    }
    data class ColorLine(val cycle: CycleMethod, val stops: List<ColorStop>) {
        fun addToPaint(paint: GradientPaint, palette: Palette) {
            for (stop in stops) {
                paint.addColorStop(stop.stopOffset, palette.colors[stop.paletteIndex].concatAd(stop.alpha))
            }
        }
    }

    inline fun <reified T> FastByteArrayInputStream.readArrayOf16(block: FastByteArrayInputStream.(index: Int) -> T): Array<T> =
        Array<T>(readU16BE()) { block(it) }
    inline fun <reified T> FastByteArrayInputStream.readArrayOf32(block: FastByteArrayInputStream.(index: Int) -> T): Array<T> =
        Array<T>(readS32BE()) { block(it) }

    class ColorStop(var stopOffset: Double, var paletteIndex: Int, var alpha: Double)

    // https://docs.microsoft.com/en-us/typography/opentype/spec/colr
    private fun interpretColrv1(glyphID: Int, s: FastByteArrayInputStream, c: Context2d, pal: Int, level: Int) {
        val nodeFormat = s.readU8()
        val isVar = nodeFormat % 2 == 1
        val indent = "  ".repeat(level)
        val debug = false
        if (debug) println("${indent}interpretColrv1[glyphID=$glyphID]: $nodeFormat - ${Colrv1Paint.BY_FORMAT.getOrNull(nodeFormat)}")
        when (nodeFormat) {
            1 -> { // PaintColrLayers
                val numLayers = s.readU8()
                val firstLayerIndex = s.readS32BE() // index into COLRv1::layerList
                for (n in 0 until numLayers) {
                    val paint = colrv1.layerList[firstLayerIndex + n]
                    interpretColrv1(glyphID, colrv1.sLayerOffset.sliceStart(paint), c, pal, level + 1)
                }
            }
            2, 3 -> { // PaintSolid, PaintVarSolid
                val paletteIndex = s.readU16BE()
                val alpha = s.readF2DOT14().toDouble()
                if (isVar) {
                    val varIndexBase = s.readVarIdxBase()
                    TODO()
                }
                val color = palettes[pal].colors[paletteIndex].concatAd(alpha)
                if (debug) println("${indent}$color")
                c.fill(color)
            }
            4, 5 -> { // PaintLinearGradient, PaintVarLinearGradient
                val colorLineOffset = s.readOffset24() // <ColorLine>
                val x0 = s.readFWORD().toDouble()
                val y0 = s.readFWORD().toDouble()
                val x1 = s.readFWORD().toDouble()
                val y1 = s.readFWORD().toDouble()
                // @TODO: WTF is this x2 and y2? LOL
                val x2 = s.readFWORD().toDouble() // Normal; Equal to (x1,y1) in simple cases.
                val y2 = s.readFWORD().toDouble()
                val colorLine = s.sliceStart(colorLineOffset).readColorLine()
                if (isVar) {
                    val varIndexBase = s.readVarIdxBase()
                }
                val paint = LinearGradientPaint(x0, -y0, x1, -y1, colorLine.cycle)
                colorLine.addToPaint(paint, palettes[pal])
                if (debug) println("${indent}$paint")
                c.fill(paint)

            }
            6, 7 -> { // PaintRadialGradient, PaintVarRadialGradient
                val colorLineOffset = s.readOffset24() // <ColorLine>
                val x0 = s.readFWORD().toDouble()
                val y0 = s.readFWORD().toDouble()
                val radius0 = s.readUFWORD().toDouble()
                val x1 = s.readFWORD().toDouble()
                val y1 = s.readFWORD().toDouble()
                val radius1 = s.readUFWORD().toDouble()
                val colorLine = s.sliceStart(colorLineOffset).readColorLine()
                if (isVar) {
                    val varIndexBase = s.readVarIdxBase()
                }
                val paint = RadialGradientPaint(x0, -y0, radius0, x1, -y1, radius1, colorLine.cycle)
                colorLine.addToPaint(paint, palettes[pal])
                if (debug) println("${indent}$paint")
                c.fill(paint)
            }
            8, 9 -> { // PaintSweepGradient, PaintVarSweepGradient
                val colorLine = s.readOffset24() // <ColorLine>
                val centerX = s.readFWORD()
                val centerY = s.readFWORD()
                val startAngle = s.readF2DOT14() // 180° in counter-clockwise degrees per 1.0 of value
                val endAngle = s.readF2DOT14() // 180° in counter-clockwise degrees per 1.0 of value
                if (isVar) {
                    val varIndexBase = s.readVarIdxBase()
                }
                TODO()
            }
            10 -> { // PaintGlyph
                val paint = s.readOffset24() // <Paint>
                val glyphID = s.readU16BE()
                val colr = colrGlyphInfos[glyphID]
                val glyph = this.getGlyphByIndex(glyphID)
                c.keep {
                    if (glyph != null) {
                        c.path(glyph.path.path)
                        interpretColrv1(glyphID, s.sliceStart(paint), c, pal, level + 1)
                    }
                }
            }
            11 -> { // PaintColrGlyph
                val glyphID = s.readU16BE()
                TODO()
            }
            12, 13 -> { // PaintTransform, PaintVarTransform
                val paint = s.readOffset24() // <Paint>
                val transform = s.readOffset24() // <Affine2x3> <VarAffine2x3>
                val affine = s.sliceStart(transform).readAffine2x3(isVar)
                if (debug) println("${indent}affine=$affine")
                c.keep {
                    //c.translate(affine.tx, affine.ty)
                    //affine.ty = -affine.ty
                    c.transform(affine)
                    interpretColrv1(glyphID, s.sliceStart(paint), c, pal, level + 1)
                }
            }
            14, 15 -> { // PaintTranslate, PaintVarTranslate
                val paint = s.readOffset24() // <Paint>
                val dx = s.readFWord().toDouble()
                val dy = s.readFWord().toDouble()
                if (isVar) {
                    val varIndexBase = s.readVarIdxBase()
                }
                c.keep {
                    if (debug) println("translate: $dx, $dy")
                    c.translate(dx, -dy)
                    //println(this.unitsPerEm)
                    interpretColrv1(glyphID, s.sliceStart(paint), c, pal, level + 1)
                }
            }
            16, 17, 18, 19 -> { // PaintScale, PaintVarScale, PaintScaleAroundCenter, PaintVarScaleAroundCenter
                val paint = s.readOffset24() // <Paint>
                val scaleX = s.readF2DOT14()
                val scaleY = s.readF2DOT14()
                val aroundCenter = nodeFormat >= 18
                if (aroundCenter) {
                    val centerX = s.readFWORD()
                    val centerY = s.readFWORD()
                    TODO()
                }
                if (isVar) {
                    val varIndexBase = s.readVarIdxBase()
                }
                c.keep {
                    c.scale(scaleX, scaleY)
                    interpretColrv1(glyphID, s.sliceStart(paint), c, pal, level + 1)
                }
            }
            20, 21, 22, 23 -> { // PaintScaleUniform, PaintVarScaleUniform, PaintScaleUniformAroundCenter, PaintVarScaleUniformAroundCenter
                val paint = s.readOffset24() // <Paint>
                val scale = s.readF2DOT14()
                if (nodeFormat >= 22) {
                    val centerX = s.readFWORD()
                    val centerY = s.readFWORD()
                }
                if (nodeFormat % 2 == 1) {
                    val varIndexBase = s.readVarIdxBase()
                }
                TODO()
            }
            24, 25, 26, 27 -> { // PaintRotate, PaintVarRotate, PaintRotateAroundCenter, PaintVarRotateAroundCenter
                val paint = s.readOffset24() // <Paint>
                val angle = s.readF2DOT14()
                if (nodeFormat >= 26) {
                    val centerX = s.readFWORD()
                    val centerY = s.readFWORD()
                }
                if (nodeFormat % 2 == 1) {
                    val varIndexBase = s.readVarIdxBase()
                }
                TODO()
            }
            28, 29, 30, 31 -> { // PaintSkew, PaintVarSkew, PaintSkewAroundCenter, PaintVarSkewAroundCenter
                val paint = s.readOffset24() // <Paint>
                val xSkewAngle = s.readF2DOT14()
                val ySkewAngle = s.readF2DOT14()
                if (nodeFormat >= 30) {
                    val centerX = s.readFWORD()
                    val centerY = s.readFWORD()
                }
                if (nodeFormat % 2 == 1) {
                    val varIndexBase = s.readVarIdxBase()
                }
                TODO()
            }
            32 -> { // Paint Composite
                val sourcePaint = s.readOffset24() // <Paint>
                val compositeMode = s.readU8() // CompositeMode
                val backdropPaint = s.readOffset24() // <Paint>
                TODO()
            }
            else -> {
                TODO("Unknown colorv1 format=$nodeFormat")
            }
        }
    }

    enum class Colrv1Paint {
        PaintColrLayers,
        PaintSolid,
        PaintVarSolid,
        PaintLinearGradient,
        PaintVarLinearGradient,
        PaintRadialGradient,
        PaintVarRadialGradient,
        PaintSweepGradient,
        PaintVarSweepGradient,
        PaintGlyph,
        PaintColrGlyph,
        PaintTransform,
        PaintVarTransform,
        PaintTranslate,
        PaintVarTranslate,
        PaintScale,
        PaintVarScale,
        PaintScaleAroundCenter,
        PaintVarScaleAroundCenter,
        PaintScaleUniform,
        PaintVarScaleUniform,
        PaintScaleUniformAroundCenter,
        PaintVarScaleUniformAroundCenter,
        PaintRotate,
        PaintVarRotate,
        PaintRotateAroundCenter,
        PaintVarRotateAroundCenter,
        PaintSkew,
        PaintVarSkew,
        PaintSkewAroundCenter,
        PaintVarSkewAroundCenter,
        PaintComposite;
        val format: Int = ordinal + 1

        companion object {
            val BY_FORMAT = arrayOfNulls<Colrv1Paint>(1) + values()
        }
    }

    object CompositeModes {
        // Porter-Duff modes
        // https://www.w3.org/TR/compositing-1/#porterduffcompositingoperators
        const val COMPOSITE_CLEAR          = 0  // https://www.w3.org/TR/compositing-1/#porterduffcompositingoperators_clear
        const val COMPOSITE_SRC            = 1  // https://www.w3.org/TR/compositing-1/#porterduffcompositingoperators_src
        const val COMPOSITE_DEST           = 2  // https://www.w3.org/TR/compositing-1/#porterduffcompositingoperators_dst
        const val COMPOSITE_SRC_OVER       = 3  // https://www.w3.org/TR/compositing-1/#porterduffcompositingoperators_srcover
        const val COMPOSITE_DEST_OVER      = 4  // https://www.w3.org/TR/compositing-1/#porterduffcompositingoperators_dstover
        const val COMPOSITE_SRC_IN         = 5  // https://www.w3.org/TR/compositing-1/#porterduffcompositingoperators_srcin
        const val COMPOSITE_DEST_IN        = 6  // https://www.w3.org/TR/compositing-1/#porterduffcompositingoperators_dstin
        const val COMPOSITE_SRC_OUT        = 7  // https://www.w3.org/TR/compositing-1/#porterduffcompositingoperators_srcout
        const val COMPOSITE_DEST_OUT       = 8  // https://www.w3.org/TR/compositing-1/#porterduffcompositingoperators_dstout
        const val COMPOSITE_SRC_ATOP       = 9  // https://www.w3.org/TR/compositing-1/#porterduffcompositingoperators_srcatop
        const val COMPOSITE_DEST_ATOP      = 10  // https://www.w3.org/TR/compositing-1/#porterduffcompositingoperators_dstatop
        const val COMPOSITE_XOR            = 11  // https://www.w3.org/TR/compositing-1/#porterduffcompositingoperators_xor
        const val COMPOSITE_PLUS           = 12  // https://www.w3.org/TR/compositing-1/#porterduffcompositingoperators_plus

        // Blend modes
        // https://www.w3.org/TR/compositing-1/#blending
        const val COMPOSITE_SCREEN         = 13  // https://www.w3.org/TR/compositing-1/#blendingscreen
        const val COMPOSITE_OVERLAY        = 14  // https://www.w3.org/TR/compositing-1/#blendingoverlay
        const val COMPOSITE_DARKEN         = 15  // https://www.w3.org/TR/compositing-1/#blendingdarken
        const val COMPOSITE_LIGHTEN        = 16  // https://www.w3.org/TR/compositing-1/#blendinglighten
        const val COMPOSITE_COLOR_DODGE    = 17  // https://www.w3.org/TR/compositing-1/#blendingcolordodge
        const val COMPOSITE_COLOR_BURN     = 18  // https://www.w3.org/TR/compositing-1/#blendingcolorburn
        const val COMPOSITE_HARD_LIGHT     = 19  // https://www.w3.org/TR/compositing-1/#blendinghardlight
        const val COMPOSITE_SOFT_LIGHT     = 20  // https://www.w3.org/TR/compositing-1/#blendingsoftlight
        const val COMPOSITE_DIFFERENCE     = 21  // https://www.w3.org/TR/compositing-1/#blendingdifference
        const val COMPOSITE_EXCLUSION      = 22  // https://www.w3.org/TR/compositing-1/#blendingexclusion
        const val COMPOSITE_MULTIPLY       = 23  // https://www.w3.org/TR/compositing-1/#blendingmultiply

        // Modes that, uniquely, do not operate on components
        // https://www.w3.org/TR/compositing-1/#blendingnonseparable
        const val COMPOSITE_HSL_HUE        = 24  // https://www.w3.org/TR/compositing-1/#blendinghue
        const val COMPOSITE_HSL_SATURATION = 25  // https://www.w3.org/TR/compositing-1/#blendingsaturation
        const val COMPOSITE_HSL_COLOR      = 26  // https://www.w3.org/TR/compositing-1/#blendingcolor
        const val COMPOSITE_HSL_LUMINOSITY = 27  // https://www.w3.org/TR/compositing-1/#blendingluminosity
    }

    data class Clip(
        val startGlyphID: Int,
        val endGlyphID: Int,
        val clipBoxOffset: Int,
    )

    class COLRv1(
        var layerList: IntArray = intArrayOf(),
        var s: FastByteArrayInputStream = FastByteArrayInputStream(byteArrayOf()),
        var sBaseOffset: FastByteArrayInputStream = FastByteArrayInputStream(byteArrayOf()),
        var sLayerOffset: FastByteArrayInputStream = FastByteArrayInputStream(byteArrayOf()),
        var sClipOffset: FastByteArrayInputStream? = null,
        //var clipList: List<Clip>? = null,
    ) {
        val glyphIDToClipOffset = IntIntMap()
    }



    private fun readGsub() = runTableUnit("GSUB") {
        val totalLength = length
        val majorVersion = readU16BE()
        val minorVersion = readU16BE()
        if (majorVersion != 1) error("Unknown GSUB majorVerrsion=$majorVersion")
        val scriptListOffset = readOffset16()
        val featureListOffset = readOffset16()
        val lookupListOffset = readOffset16()
        if (minorVersion >= 1) {
            val featureVariationsOffset = readOffset32()
        }
        //println("GSUB: $majorVersion,$minorVersion, totalLength=$totalLength, scriptListOffset=$scriptListOffset, featureListOffset=$featureListOffset, lookupListOffset=$lookupListOffset")
        sliceStart(scriptListOffset).apply {
            val scriptCount = readU16BE()
            //println("scriptCount=$scriptCount")
            for (n in 0 until scriptCount) {
                val scriptTag = readStringz(4)
                val scriptOffset = readOffset16()
                //println("script[$scriptTag]=$scriptOffset")
            }
        }
        sliceStart(featureListOffset).apply {
            val featureCount = readU16BE()
            //println("featureCount=$featureCount")
            for (n in 0 until featureCount) {
                val featureTag = readStringz(4)
                val featureOffset = readOffset16()
                //println("feature[$featureTag]=$featureOffset")
            }
        }

        fun FastByteArrayInputStream.readCoverage(count: Int): IntArray {
            val coverageFormat = readU16BE()
            when (coverageFormat) {
                1 -> {
                    val glyphCount = readU16BE()
                    return readCharArrayBE(glyphCount).mapInt { it.code }
                    //println("glyphArray[${glyphArray.size}]=${glyphArray.toList()}")
                }
                2 -> {
                    val glyphArray = IntArray(count)
                    val rangeCount = readU16BE()
                    repeat(rangeCount) {
                        val startGlyphID = readU16BE()
                        val endGlyphID = readU16BE()
                        val startCoverageIndex = readU16BE()
                        for (glyphID in startGlyphID..endGlyphID) {
                            val offset = glyphID - startGlyphID
                            glyphArray[startCoverageIndex + offset] = glyphID
                        }
                        //println("startGlyphID=$startGlyphID, endGlyphID=$endGlyphID, startCoverageIndex=$startCoverageIndex")
                    }
                    return glyphArray
                    //println("UNSUPPORTED coverageFormat=$coverageFormat")
                    //return@runTableUnit
                }
                else -> TODO("coverageFormat=$coverageFormat")
            }
        }

        // https://docs.microsoft.com/en-us/typography/opentype/spec/chapter2#lulTbl
        sliceStart(lookupListOffset).apply {
            val lookupCount = readU16BE()
            //println("lookupCount=$lookupCount")
            for (n in 0 until lookupCount) {
                val lookupOffset = readOffset16()
                //println("lookup=$lookupOffset")
                sliceStart(lookupOffset).apply {
                    val lookupType = readU16BE()
                    val lookupFlag = readU16BE()
                    val subTableCount = readU16BE()
                    val subtableOffsets = readShortArrayBE(subTableCount).mapInt { it.toInt() and 0xFFFF }
                    val markFilteringSet = readU16BE()
                    //println("   - lookupType=$lookupType, lookupFlag=$lookupFlag, subTableCount=$subTableCount")
                    for (offset in subtableOffsets) {
                        sliceStart(offset).apply {
                            val subsTable = this
                            when (lookupType) {
                                4 -> { // https://docs.microsoft.com/en-us/typography/opentype/spec/gsub#LS
                                    val substFormat = readU16BE()
                                    val coverageOffset = readOffset16() // https://docs.microsoft.com/en-us/typography/opentype/spec/chapter2#coverage-table
                                    val ligatureSetCount = readU16BE()
                                    val ligatureSetOffsets = readCharArrayBE(ligatureSetCount).mapInt { it.code }
                                    val glyphArray = sliceStart(coverageOffset).readCoverage(ligatureSetCount)
                                    //println("   - substFormat=$substFormat")
                                    //println("   - coverageOffset=$coverageOffset")
                                    //println("   - ligatureSetCount=$ligatureSetCount")
                                    val coverageTable = sliceStart(coverageOffset)
                                    val glyphIDs = sliceStart(coverageOffset).let {
                                        it.readCharArrayBE(ligatureSetCount).mapInt { it.code }
                                    }
                                    //println("glyphIDs=${glyphIDs.toList()}")
                                    //println("----")
                                    for (n in ligatureSetOffsets.indices) {
                                        val glyphID = glyphArray[n]
                                        val asetoffset = ligatureSetOffsets[n]
                                        //val map = LinkedHashMap<List<Int>, List<Int>>()
                                        val map = LinkedHashMap<WString, IntArray>()
                                        val codePoint = getCodePointFromCharIndexOrElse(glyphID)
                                        val codePointIntArray = intArrayOf(codePoint)
                                        var maxComponentCount = 1
                                        //substitutionsGlyphIds[glyphID] = map
                                        subsTable.sliceStart(asetoffset).apply {
                                            val ligatureCount = readU16BE()
                                            val ligatureOffsets = readCharArrayBE(ligatureCount).mapInt { it.code }
                                            //println("      - $ligatureCount: ${ligatureOffsets.toList()}")
                                            for (offset in ligatureOffsets) {
                                                sliceStart(offset).apply {
                                                    val ligatureGlyph = readU16BE()
                                                    val componentCount = readU16BE()
                                                    maxComponentCount = kotlin.math.max(maxComponentCount, componentCount + 1)
                                                    val componentCodePoints = readCharArrayBE(componentCount - 1).mapInt { getCodePointFromCharIndexOrElse(it.code) }
                                                    val ligature = WString(codePointIntArray + componentCodePoints)
                                                    map[ligature] = intArrayOf(ligatureGlyph)

                                                    //println("            - ${listOf(glyphID) + componentGlyphIDs.toList()} -> $ligatureGlyph")
                                                }
                                            }
                                        }
                                        substitutionsCodePoints[codePoint] = SubstitutionInfo(maxComponentCount, map)
                                    }
                                }
                                else -> {
                                    println("TTF: Unsupported lookupType=$lookupType")
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun readColr() = runTableUnit("COLR") {
        val version = readU16BE()
        when (version) {
            0, 1 -> { // COLRv0, COLRv1
                val numBaseGlyphRecords = readU16BE()
                val baseGlyphRecordsOffset = readS32BE()
                val layerRecordsOffset = readS32BE()
                val numLayerRecords = readU16BE()
                //println("numBaseGlyphRecords=$numBaseGlyphRecords")
                //println("baseGlyphRecordsOffset=$baseGlyphRecordsOffset")
                //println("layerRecordsOffset=$layerRecordsOffset")
                //println("numLayerRecords=$numLayerRecords")
                sliceStart(layerRecordsOffset).apply {
                    colrv0LayerInfos = Array(numLayerRecords) { ColrLayerInfo(readU16BE(), readU16BE()) }
                }
                sliceStart(baseGlyphRecordsOffset).apply {
                    for (n in 0 until numBaseGlyphRecords) {
                        val info = ColrGlyphInfoV0(readU16BE(), readU16BE(), readU16BE())
                        colrGlyphInfos[info.glyphID] = info
                        //println("- $glyphID = $firstLayerIndex / $numLayers")
                    }
                }
                if (version == 1) {
                    val baseGlyphListOffset = readOffset32() // <BaseGlyphList>
                    val layerListOffset = readOffset32() // <LayerList>
                    val clipListOffset = readOffset32() // <ClipList>. May be NULL
                    val varIdxMapOffset = readOffset32() // <VarIdxMap>. May be NULL
                    val varStoreOffset = readOffset32() // <ItemVariationStore>

                    //println("clipListOffset=$clipListOffset")
                    //println("varIdxMapOffset=$varIdxMapOffset")
                    //println("varStoreOffset=$varStoreOffset")

                    colrv1.glyphIDToClipOffset.clear()
                    colrv1 = COLRv1(
                        s = sliceStart(0),
                        layerList = sliceStart(layerListOffset).readArrayOf32 { readOffset32() }.toIntArray(),
                        sBaseOffset = this.sliceStart(baseGlyphListOffset),
                        sLayerOffset = this.sliceStart(layerListOffset),
                        sClipOffset = clipListOffset.takeIf { it != 0 }?.let { this.sliceStart(it) },
                        //val varIdxMap = sliceStart(varIdxMapOffset).readArrayOf32 { readOffset32() }
                        //val varStore = sliceStart(varStoreOffset).readArrayOf32 { readOffset32() }
                    )

                    clipListOffset.takeIf { it != 0 }?.let {
                        val s = sliceStart(clipListOffset)
                        val format = s.readU8()
                        s.readArrayOf32 {
                            val startGlyphID = readU16BE() // first gid clip applies to
                            val endGlyphID = readU16BE() // last gid clip applies to, inclusive
                            val clipBoxOffset = readOffset24()
                            for (glyphID in startGlyphID..endGlyphID) {
                                colrv1.glyphIDToClipOffset[glyphID] = clipBoxOffset
                            }
                            Unit
                            //Clip(startGlyphID, endGlyphID, clipBoxOffset)
                        }//.toList()
                    }

                    sliceStart(baseGlyphListOffset).readArrayOf32 {
                        val glyphID: Int = readU16BE()
                        val paint: Int = readOffset32()
                        //println(colrv1.s.sliceStart(paint).readU8())
                        colrGlyphInfos[glyphID] = ColrGlyphInfoV1(glyphID, paint)
                        Unit
                    }

                    //println("baseGlyphList[${colrv1.baseGlyphList.size}]=${colrv1.baseGlyphList}")
                    //println("layerList[${colrv1.layerList.size}]=${colrv1.layerList}")
                    //println("clipList[${clipList.size}]=$clipList")
                    //println("varIdxMap[${varIdxMap.size}]=$varIdxMap")
                    //println("varStore[${varStore.size}]=$varStore")
                }
            }
            else -> {
                println("TTF WARNING CCOLv$version not supported")
            }
        }
    }

	private fun readCmap() = runTableUnit("cmap") {
		data class EncodingRecord(val platformId: Int, val encodingId: Int, val offset: Int)

		val version = readU16BE()
		val numTables = readU16BE()
		val tables = (0 until numTables).map { EncodingRecord(readU16BE(), readU16BE(), readS32BE()) }

        var index: Int = 0

        for (table in tables) {
			sliceStart(table.offset).run {
				val format = readU16BE()
                //println("TABLE FORMAT[${this@TtfFont}]: $format")
				when (format) {
				    0 -> { // Byte encoding table
                        println("UNSUPPORTED CMAP format = $format")
				    }
                    2 -> { // High-byte mapping through table
                        println("UNSUPPORTED CMAP format = $format")
                    }
					4 -> { // Segment mapping to delta values
						val length = readU16BE()
						//s.readStream(length - 4).run {
						val language = readU16BE()
						val segCount = readU16BE() / 2
						val searchRangeS = readU16BE()
						val entrySelector = readU16BE()
						val rangeShift = readU16BE()
						val endCount = readCharArrayBE(segCount)
						readU16BE() // reserved
						val startCount = readCharArrayBE(segCount)
						val idDelta = readShortArrayBE(segCount)
						val rangeOffsetPos = position.toInt()
						val idRangeOffset = readCharArrayBE(segCount)
						//val glyphIdArray = readCharArrayBE(idRangeOffset.maxOrNull()?.toInt() ?: 0)

						//println("$language")

						for (n in 0 until segCount) {
							val ec = endCount[n].toInt()
							val sc = startCount[n].toInt()
							val delta = idDelta[n].toInt()
							val iro = idRangeOffset[n].toInt()
							//println("%04X-%04X : %d : %d".format(sc, ec, delta, iro))
							for (c in sc..ec) {
								if (iro != 0) {
									var glyphIndexOffset = rangeOffsetPos + n * 2
									glyphIndexOffset += iro
									glyphIndexOffset += (c - sc) * 2
									index = sliceStart(glyphIndexOffset).readU16BE()
									if (index != 0) {
										index += delta
									}
								} else {
									index = c + delta
								}
                                //index = index and 0xFFFF
                                addCharacterMap(c, index and 0xFFFF)
								//println("%04X --> %d".format(c, index and 0xFFFF))
							}
						}

						//for ((c, index) in characterMaps) println("\\u%04X -> %d".format(c.toInt(), index))
					}
                    6 -> { // Trimmed table mapping
                        val length = readU16BE()
                        val language = readU16BE()
                        val firstCode = readU16BE()
                        val entryCount = readU16BE()
                        for (n in 0 until entryCount) {
                            val codePoint = firstCode + n
                            //if (codePoint in characterMaps) println("For codePoint=$codePoint, old=${characterMaps[codePoint]}, new=$n")
                            addCharacterMap(codePoint, readU16BE())
                        }
                    }
                    8 -> { // mixed 16-bit and 32-bit coverage
                        println("UNSUPPORTED CMAP format = $format")
                    }
                    10 -> { // Trimmed array
                        println("UNSUPPORTED CMAP format = $format")
                    }
					12 -> { // Segmented coverage
						readU16BE() // reserved
						val length = readS32BE()
						val language = readS32BE()
						val numGroups = readS32BE()

						for (n in 0 until numGroups) {
							val startCharCode = readS32BE()
							val endCharCode = readS32BE()
							val startGlyphId = readS32BE()

							for (c in startCharCode..endCharCode) {
                                val m = c - startCharCode
                                addCharacterMap(c, startGlyphId + m)
                                //println(" - $c -> $glyphId")
							}
						}
					}
                    13 -> { // Many-to-one range mappings
                        println("UNSUPPORTED CMAP format = $format")
                    }
                    //14 -> {
                    //    println("UNSUPPORTED CMAP format = $format")
                    //}
                    //14 -> { // Unicode Variation Sequences
                    //    val length = readS32BE()
                    //    val numVarSelectorRecords = readS32BE()
                    //    data class VarSelectorRecord(val varSelector: Int, val defaultUVSOffset: Int, val nonDefaultUVSOffset: Int)
                    //    val records = Array(numVarSelectorRecords) { VarSelectorRecord(readU24BE(), readS32BE(), readS32BE()) }
                    //    var glyphId = 0
                    //    for (record in records) {
                    //        run {
                    //            position = record.defaultUVSOffset
                    //            val numUnicodeValueRanges = readS32BE()
                    //            for (n in 0 until numUnicodeValueRanges) {
                    //                val startUnicodeValue = readU24BE()
                    //                val additionalCount = readU8()
                    //                val count = 1 + additionalCount
                    //                //println("startUnicodeValue=$startUnicodeValue, additionalCount=$additionalCount")
                    //                for (m in 0 until count) {
                    //                    //addCharacterMap(startUnicodeValue + m, glyphId++)
                    //                }
                    //            }
                    //        }
                    //        run {
                    //            position = record.nonDefaultUVSOffset
                    //            val numUVSMappings = readS32BE()
                    //            for (n in 0 until numUVSMappings) {
                    //                val unicodeValue = readU24BE()
                    //                val glyphID = readU16BE()
                    //                //println("startUnicodeValue=$unicodeValue, glyphID=$glyphID")
                    //            }
                    //        }
                    //    }
                    //    //println(characterMaps)
                    //}
					else -> { // Ignored
                        println("UNSUPPORTED CMAP format = $format")
					}
				}
				//println("cmap.table.format: $format")
			}
		}
        //println("${this@TtfFont.name}: $characterMaps")

        //println(tables)
	}

    fun getCodePointFromCharIndexOrElse(charIndex: Int, default: Int = -1): Int = characterMapsReverse.getOrElse(charIndex) { default }
    fun getCodePointFromCharIndex(charIndex: Int): Int? = characterMapsReverse[charIndex]
    fun getCharIndexFromCodePoint(codePoint: Int): Int? = getCharacterMapOrNull(codePoint)
    fun getCharIndexFromChar(char: Char): Int? = getCharacterMapOrNull(char.code)
    fun getCharIndexFromWChar(char: WChar): Int? = getCharacterMapOrNull(char.code)

    fun getGlyphByCodePoint(codePoint: Int, cache: Boolean = true): Glyph? = getCharacterMapOrNull(codePoint)?.let { getGlyphByIndex(it, cache, codePoint) }
    fun getGlyphByWChar(char: WChar, cache: Boolean = true): Glyph? = getGlyphByCodePoint(char.code, cache)
    fun getGlyphByChar(char: Char, cache: Boolean = true): Glyph? = getGlyphByCodePoint(char.code, cache)

    operator fun get(char: Char) = getGlyphByChar(char)
    operator fun get(codePoint: Int) = getGlyphByCodePoint(codePoint)
    operator fun get(codePoint: WChar) = getGlyphByCodePoint(codePoint.code)

    fun getGlyphByIndex(index: Int, cache: Boolean = true, codePoint: Int = -1): Glyph? {
        //val finalCodePoint = if (codePoint < 0) getCodePointFromCharIndex(index) ?: -1 else codePoint
        val start = locs.getOrNull(index) ?: 0
        val end = locs.getOrNull(index + 1) ?: start
        val size = end - start
        //println("GLYPH INDEX: $index")
        val glyph = when {
            size != 0 -> this.glyphCache[index] ?: runTable("glyf") { table ->
                //println("READ GLYPH[$index]: start=$start, end=$end, size=$size, table=$table")
                sliceStart(start).readGlyph(index).also {
                    //it.codePoint = finalCodePoint
                }
            }
            else -> {
                //println("EMPTY GLYPH: SIZE: $size, index=$index")
                SimpleGlyph(index, 0, 0, 0, 0, intArrayOf(), intArrayOf(), intArrayOf(), intArrayOf(), horMetrics[index].advanceWidth).also {
                    //it.codePoint = finalCodePoint
                }
            }
        }
        if (cache && !frozen) this.glyphCache[index] = glyph
        return glyph
    }

    fun getAllGlyphs(cache: Boolean = false) = (0 until numGlyphs).mapNotNull { getGlyphByIndex(it, cache) }

    private val nonExistantGlyphMetrics1px = GlyphMetrics(1.0, false, 0, Rectangle(), 0.0)

    private data class Contour(var x: Int = 0, var y: Int = 0, var onCurve: Boolean = false) {
		fun copyFrom(that: Contour) {
			this.x = that.x
			this.y = that.y
			this.onCurve = that.onCurve
		}
	}

    data class GlyphReference(
        val glyph: Glyph,
        val x: Int, val y: Int,
        val scaleX: Float,
        val scale01: Float,
        val scale10: Float,
        val scaleY: Float
	)

    inner class ColrLayerInfo(val glyphID: Int, val paletteIndex: Int) {
        fun color(pal: Int) = palettes.getCyclic(pal).colors.getOrElse(paletteIndex) { Colors.FUCHSIA }
        val color: RGBA get() = color(0)
        fun getColorPath(pal: Int = 0): FillShape? = getGlyphByIndex(glyphID)?.path?.let { FillShape(it.path, null, color(pal)) }
        override fun toString(): String = "ColrLayerInfo($glyphID, $paletteIndex, $color)"
    }

    interface ColrGlyphInfo {
        fun getColorPaths(pal: Int = 0): List<Shape> = listOf(getColorPath(pal))
        fun getColorPath(pal: Int = 0): Shape
    }

    inner class ColrGlyphInfoV1(val glyphID: Int, val paint: Int) : ColrGlyphInfo {
        override fun getColorPath(pal: Int): Shape {
            return buildShape {
                val paintS = colrv1.sBaseOffset.sliceStart(paint)
                //println(this.state.transform)
                try {
                    interpretColrv1(glyphID, paintS, this, pal, 0)
                } catch (e: Throwable) {
                    e.printStackTrace()
                }
            }.also { shape ->
                //println("ColrGlyphInfoV1.getColorPath($pal): $shape")
                //println("ColrGlyphInfoV1.getColorPath($pal):\n${shape.toSvg().toOuterXmlIndentedString()}")
            }
        }
        override fun toString(): String = "ColrGlyphInfoV1[$glyphID][$paint]"
    }

    inner class ColrGlyphInfoV0(val glyphID: Int, val firstLayerIndex: Int, val numLayers: Int) : ColrGlyphInfo {
        operator fun get(index: Int) = colrv0LayerInfos[firstLayerIndex + index]
        fun toList() = Array(numLayers) { this[it] }.toList()

        override fun getColorPaths(pal: Int): List<FillShape> = toList().mapNotNull { it.getColorPath(pal) }
        override fun getColorPath(pal: Int): Shape = CompoundShape(getColorPaths(pal))
        override fun toString(): String = "ColrGlyphInfoV0[$glyphID][$numLayers](${toList()})"
    }

    data class GlyphGraphicsPath(
        val glpyhIndex: Int,
        val path: VectorPath
    ) : IVectorPath by path {
        override fun toString(): String = "GraphicsPath(glpyhIndex=$glpyhIndex, \"${this.path.toSvgPathString()}\")"
    }

    abstract inner class Glyph(
        val index: Int,
        val xMin: Int, val yMin: Int,
        val xMax: Int, val yMax: Int,
        val advanceWidth: Int
    ) {
        abstract val path: GlyphGraphicsPath
        abstract val paths: List<GlyphGraphicsPath>

        //val colorEntry get() = colrGlyphInfos[codePoint]
        val colorEntry: ColrGlyphInfo? get() = colrGlyphInfos[index]
        val bitmapEntry: BitmapGlyphInfo? get() = bitmapGlyphInfos[index]

        val metrics1px = run {
            var xMin = xMin
            var yMin = yMin
            var xMax = xMax
            var yMax = yMax

            if (xMin == 0 && yMin == 0 && xMax == 0 && yMax == 0) {
                val sClipOffset = colrv1.sClipOffset
                val glyphIDToClipOffset = colrv1.glyphIDToClipOffset
                val bounds = if (sClipOffset != null && glyphIDToClipOffset.contains(index)) {
                    val clipOffset = glyphIDToClipOffset[index]
                    sClipOffset.sliceStart(clipOffset).readClipBox()
                } else {
                    colorEntry?.getColorPath()?.bounds
                }
                if (bounds != null) {
                    xMin = bounds.left.toInt()
                    xMax = bounds.right.toInt()
                    yMin = bounds.top.toInt()
                    yMax = bounds.bottom.toInt()
                }
            }

            val size = unitsPerEm.toDouble()
            val scale = getTextScale(size)
            GlyphMetrics(size, true, -1, Rectangle.fromBounds(
                xMin * scale, yMin * scale,
                xMax * scale, yMax * scale
            ), advanceWidth * scale)
        }
    }

    inner class CompositeGlyph(
        index: Int,
        xMin: Int, yMin: Int,
        xMax: Int, yMax: Int,
        val refs: List<GlyphReference>,
        advanceWidth: Int
	) : Glyph(index, xMin, yMin, xMax, yMax, advanceWidth) {
        override fun toString(): String = "CompositeGlyph[$advanceWidth](${refs})"

        override val paths = refs.map { ref ->
            val gpath = ref.glyph.path.path
            GlyphGraphicsPath(ref.glyph.index, VectorPath(IntArrayList(gpath.commands.size), DoubleArrayList(gpath.data.size))).also { out ->
                val m = Matrix()
                m.translate(ref.x, -ref.y)
                m.scale(ref.scaleX, ref.scaleY)
                out.path.write(ref.glyph.path.path, m)
            }
        }

        // @TODO: Do not use by lazy, since this causes a crash on Kotlin/Native
        override val path: GlyphGraphicsPath = run {
            var commandsSize = 0
            var dataSize = 0

            paths.fastForEach { gpath ->
                commandsSize += gpath.path.commands.size
                dataSize += gpath.path.data.size
            }

            GlyphGraphicsPath(index, VectorPath(IntArrayList(commandsSize), DoubleArrayList(dataSize))).also { out ->
                paths.fastForEach { out.path.write(it.path) }
            }
        }
    }

    inner class SimpleGlyph(
        index: Int,
		xMin: Int, yMin: Int,
		xMax: Int, yMax: Int,
		val contoursIndices: IntArray,
		val flags: IntArray,
		val xPos: IntArray,
		val yPos: IntArray,
		advanceWidth: Int
	) : Glyph(index, xMin, yMin, xMax, yMax, advanceWidth) {
        override fun toString(): String = "SimpleGlyph[$advanceWidth]($index) : $path"
        val npoints: Int get() = xPos.size
        private fun onCurve(n: Int) = (flags[n] and 1) != 0
		private fun contour(n: Int, out: Contour = Contour()) = out.apply {
			x = xPos[n]
			y = yPos[n]
			onCurve = onCurve(n)
		}

        // @TODO: Do not use by lazy, since this causes a crash on Kotlin/Native
        override val path = run {
            var commandSize = 0
            var dataSize = 0

            forEachContour { cstart, cend, csize ->
                commandSize += 1 + (csize * 2) // Bigger than required
                dataSize += 2 + (csize * 8) // Bigger than required
            }

            GlyphGraphicsPath(index, VectorPath(IntArrayList(commandSize), DoubleArrayList(dataSize))).also { p ->
                //println("flags.size: ${flags.size}, contoursIndices.size=${contoursIndices.size}, xPos.size=${xPos.size}, yPos.size=${yPos.size}")
                //assert(flags.size == contoursIndices.size)
                //assert(xPos.size == contoursIndices.size)
                //assert(yPos.size == contoursIndices.size)
                forEachContour { cstart, cend, csize ->
                    //if (cend >= xPos.size) break
                    var curr: Contour = contour(cend, tempContours[0])
                    var next: Contour = contour(cstart, tempContours[1])

                    when {
                        curr.onCurve -> p.moveTo(curr.x, -curr.y)
                        next.onCurve -> p.moveTo(next.x, -next.y)
                        else -> p.moveTo((curr.x + next.x) * 0.5.toInt(), -((curr.y + next.y) * 0.5).toInt())
                    }

                    for (cpos in 0 until csize) {
                        val prev = curr
                        curr = next
                        next = contour(cstart + ((cpos + 1) % csize), tempContours[(cpos + 2) % 3])

                        if (curr.onCurve) {
                            p.lineTo(curr.x, -curr.y)
                        } else {
                            var prev2X = prev.x
                            var prev2Y = prev.y
                            var next2X = next.x
                            var next2Y = next.y

                            if (!prev.onCurve) {
                                prev2X = ((curr.x + prev.x) * 0.5).toInt()
                                prev2Y = ((curr.y + prev.y) * 0.5).toInt()
                                p.lineTo(prev2X, -prev2Y)
                            }

                            if (!next.onCurve) {
                                next2X = ((curr.x + next.x) * 0.5).toInt()
                                next2Y = ((curr.y + next.y) * 0.5).toInt()
                            }

                            p.lineTo(prev2X, -prev2Y)
                            p.quadTo(curr.x, -curr.y, next2X, -next2Y)
                        }
                    }

                    p.close()
                }
            }
        }

        override val paths = listOf(path)

        private inline fun forEachContour(block: (cstart: Int, cend: Int, csize: Int) -> Unit) {
            for (n in 0 until contoursIndices.size - 1) {
                val cstart = contoursIndices[n] + 1
                val cend = contoursIndices[n + 1]
                val csize = cend - cstart + 1
                block(cstart, cend, csize)
            }
        }
    }

    private fun FastByteArrayInputStream.readF2DOT14(): Float {
		val v = readS16BE()
		val i = v.extractSigned(14, 2)
		val f = v.extract(0, 14)
		return i.toFloat() + f.toFloat() / 16384f
	}

    private fun FastByteArrayInputStream.readFIXED3(): Float {
        val v = readS32BE()
        val i = v.extractSigned(16, 16)
        val f = v.extract(0, 16)
        return i.toFloat() + f.toFloat() / 65536f
    }

	@Suppress("FunctionName")
    private fun FastByteArrayInputStream.readMixBE(signed: Boolean, word: Boolean): Int {
		return when {
			!word && signed -> readS8()
			!word && !signed -> readU8()
			word && signed -> readS16BE()
			word && !signed -> readU16BE()
			else -> invalidOp
		}
	}

    private fun FastByteArrayInputStream.readGlyph(index: Int): Glyph {
		val ncontours = readS16BE()
		val xMin = readS16BE()
		val yMin = readS16BE()
		val xMax = readS16BE()
		val yMax = readS16BE()

        //println("glyph[$index]: ncontours=$ncontours [${ncontours.hex}], xMin=$xMin, yMin=$yMin -- xMax=$xMax, yMax=$yMax")

		if (ncontours < 0) {
			//println("WARNING: readCompositeGlyph not implemented")

			val ARG_1_AND_2_ARE_WORDS = 0x0001
			val ARGS_ARE_XY_VALUES = 0x0002
			val ROUND_XY_TO_GRID = 0x0004
			val WE_HAVE_A_SCALE = 0x0008
			val MORE_COMPONENTS = 0x0020
			val WE_HAVE_AN_X_AND_Y_SCALE = 0x0040
			val WE_HAVE_A_TWO_BY_TWO = 0x0080
			val WE_HAVE_INSTRUCTIONS = 0x0100
			val USE_MY_METRICS = 0x0200
			val OVERLAP_COMPOUND = 0x0400
			val SCALED_COMPONENT_OFFSET = 0x0800
			val UNSCALED_COMPONENT_OFFSET = 0x1000

			val references = arrayListOf<GlyphReference>()

			do {
				val flags = readU16BE()
				val glyphIndex = readU16BE()
                //println("COMPOUND: flags=${flags.shex}, glyphIndex=$glyphIndex")
				val signed = (flags and ARGS_ARE_XY_VALUES) != 0
				val words = (flags and ARG_1_AND_2_ARE_WORDS) != 0
				val x = readMixBE(signed, words)
				val y = readMixBE(signed, words)
				var scaleX = 1f
				var scaleY = 1f
				var scale01 = 0f
				var scale10 = 0f

				when {
					(flags and WE_HAVE_A_SCALE) != 0 -> {
						scaleX = readF2DOT14()
						scaleY = scaleX
					}
					(flags and WE_HAVE_AN_X_AND_Y_SCALE) != 0 -> {
						scaleX = readF2DOT14()
						scaleY = readF2DOT14()
					}
					(flags and WE_HAVE_A_TWO_BY_TWO) != 0 -> {
						scaleX = readF2DOT14()
						scale01 = readF2DOT14()
						scale10 = readF2DOT14()
						scaleY = readF2DOT14()
					}
				}

				//val useMyMetrics = flags hasFlag USE_MY_METRICS
				val ref = GlyphReference(
                    getGlyphByIndex(glyphIndex)!!, x, y,
                    scaleX, scale01, scale10, scaleY
                )
				//println("signed=$signed, words=$words, useMyMetrics=$useMyMetrics")
				//println(ref)
				references += ref
			} while ((flags and MORE_COMPONENTS) != 0)

			return CompositeGlyph(index, xMin, yMin, xMax, yMax, references, horMetrics[index].advanceWidth)
		} else {
			val contoursIndices = IntArray(ncontours + 1)
			contoursIndices[0] = -1
			for (n in 1..ncontours) contoursIndices[n] = readU16BE()
			val instructionLength = readU16BE()
            //println("instructionLength: $instructionLength, $available, ${this@TtfFont.s.length}")
			val instructions = readBytesExact(instructionLength)
			val numPoints = contoursIndices.lastOrNull()?.plus(1) ?: 0
			val flags = IntArrayList()

			var npos = 0
			while (npos < numPoints) {
				val cf = readU8()
				flags.add(cf)
				// Repeat
				if ((cf and 8) != 0) {
					val count = readU8()
					for (n in 0 until count) flags.add(cf)
					npos += count + 1
				} else {
					npos++
				}
			}

			val xPos = IntArray(numPoints)
			val yPos = IntArray(numPoints)

			//println("--------------: $numPoints flags=${flags.toList()}")

			for (xy in 0..1) {
				val pos = if (xy == 0) xPos else yPos
                var p = 0
				for (n in 0 until numPoints) {
					val flag = flags.getAt(n)
					val b1 = ((flag ushr (1 + xy)) and 1) != 0
					val b2 = ((flag ushr (4 + xy)) and 1) != 0
					if (b1) {
						val magnitude = readU8()
						if (b2) p += magnitude else p -= magnitude
					} else if (!b2) {
						p += readS16BE()
					}
					pos[n] = p
				}
			}

			//println("$ncontours, $xMin, $yMax, $xMax, $yMax, ${endPtsOfContours.toList()}, $numPoints, ${flags.toList()}")
			//println(xPos.toList())
			//println(yPos.toList())
			return SimpleGlyph(
                index,
				xMin, yMin,
				xMax, yMax,
				contoursIndices,
				flags.toIntArray(),
				xPos, yPos,
				horMetrics[index].advanceWidth
			)
		}
	}
}

/** int16 that describes a quantity in font design units. */
// https://docs.microsoft.com/en-us/windows/win32/gdi/device-vs--design-units
// DeviceUnits	Specifies the DesignUnits font metric converted to device units. This value is in the same units as the value specified for DeviceResolution.
// DesignUnits	Specifies the font metric to be converted to device units. This value can be any font metric, including the width of a character or the ascender value for an entire font.
// unitsPerEm	Specifies the em square size for the font.
// PointSize	Specifies size of the font, in points. (One point equals 1/72 of an inch.)
// DeviceResolution	Specifies number of device units (pixels) per inch. Typical values might be 300 for a laser printer or 96 for a VGA screen.
internal inline class FWord(val data: Int) {
    fun toDouble(): Double = data.toDouble()
}

internal inline class Fixed(val data: Int) {
    val num: Int get() = data.extract16Signed(0)
    val den: Int get() = data.extract16Signed(16)
    val value: Double get() = num.toDouble() + den.toDouble() / 65536.0
    fun toDouble(): Double = value
    companion object {
        operator fun invoke(num: Int, den: Int): Int = 0.insert16(num, 0).insert16(den, 16)
    }

    override fun toString(): String = "Fixed($num,$den)=$value"
}

suspend fun VfsFile.readTtfFont(
    preload: Boolean = false,
    onlyReadMetadata: Boolean = false,
) = TtfFont(this.readAll(), freeze = preload, extName = this.baseName, onlyReadMetadata = onlyReadMetadata)

// @TODO: Move to KorMA
private fun VectorPath.write(path: VectorPath, transform: Matrix) {
    this.commands += path.commands
    for (n in 0 until path.data.size step 2) {
        val x = path.data.getAt(n + 0)
        val y = path.data.getAt(n + 1)
        this.data += transform.transformX(x, y)
        this.data += transform.transformY(x, y)
    }
    this.lastX = transform.transformX(path.lastX, path.lastY)
    this.lastY = transform.transformY(path.lastX, path.lastY)
}
