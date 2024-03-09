package korlibs.image.font

import korlibs.datastructure.*
import korlibs.io.lang.*
import korlibs.io.stream.*
import korlibs.logger.*
import korlibs.math.*
import korlibs.math.geom.*
import korlibs.math.geom.vector.*
import kotlin.collections.*
import kotlin.math.*
import kotlin.random.*

/**
 * Compact Font Format font representation (also known as a PostScript Type 1, or CIDFont)
 * https://docs.microsoft.com/en-us/typography/opentype/spec/cff
 * - https://partners.adobe.com/public/developer/en/font/5176.CFF.pdf
 * - https://adobe-type-tools.github.io/font-tech-notes/pdfs/5177.Type2.pdf
 * - https://learn.microsoft.com/en-us/typography/opentype/spec/cff2
 *
 * TOOLS for debugging and reference:
 * - https://fontdrop.info/#/?darkmode=true
 * - https://yqnn.github.io/svg-path-editor/
 * - https://github.com/caryll/otfcc/blob/617837bc6a74b0cf6be9e77a8ed1d2ffb39ea425/lib/libcff/cff-parser.c#L342
 * - https://github.com/RazrFalcon/ttf-parser/blob/master/src/tables/cff/cff2.rs
 * - https://github.com/nothings/stb/blob/master/stb_truetype.h
 * - https://github.com/opentypejs/opentype.js
 */
object TtfCIDFont {
    val logger = Logger("TtfCIDFont")

    /**
     * https://partners.adobe.com/public/developer/en/font/5176.CFF.pdf
     *
     * https://github.com/caryll/otfcc/blob/617837bc6a74b0cf6be9e77a8ed1d2ffb39ea425/lib/table/CFF.c
     */
    object CFF {
        /**
         * Table 1 CFF Data Layout
         * - Header –
         * - Name INDEX –
         * - Top DICT INDEX –
         * - String INDEX –
         * - Global Subr INDEX –
         * - Encodings –
         * - Charsets –
         * - FDSelect CIDFonts only
         * - CharStrings INDEX per-font
         * - Font DICT INDEX per-font, CIDFonts only
         * - Private DICT per-font
         * - Local Subr INDEX per-font or per-Private DICT for CIDFonts
         * - Copyright and
         * - Trademark Notices
         */
        fun FastByteArrayInputStream.readCFF(): CFFResult {
            readHeader()
            val nameIndex = readIndex()
            //println("nameIndex=$nameIndex, str=${nameIndex[0].readStringz(0)}")

            val topDictIndex = readIndex()
            val topDictByte = topDictIndex[0][1]
            //println("topDictByte=${topDictByte.toUByte()}")
            val topDict = topDictIndex[0].openFastStream().readDICTMap()
            //println("topDict=$topDictIndex")
            //println("topDict=$topDict")
            val stringIndex = readIndex()
            //println("stringIndex=$stringIndex")
            //println(stringIndex.toList().map { it.decodeToString() })
            val globalSubrIndex = readIndex()
            //println("globalSubrIndex=$globalSubrIndex")

            val privateSizeOffset = topDict[Op.Private]
            var localSubrsIndex: DataIndex = DataIndex()

            var privateDict: Map<Op, List<Number>> = emptyMap()

            if (privateSizeOffset != null) {
                val (privateSize, privateOffset) = privateSizeOffset.map { it.toInt() }
                privateDict = this.sliceWithSize(privateOffset, privateSize).readDICTMap()

                //println("privateDict=$privateDict")
                val localSubrs = privateDict[Op.Subrs]
                if (localSubrs != null) {
                    val subrsOffset = privateOffset + localSubrs.last().toInt()
                    localSubrsIndex = sliceStart(subrsOffset).readIndex()
                    //println("localSubrsIndex=$localSubrsIndex")

                    //val evalCtx = CharStringType2.EvalContext(globalSubrIndex, localSubrsIndex)
                    //for (n in 0 until localSubrsIndex.size) {
                    //    val vp = VectorPath()
                    //    val bytes = localSubrsIndex[n]
                    //    try {
                    //        //println("### SUBR[$n][${bytes.size}]:")
                    //        CharStringType2.eval(vp, bytes.openFastStream(), evalCtx)
                    //    } catch (e: Throwable) {
                    //        e.printStackTrace()
                    //    }
                    //}
                }
                //println("PRIV[$privateSizeOffset]: $privateDict")
            }

            //println("topDict=$topDict")
            //println("privateDict=$privateDict")

            val charstringsOffsets: List<Number>? = topDict[Op.CharStrings]
            val charstringsOffset = charstringsOffsets!!.first().toInt()
            //println("charstringsOffset=$charstringsOffset")
            val charstringsIndex = this.sliceStart(charstringsOffset).readIndex()
            //println("charstringsIndex=$charstringsIndex")

            return CFFResult(charstringsIndex, globalSubrIndex, localSubrsIndex, privateDict)
            /*
            //for (n in 0 until charstringsIndex.size) {
            for (n in 9..9) {
            //for (n in 5..5) {
                val vp = VectorPath()
                val bytes = charstringsIndex[n]
                try {
                    CharStringType2.eval(vp, bytes.openFastStream(), evalCtx)
                    println("### CHAR[$n/${charstringsIndex.size}][${bytes.size}]: ${vp.toSvgString().color(AnsiEscape.Color.YELLOW)}")
                } catch (e: Throwable) {
                    e.printStackTrace()
                }
                if (n >= 10) {
                    println("***********")
                    break
                }
            }

             */
        }

