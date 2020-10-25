package com.soywiz.korim.font

import com.soywiz.kds.*
import com.soywiz.kds.iterators.fastForEach
import com.soywiz.kmem.extract16Signed
import com.soywiz.kmem.insert
import com.soywiz.kmem.unsigned
import com.soywiz.korim.vector.*
import com.soywiz.korio.file.*
import com.soywiz.korio.lang.*
import com.soywiz.korio.stream.*
import com.soywiz.korio.util.encoding.*
import com.soywiz.korma.geom.*
import com.soywiz.korma.geom.vector.*
import kotlin.collections.set

@Suppress("MemberVisibilityCanBePrivate", "UNUSED_VARIABLE", "LocalVariableName", "unused")
// Used information from:
// - https://www.sweetscape.com/010editor/repository/files/TTF.bt
// - http://scripts.sil.org/cms/scripts/page.php?site_id=nrsi&id=iws-chapter08
// - https://www.microsoft.com/en-us/Typography/OpenTypeSpecification.aspx
// - https://en.wikipedia.org/wiki/Em_(typography)
// - http://stevehanov.ca/blog/index.php?id=143 (Let's read a Truetype font file from scratch)
// - http://chanae.walon.org/pub/ttf/ttf_glyphs.htm
class TtfFont(private val s: FastByteArrayInputStream, private val freeze: Boolean = false, private val extName: String? = null) : VectorFont {
    constructor(d: ByteArray, freeze: Boolean = false, extName: String? = null) : this(d.openFastStream(), freeze, extName)

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

    override fun getGlyphPath(size: Double, codePoint: Int, path: GlyphPath): GlyphPath? {
        val g = getGlyphByCodePoint(codePoint) ?: return null
        val scale = getTextScale(size)
        path.path = g.path
        path.transform.identity()
        //path.transform.scale(scale, -scale)
        path.transform.scale(scale, scale)
        return path
    }

    private fun getTextScale(size: Double) = size / unitsPerEm.toDouble()

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
    private var unitsPerEm = 128
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
    private val characterMaps = LinkedHashMap<Int, Int>()
    private val tablesByName = LinkedHashMap<String, Table>()
    private val glyphCache = IntMap<Glyph>(512)
    private fun getCharacterMapOrNull(key: Int): Int? = characterMaps[key]

    private var frozen = false

    init {
        readHeaderTables()
        readHead()
        readMaxp()
        readHhea()
        readNames()
        readLoca()
        readCmap()
        readHmtx()

        if (freeze) {
            getAllGlyphs(cache = true).fastForEach {
                it.metrics1px // Compute it
                it.path // Compute it
            }
        }

        frozen = true
    }

    override val name: String get() = extName ?: "TtfFont" // @TODO: Use loaded name

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

    fun FastByteArrayInputStream.readFixed() = Fixed(readS16LE(), readS16LE())
	data class HorMetric(val advanceWidth: Int, val lsb: Int)

    @PublishedApi
	internal fun openTable(name: String) = tablesByName[name]?.open()

	private fun readHeaderTables() = s.sliceStart().apply {
		val majorVersion = readU16BE().apply { if (this != 1) invalidOp("Not a TTF file") }
		val minorVersion = readU16BE().apply { if (this != 0) invalidOp("Not a TTF file") }
		val numTables = readU16BE()
		val searchRange = readU16BE()
		val entrySelector = readU16BE()
		val rangeShift = readU16BE()

		val tables = (0 until numTables).map {
            Table(readStringz(4), readS32BE(), readS32BE(), readS32BE())
		}

		for (table in tables) {
			table.s = sliceWithSize(table.offset, table.length)
			tablesByName[table.id] = table
		}

		//for (table in tables) println(table)
	}

    private inline fun runTableUnit(name: String, callback: FastByteArrayInputStream.() -> Unit) {
		openTable(name)?.callback()
	}

    private inline fun <T> runTable(name: String, callback: FastByteArrayInputStream.(Table) -> T): T? = openTable(name)?.let { callback(it, tablesByName[name]!!) }

    private fun readNames() = runTableUnit("name") {
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

			val charset = when (encodingId) {
				0 -> UTF8
				1 -> UTF16_BE
				else -> UTF16_BE
			}
			//println("" + (stringOffset.toLong() + offset) + " : " + length + " : " + charset)
			val string =
				this.clone().sliceWithSize(stringOffset + offset, length).readAll().toString(charset)
			//println(string)
		}
	}

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

	private fun readCmap() = runTableUnit("cmap") {
		data class EncodingRecord(val platformId: Int, val encodingId: Int, val offset: Int)

		val version = readU16BE()
		val numTables = readU16BE()
		val tables = (0 until numTables).map { EncodingRecord(readU16BE(), readU16BE(), readS32BE()) }

		for (table in tables) {
			sliceStart(table.offset).run {
				val format = readU16BE()
				when (format) {
					4 -> {
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
						//val glyphIdArray = readCharArrayBE(idRangeOffset.max()?.toInt() ?: 0)

						//println("$language")

						for (n in 0 until segCount) {
							val ec = endCount[n].toInt()
							val sc = startCount[n].toInt()
							val delta = idDelta[n].toInt()
							val iro = idRangeOffset[n].toInt()
							//println("%04X-%04X : %d : %d".format(sc, ec, delta, iro))
							for (c in sc..ec) {
								var index: Int
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
								characterMaps[c] = index and 0xFFFF
								//println("%04X --> %d".format(c, index and 0xFFFF))
							}
						}

						//for ((c, index) in characterMaps) println("\\u%04X -> %d".format(c.toInt(), index))
					}
					12 -> {
						readU16BE() // reserved
						val length = readS32BE()
						val language = readS32BE()
						val numGroups = readS32BE()

						for (n in 0 until numGroups) {
							val startCharCode = readS32BE()
							val endCharCode = readS32BE()
							val startGlyphId = readS32BE()

							var glyphId = startGlyphId
							for (c in startCharCode..endCharCode) {
								characterMaps[c] = glyphId++
							}
						}
					}
					else -> { // Ignored

					}
				}
				//println("cmap.table.format: $format")
			}
		}
		//println(tables)
	}

    private fun getCharIndexFromCodePoint(codePoint: Int): Int? = getCharacterMapOrNull(codePoint)
    private fun getCharIndexFromChar(char: Char): Int? = getCharacterMapOrNull(char.toInt())

    private fun getGlyphByCodePoint(codePoint: Int, cache: Boolean = true): Glyph? = getCharacterMapOrNull(codePoint)?.let { getGlyphByIndex(it, cache) }
    private fun getGlyphByChar(char: Char, cache: Boolean = true): Glyph? = getGlyphByCodePoint(char.toInt(), cache)

    private operator fun get(char: Char) = getGlyphByChar(char)
    private operator fun get(codePoint: Int) = getGlyphByCodePoint(codePoint)

    private fun getGlyphByIndex(index: Int, cache: Boolean = true): Glyph? {
        val start = locs.getOrNull(index) ?: 0
        val end = locs.getOrNull(index + 1) ?: start
        val size = end - start
        //println("GLYPH INDEX: $index")
        val glyph = when {
            size != 0 -> this.glyphCache[index] ?: runTable("glyf") { table ->
                //println("READ GLYPH[$index]: start=$start, end=$end, size=$size, table=$table")
                sliceStart(start).readGlyph(index)
            }
            else -> {
                //println("EMPTY GLYPH: SIZE: $size, index=$index")
                SimpleGlyph(index, 0, 0, 0, 0, intArrayOf(), intArrayOf(), intArrayOf(), intArrayOf(), horMetrics[index].advanceWidth)
            }
        }
        if (cache && !frozen) this.glyphCache[index] = glyph
        return glyph
    }

    private fun getAllGlyphs(cache: Boolean = false) = (0 until numGlyphs).mapNotNull { getGlyphByIndex(it, cache) }

    private val nonExistantGlyphMetrics1px = GlyphMetrics(1.0, false, 0, Rectangle(), 0.0)

    private data class Contour(var x: Int = 0, var y: Int = 0, var onCurve: Boolean = false) {
		fun copyFrom(that: Contour) {
			this.x = that.x
			this.y = that.y
			this.onCurve = that.onCurve
		}
	}

    private data class GlyphReference(
        val glyph: Glyph,
        val x: Int, val y: Int,
        val scaleX: Float,
        val scale01: Float,
        val scale10: Float,
        val scaleY: Float
	)

    private abstract inner class Glyph(
        val index: Int,
        val xMin: Int, val yMin: Int,
        val xMax: Int, val yMax: Int,
        val advanceWidth: Int
    ) {
        abstract val path: GraphicsPath

        internal val metrics1px = run {
            val size = unitsPerEm.toDouble()
            val scale = getTextScale(size)
            GlyphMetrics(size, true, -1, Rectangle.fromBounds(
                xMin * scale, yMin * scale,
                xMax * scale, yMax * scale
            ), advanceWidth * scale)
        }
    }

    private inner class CompositeGlyph(
        index: Int,
        xMin: Int, yMin: Int,
        xMax: Int, yMax: Int,
        val refs: List<GlyphReference>,
        advanceWidth: Int
	) : Glyph(index, xMin, yMin, xMax, yMax, advanceWidth) {
        override fun toString(): String = "CompositeGlyph[$advanceWidth](${refs})"

        // @TODO: Do not use by lazy, since this causes a crash on Kotlin/Native
        override val path: GraphicsPath = run {
            var commandsSize = 0
            var dataSize = 0

            refs.fastForEach { ref ->
                val gpath = ref.glyph.path
                commandsSize += gpath.commands.size
                dataSize += gpath.data.size
            }

            GraphicsPath(IntArrayList(commandsSize), DoubleArrayList(dataSize)).also { out ->
                refs.fastForEach { ref ->
                    val m = Matrix()
                    m.translate(ref.x, ref.y)
                    m.scale(ref.scaleX, ref.scaleY)
                    out.write(ref.glyph.path, m)
                }
            }
        }
	}


    private inner class SimpleGlyph(
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

            GraphicsPath(IntArrayList(commandSize), DoubleArrayList(dataSize)).also { p ->
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
		val i = v shr 14
		val f = v and 0x3FFF
		return i.toFloat() + f.toFloat() / 16384f
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

internal inline class Fixed(val data: Int) {
    val num: Int get() = data.extract16Signed(0)
    val den: Int get() = data.extract16Signed(16)
    companion object {
        operator fun invoke(num: Int, den: Int) = 0.insert(num, 0, 16).insert(den, 16, 16)
    }
}

suspend fun VfsFile.readTtfFont(preload: Boolean = false) = TtfFont(this.readAll(), freeze = preload, extName = this.baseName)

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