        data class GlyphPath(val path: VectorPath, val advanceWidth: Double)

        class CFFResult(
            val charstringsIndex: DataIndex,
            val globalSubrIndex: DataIndex,
            val localSubrsIndex: DataIndex,
            val privateDict: Map<Op, List<Number>>
        ) {
            var defaultWidthX = privateDict[Op.defaultWidthX]?.firstOrNull()?.toDouble() ?: 0.0
            var nominalWidthX = privateDict[Op.nominalWidthX]?.firstOrNull()?.toDouble() ?: 0.0

            fun getGlyphVector(index: Int, flipY: Boolean = true): GlyphPath {
                val evalCtx = CharStringType2.EvalContext(globalSubrIndex, localSubrsIndex, defaultWidthX, nominalWidthX, index)
                val vp = VectorPath()
                val bytes = charstringsIndex[index]
                CharStringType2.eval(vp, bytes.openFastStream(), evalCtx)
                if (flipY) vp.scale(1.0, -1.0)
                //println("width=${evalCtx.width}")
                return GlyphPath(vp, evalCtx.width)
            }
        }

        fun FastByteArrayInputStream.readHeader() {
            val versionMajor = readU8()
            val versionMinor = readU8()
            val headerSize = readU8()
            val offsetSize = readU8()

            if (versionMajor > 1) error("Only supported CFF version 1")

            logger.debug { "CFF.versionMajor/Minor: $versionMajor.$versionMinor : headerSize=$headerSize, offsetSize=$offsetSize" }
        }

        fun FastByteArrayInputStream.readOffSize(): Int = readU8()

        fun FastByteArrayInputStream.readOffset(offSize: Int = -1): Int {
            val roffSize = if (offSize < 0) readOffSize() else offSize
            return when (roffSize) {
                //0 -> 0
                1 -> readU8()
                2 -> readU16BE()
                3 -> readU24BE()
                4 -> readS32BE()
                else -> reserved("$roffSize")
            }
        }

        class DataIndex(val offsets: IntArray, val bytes: ByteArray) : Iterable<ByteArray> {
            constructor() : this(intArrayOf(0), byteArrayOf())
            val size: Int get() = offsets.size - 1
            fun itemSize(index: Int): Int = offsets[index + 1] - offsets[index]
            operator fun get(index: Int): ByteArray = bytes.copyOfRange(offsets[index], offsets[index + 1])
            fun toList(): List<ByteArray> = (0 until size).map { this[it] }
            override fun iterator(): Iterator<ByteArray> = toList().iterator()

            override fun toString(): String = "DataIndex[$size](${offsets.toList()})(${(0 until size).map { itemSize(it) }})"
        }

        fun FastByteArrayInputStream.readIndex(): DataIndex {
            val count = readU16BE()
            if (count == 0) return DataIndex()
            val offSize = readOffSize()
            //println("offSize=$offSize")
            val offsets = (0 until count + 1).mapInt { readOffset(offSize) - 1 }
            val bytes = readBytesExact(offsets.last())
            return DataIndex(offsets.toIntArray(), bytes)
        }

        fun FastByteArrayInputStream.readDICTMap(): Map<Op, List<Number>> {
            val map = LinkedHashMap<Op, List<Number>>()
            val values = arrayListOf<Number>()
            while (hasMore) {
                val item = readDICTElement()
                when (item) {
                    is Op -> {
                        map[item] = values.toList()
                        values.clear()
                    }
                    is Number -> values.add(item)
                }
            }
            return map
        }

        // Table 3 Operand Encoding
        fun FastByteArrayInputStream.readDICTElement(): Any? {
            val b0 = readU8()
            return when (b0) {
                in 0..21 -> Op[
                    when (b0) {
                        12 -> (b0 shl 8) or readU8()
                        else -> b0
                    }
                ]
                in 22..27 -> reserved
                // shortint
                28 -> ((readU8() shl 8) or readU8()).toShort().toInt()
                29 -> (readU8() shl 24) or (readU8() shl 16) or (readU8() shl 8) or (readU8())
                30 -> readEncodedReal(readHeader = false)
                31 -> reserved
                in 32..246 -> b0 - 139
                in 247..250 -> (b0 - 247) * 256 + readU8() + 108
                in 251..254 -> -((b0 - 251) * 256) - readU8() - 108
                255 -> reserved
                else -> unreachable
            }
        }

        fun FastByteArrayInputStream.readEncodedRealString(readHeader: Boolean = true): String {
            // -2.25 :: 1e e2 a2 5f
            // 0.140541E–3 :: 1e 0a 14 05 41 c3 ff

            // Starts with: 0x1e - 30
            if (readHeader) assert(readU8() == 0x1e)
            val str = StringBuilder()
            end@while (true) {
                val byte = readU8()
                for (n in 0..1) {
                    val nibble: Int = (byte ushr ((1 - n) * 4)) and 0xF
                    if (nibble == 0xF) break@end
                    when (nibble) {
                        0xF -> break@end
                        0xC -> str.append("E-")
                        else -> str.append(when (nibble) {
                            in 0..9 -> '0' + nibble
                            0xA -> '.'; 0xB -> 'E'; 0xE -> '-'; else -> '0'
                        })
                    }
                }
            }
            return str.toString()
        }

        fun FastByteArrayInputStream.readEncodedReal(readHeader: Boolean = true): Double {
            return readEncodedRealString(readHeader).toDouble()
        }

        // CFF DICT Operators
        enum class Op(val id: Int) : CharStringType2.Token {
            version(0x00), Copyright(0x0c00),
            Notice(0x01), isFixedPitch(0x0c01),
            FullName(0x02), ItalicAngle(0x0c02),
            FamilyName(0x03), UnderlinePosition(0x0c03),
            Weight(0x04), UnderlineThickness(0x0c04),
            FontBBox(0x05), PaintType(0x0c05),
            BlueValues(0x06), CharstringType(0x0c06),
            OtherBlues(0x07), FontMatrix(0x0c07),
            FamilyBlues(0x08), StrokeWidth(0x0c08),
            FamilyOtherBlues(0x09), BlueScale(0x0c09),
            StdHW(0x0a), BlueShift(0x0c0a),
            StdVW(0x0b), BlueFuzz(0x0c0b),
            /* 0x0c escape */           StemSnapH(0x0c0c),
            UniqueID(0x0d), StemSnapV(0x0c0d),
            XUID(0x0e), ForceBold(0x0c0e),
            charset(0x0f), /* 0x0c0f Reserved */
            Encoding(0x10), /* 0x0c10 Reserved */
            CharStrings(0x11), LanguageGroup(0x0c11),
            Private(0x12), ExpansionFactor(0x0c12),
            Subrs(0x13), initialRandomSeed(0x0c13),
            defaultWidthX(0x14), SyntheicBase(0x0c14),
            nominalWidthX(0x15), PostScript(0x0c15),
            vsindex(0x16), BaseFontName(0x0c16),
            blend(0x17), BaseFontBlend(0x0c17),
            vstore(0x18), /* 0x0c18 Reserved */
            maxstack(0x19), /* 0x0c19 Reserved */
            /* 0x0c1a Reserved */
            /* 0x0c1b Reserved */
            /* 0x0c1c Reserved */
            /* 0x0c1d Reserved */
            ROS(0x0c1e),
            CIDFontVersion(0x0c1f),
            CIDFontRevision(0x0c20),
            CIDFontType(0x0c21),
            CIDCount(0x0c22),
            UIDBase(0x0c23),
            FDArray(0x0c24),
            FDSelect(0x0c25),
            FontName(0x0c26);
            companion object {
                val VALUES_BY_ID = values().associateBy { it.id }.toIntMap()
                operator fun get(id: Int): Op? = VALUES_BY_ID[id]
            }
        }
    }

    /**
     * The Type 2 format provides a method for compact encoding of
     * glyph procedures in an outline font program. Type 2 charstrings
     * must be used in a CFF (Compact Font Format) or OpenType font
     * file to create a complete font program.
     *
     * - https://adobe-type-tools.github.io/font-tech-notes/pdfs/5177.Type2.pdf
     */
    object CharStringType2 {
        sealed interface Token

        var DoubleArrayList.last: Double
            get() = this[size - 1]
            set(value) { this[size - 1] = value }

        fun DoubleArrayList.pop(): Double = removeAt(size - 1)
        fun DoubleArrayList.push(value: Double): Unit { add(value) }
        fun DoubleArrayList.push(value: Int): Unit { add(value.toDouble()) }

        inline fun DoubleArrayList.binop(block: (l: Double, r: Double) -> Double) {
            val r = pop()
            val l = pop()
            push(block(l, r))
        }

        inline fun DoubleArrayList.unop(block: (r: Double) -> Double) {
            last = block(last)
        }

        class EvalContext(
            val globalSubrIndex: CFF.DataIndex = CFF.DataIndex(),
            val localSubrsIndex: CFF.DataIndex = CFF.DataIndex(),
            var defaultWidthX: Double = 0.0,
            var nominalWidthX: Double = 0.0,
            val index: Int = -1,
            val heap: DoubleArray = DoubleArray(32)
        ) {
            val globalBias = computeSubrBias(globalSubrIndex.size)
            val localBias = computeSubrBias(localSubrsIndex.size)

            var nStems = 0
            var width = defaultWidthX
            var haveWidth = false
            init {
                //println("index=$index, defaultWidthX=$defaultWidthX")
            }
        }

        fun VectorBuilder.cfrClose() {
            //println("Z".color(AnsiEscape.Color.RED))
            val lastPoint = this.lastPos
            close()
            moveTo(lastPoint)
        }
        fun VectorBuilder.cfrMoveTo(x: Double, y: Double) {
            //println("M x=${lastX + x} y=${lastY + y}".color(AnsiEscape.Color.RED))
            rMoveTo(Point(x, y))
        }
        fun VectorBuilder.cfrLineTo(x: Double, y: Double) {
            //println("L x=${lastX + x} y=${lastY + y}".color(AnsiEscape.Color.RED))
            rLineTo(Point(x, y))
        }
        fun VectorBuilder.cfCubicTo(fcx1: Double, fcy1: Double, fcx2: Double, fcy2: Double, fax: Double, fay: Double) {
            //println("C x=$fax y=$fay x1=$fcx1 y1=$fcy1 x2=$fcx2 y2=$fcy2".color(AnsiEscape.Color.RED))
            cubicTo(Point(fcx1, fcy1), Point(fcx2, fcy2), Point(fax, fay))
        }
        fun VectorBuilder.cfrCubicTo(cx1: Double, cy1: Double, cx2: Double, cy2: Double, ax: Double, ay: Double) {
            //rCubicTo(ax, ay, cx1, cy1, cx2, cy2)

            val fcx1 = lastPos.x + cx1
            val fcy1 = lastPos.y + cy1

            val fcx2 = fcx1 + cx2
            val fcy2 = fcy1 + cy2

            val fax = fcx2 + ax
            val fay = fcy2 + ay

            cfCubicTo(fcx1, fcy1, fcx2, fcy2, fax, fay)
        }
        fun VectorBuilder.cfrMoveToHV(v: Double, horizontal: Boolean) = cfrMoveTo(if (horizontal) v else 0.0, if (!horizontal) v else 0.0)
        fun VectorBuilder.cfrLineToHV(v: Double, horizontal: Boolean) = cfrLineTo(if (horizontal) v else 0.0, if (!horizontal) v else 0.0)
        
        fun FastByteArrayInputStream.u8(): Int {
            return readU8().also {
                //println("READ[${position}/${length}]: $it, ${it.hex}")
            }
        }

        // https://github.com/caryll/otfcc/blob/617837bc6a74b0cf6be9e77a8ed1d2ffb39ea425/lib/libcff/cff-parser.c#L342
        // Table 1 Type 2 Charstring Encoding Values
        fun eval(ctx: VectorBuilder, s: FastByteArrayInputStream, evalCtx: EvalContext, stack: DoubleArrayList = DoubleArrayList(32), stackLevel: Int = 0) {
            while (s.hasMore) {
                val v0 = s.u8()
                when (v0) {
                    // Operator
                    in 0..27, in 29..31 -> {
                        val op = Op[when (v0) {
                            12 -> (v0 shl 8) or s.u8()
                            else -> v0
                        }]

                        //print(" ".repeat(stackLevel))
                        //println("OP: $op - $stack")

                        when (op) {
                            // Special
                            Op.`return` -> return
                            Op.endchar -> {
                                if (stack.isNotEmpty() && !evalCtx.haveWidth) {
                                    evalCtx.width = stack[0] + evalCtx.nominalWidthX
                                    evalCtx.haveWidth = true
                                }

                                if (ctx.totalPoints != 0) {
                                    ctx.cfrClose()
                                }
                                return
                            }
                            Op.callsubr, Op.callgsubr -> {
                                val global = op == Op.callgsubr
                                val funcOffset = stack.pop().toInt()
                                val bytes = when {
                                    global -> evalCtx.globalSubrIndex[evalCtx.globalBias + funcOffset]
                                    else -> evalCtx.localSubrsIndex[evalCtx.localBias + funcOffset]
                                }
                                eval(ctx, bytes.openFastStream(), evalCtx, stack, stackLevel = stackLevel + 1)
                            }

                            // Stack & heap (32 elements)
                            Op.drop -> stack.pop()
                            Op.dup -> stack.pop().also { stack.add(it, it) }
                            Op.exch -> stack.swap(stack.size - 2, stack.size - 1)
                            // @TODO: CHECK
                            Op.roll -> TODO()
                            // @TODO: CHECK
                            Op.index -> {
                                TODO()
                                assert(stack.size >= 2)
                                val index = stack.pop().toInt()
                                stack.push(if (index < 0) stack[stack.size - 1] else stack[stack.size - 1 - index])
                            }
                            Op.put -> {
                                val index = stack.pop()
                                val value = stack.pop()
                                evalCtx.heap[index.toInt()] = value
                            }
                            Op.get -> stack.unop { evalCtx.heap[it.toInt()] }

                            // Logic
                            Op.and -> stack.binop { l, r -> (l != 0.0 && r != 0.0).toInt().toDouble() }
                            Op.or -> stack.binop { l, r -> (l != 0.0 || r != 0.0).toInt().toDouble() }
                            Op.not -> stack.unop { (it == 0.0).toInt().toDouble() }

                            // Comparison
                            Op.eq -> stack.binop { l, r -> (l == r).toInt().toDouble() }
                            Op.ifelse -> {
                                val v2 = stack.pop()
                                val v1 = stack.pop()
                                val s2 = stack.pop()
                                val s1 = stack.pop()
                                stack.push(if (v1 <= v2) s1 else s2)
                            }

                            // Generator
                            Op.random -> stack.add(Random.nextDouble()) // @TODO: Validate this is between 0 and 1

                            // Arithmetic
                            // Unop
                            Op.sqrt -> stack.unop { sqrt(it) }
                            Op.abs -> stack.unop { abs(it) }
                            Op.neg -> stack.unop { -it }
                            // Binop
                            Op.add -> stack.binop { l, r -> l + r }
                            Op.sub -> stack.binop { l, r -> l - r }
                            Op.div -> stack.binop { l, r -> l / r }
                            Op.mul -> stack.binop { l, r -> l * r }

                            // Drawing
                            Op.rmoveto -> {
                                if (stack.size > 2) {
                                    evalCtx.width = stack[0] + evalCtx.nominalWidthX
                                    evalCtx.haveWidth = true
                                    //println("rmoveto width ${evalCtx.width} : $stack")
                                }
                                if (ctx.totalPoints != 0) {
                                    ctx.cfrClose()
                                }
                                ctx.cfrMoveTo(stack[stack.size - 2], stack[stack.size - 1])
                                stack.clear()
                            }
                            Op.vmoveto, Op.hmoveto -> {
                                if (stack.size > 1) {
                                    evalCtx.width = stack[0] + evalCtx.nominalWidthX
                                    evalCtx.haveWidth = true
                                }
                                ctx.cfrMoveToHV(stack.pop(), op == Op.hmoveto)
                                stack.clear()
                            }
                            Op.rlineto -> {
                                for (n in 0 until stack.size step 2) ctx.cfrLineTo(stack[n], stack[n + 1])
                                stack.clear()
                            }
                            Op.hlineto, Op.vlineto -> {
                                var toggle = op == Op.hlineto
                                for (n in 0 until stack.size) {
                                    ctx.cfrLineToHV(stack[n], horizontal = toggle)
                                    toggle = !toggle
                                }
                                stack.clear()
                            }
                            Op.rrcurveto, Op.rcurveline, Op.rlinecurve -> {
                                var n = 0
                                if (op == Op.rlinecurve) {
                                    while (stack.size - n < 6) ctx.cfrLineTo(stack[n++], stack[n++])
                                }
                                while (stack.size - n >= 6) {
                                    ctx.cfrCubicTo(stack[n++], stack[n++], stack[n++], stack[n++], stack[n++], stack[n++])
                                }
                                if (op == Op.rcurveline) {
                                    while (n < stack.size) ctx.cfrLineTo(stack[n++], stack[n++])
                                }
                                stack.clear()
                            }
                            Op.vvcurveto, Op.hhcurveto -> {
                                // vvcurveto: dx1? {dya dxb dyb dyc}+
                                // hhcurveto: dy1? {dxa dxb dyb dxc}+

                                val horizontal = op == Op.hhcurveto
                                var n = 0

                                // The odd argument count indicates an X position.
                                val v = if (stack.size % 2 == 1) stack[n++] else 0.0
                                var initialY = if (horizontal) v else 0.0
                                var initialX = if (horizontal) 0.0 else v

                                if ((stack.size - n) % 4 != 0) error("Invalid number of operands")

                                while (n < stack.size) {
                                    val vv = stack[n++]
                                    val x1 = initialX + (if (horizontal) vv else 0.0)
                                    val y1 = initialY + (if (horizontal) 0.0 else vv)
                                    val x2 = stack[n++]
                                    val y2 = stack[n++]
                                    val v1 = stack[n++]
                                    val lastX = (if (horizontal) v1 else 0.0)
                                    val lastY = (if (horizontal) 0.0 else v1)
                                    ctx.cfrCubicTo(x1, y1, x2, y2, lastX, lastY)
                                    initialX = 0.0
                                    initialY = 0.0
                                }

                                stack.clear()
                            }
                            // @TODO: Rewrite this
                            Op.vhcurveto, Op.hvcurveto -> {
                                stack.reverse()
                                var toggle = op == Op.hvcurveto
                                while (stack.isNotEmpty()) {
                                    val vv = stack.pop()
                                    val x1 = (if (toggle) vv else 0.0)
                                    val y1 = (if (toggle) 0.0 else vv)
                                    val x2 = stack.pop()
                                    val y2 = stack.pop()
                                    val v1 = stack.pop()
                                    val v2 = if (stack.size == 1) stack.pop() else 0.0
                                    val ax = if (toggle) v2 else v1
                                    val ay = if (toggle) v1 else v2
                                    ctx.cfrCubicTo(x1, y1, x2, y2, ax, ay)
                                    toggle = !toggle
                                }
                                stack.clear()
                            }
                            // Curves or straights depending on rendering factors
                            Op.hflex, Op.flex, Op.hflex1, Op.flex1 -> {
                                // Ignore flexes for now
                                TODO()
                                stack.clear()
                            }
                            // Hints
                            Op.hstem, Op.vstem, Op.hstemhm, Op.vstemhm, Op.hintmask, Op.cntrmask -> {
                                // Ignore hints for now
                                //TODO()

                                // The number of stem operators on the stack is always even.
                                // If the value is odd, that means a width is specified.
                                if (stack.size % 2 != 0 && !evalCtx.haveWidth) {
                                    val value = stack[0]
                                    evalCtx.width = value + evalCtx.nominalWidthX
                                    //println("parseStems $op width: ${evalCtx.width} value=$value, nominalWidthX=${evalCtx.nominalWidthX} : $stack")
                                }

                                evalCtx.nStems += stack.size ushr 1
                                //println("parseStems: " + (stack.size ushr 1) + ", " + evalCtx.nStems)
                                evalCtx.haveWidth = true;
                                stack.clear()

                                if (op == Op.hintmask || op == Op.cntrmask) {
                                    val skipCount = (evalCtx.nStems + 7) ushr 3
                                    //println("$op: skipCount=$skipCount, nStems=${evalCtx.nStems}")
                                    s.skip(skipCount)
                                }
                            }
                            // Deprecated
                            Op.cff2vsidx -> TODO()
                            Op.cff2blend -> TODO()
                        }
                    }
                    // Push double
                    255 -> {
                        val integerPart = (s.u8() shl 8) or s.u8()
                        val fractionPart = (s.u8() shl 8) or s.u8()
                        val value = integerPart.toDouble() + fractionPart.toDouble() / 65536.0
                        stack.push(value)
                        //println("PUSH: $value")
                    }
                    // Push int
                    else -> {
                        val value: Int = when (v0) {
                            28 -> ((s.u8() shl 8) or s.u8()).toShort().toInt()
                            in 32..246 -> (v0 - 139)
                            in 247..250 -> ((v0 - 247) * 256 + s.u8() + 108)
                            in 251..254 -> (-((v0 - 251) * 256) - s.u8() - 108)
                            else -> unreachable
                        }
                        stack.push(value)
                        //println("PUSH: $value")
                    }
                }
            }
        }

        private fun computeSubrBias(count: Int): Int {
            return when {
                count < 1240 -> 107
                count < 33900 -> 1131
                else -> 32768
            }
        }

        // Type2 CharString Operators
        enum class Op(val id: Int) : Token {
            /* 0x00 Reserved */   /* 0x0c00 Reserved */
            hstem(0x01), /* 0x0c01 Reserved */
            /* 0x02 Reserved */   /* 0x0c02 Reserved */
            vstem(0x03), and(0x0c03),
            vmoveto(0x04), or(0x0c04),
            rlineto(0x05), not(0x0c05),
            hlineto(0x06), /* 0x0c06 Reserved */
            vlineto(0x07), /* 0x0c07 Reserved */
            rrcurveto(0x08), /* 0x0c08 Reserved */
            /* 0x09 Reserved */   abs(0x0c09),
            callsubr(0x0a), add(0x0c0a),
            `return`(0x0b), sub(0x0c0b),
            /* 0x0c escape   */   div(0x0c0c),
            /* 0x0d Reserved */   /* 0x0c0d Reserved */
            endchar(0x0e), neg(0x0c0e),
            cff2vsidx(0x0f), eq(0x0c0f),
            cff2blend(0x10), /* 0x0c10 Reserved */
            /* 0x11 Reserved */   /* 0x0c11 Reserved */
            hstemhm(0x12), drop(0x0c12),
            hintmask(0x13), /* 0x0c13 Reserved */
            cntrmask(0x14), put(0x0c14),
            rmoveto(0x15), get(0x0c15),
            hmoveto(0x16), ifelse(0x0c16),
            vstemhm(0x17), random(0x0c17),
            rcurveline(0x18), mul(0x0c18),
            rlinecurve(0x19), /* 0x0c19 Reserved */
            vvcurveto(0x1a), sqrt(0x0c1a),
            hhcurveto(0x1b), dup(0x0c1b),
            /* 0x1c short int */  exch(0x0c1c),
            callgsubr(0x1d), index(0x0c1d),
            vhcurveto(0x1e), roll(0x0c1e),
            hvcurveto(0x1f), /* 0x0c1f Reserved */
            /* 0x0c20 Reserved */
            /* 0x0c21 Reserved */
            hflex(0x0c22),
            flex(0x0c23),
            hflex1(0x0c24),
            flex1(0x0c25);

            companion object {
                val MAP = values().associateBy { it.id }.toIntMap()

                operator fun get(id: Int): Op = MAP[id] ?: error("Unknown CFF CS2 operator $id")
            }
        }
    }
}
