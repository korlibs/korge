package com.soywiz.korim.font

import com.soywiz.kds.DoubleArrayList
import com.soywiz.kds.mapInt
import com.soywiz.kds.toIntMap
import com.soywiz.kmem.toInt
import com.soywiz.korim.vector.Context2d
import com.soywiz.korio.lang.assert
import com.soywiz.korio.lang.readStringz
import com.soywiz.korio.lang.reserved
import com.soywiz.korio.lang.unreachable
import com.soywiz.korio.stream.FastByteArrayInputStream
import com.soywiz.korio.stream.openFastStream
import com.soywiz.korma.geom.vector.rLineTo
import kotlin.math.abs
import kotlin.math.sqrt
import kotlin.random.Random

/**
 * Compact Font Format font representation (also known as a PostScript Type 1, or CIDFont)
 * https://docs.microsoft.com/en-us/typography/opentype/spec/cff
 * - https://partners.adobe.com/public/developer/en/font/5176.CFF.pdf
 * - https://adobe-type-tools.github.io/font-tech-notes/pdfs/5177.Type2.pdf
 */
object TtfCIDFont {
    /**
     * https://partners.adobe.com/public/developer/en/font/5176.CFF.pdf
     *
     * https://github.com/caryll/otfcc/blob/617837bc6a74b0cf6be9e77a8ed1d2ffb39ea425/lib/table/CFF.c
     */
    object CFF {
        fun FastByteArrayInputStream.readCFF() {
            readHeader()
            val nameIndex = readIndex()
            println("nameIndex=$nameIndex, str=${nameIndex[0].readStringz(0)}")

            val topDictIndex = readIndex()
            val topDictByte = topDictIndex[0][1]
            println("topDictByte=${topDictByte.toUByte()}")
            val topDict = topDictIndex[0].openFastStream().also { it.skip(0) }.readDICTMap()

            println("topDict=$topDictIndex")
            println("topDict=$topDict")

            val stringIndex = readIndex()
            println("stringIndex=$stringIndex")
            println(stringIndex.toList().map { it.decodeToString() })

            val globalSubrIndex = readIndex()
            println("globalSubrIndex=$globalSubrIndex")

            val charStringsIndex = readIndex()
            println("charStringsIndex=$charStringsIndex")

            val privateDict = readDICTMap()
            println("privateDict=$privateDict")
        }

        fun FastByteArrayInputStream.readHeader() {
            val versionMajor = readU8()
            val versionMinor = readU8()
            val headerSize = readU8()
            val offsetSize = readU8()

            if (versionMajor > 1) error("Only supported CFF version 1")

            println("CFF.versionMajor/Minor: $versionMajor.$versionMinor : headerSize=$headerSize, offsetSize=$offsetSize")
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
            val size: Int get() = offsets.size - 1
            fun itemSize(index: Int): Int = offsets[index + 1] - offsets[index]
            operator fun get(index: Int): ByteArray = bytes.copyOfRange(offsets[index], offsets[index + 1])
            fun toList(): List<ByteArray> = (0 until size).map { this[it] }
            override fun iterator(): Iterator<ByteArray> = toList().iterator()

            override fun toString(): String = "DataIndex[$size](${offsets.toList()})(${(0 until size).map { itemSize(it) }})"
        }

        fun FastByteArrayInputStream.readIndex(): DataIndex {
            val count = readU16BE()
            if (count == 0) return DataIndex(intArrayOf(0), byteArrayOf())
            val offSize = readOffSize()
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
        enum class Op(val id: Int) : PostScriptType2.Token {
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
    object PostScriptType2 {
        sealed interface Token

        data class Value(val value: Double) : Token {
            constructor(value: Int) : this(value.toDouble())
        }

        var DoubleArrayList.last: Double
            get() = this[size - 1]
            set(value) { this[size - 1] = value }

        fun DoubleArrayList.pop(): Double = removeAt(size - 1)
        fun DoubleArrayList.push(value: Double): Unit { add(value) }

        inline fun DoubleArrayList.binop(block: (l: Double, r: Double) -> Double) {
            val r = pop()
            val l = pop()
            push(block(l, r))
        }

        inline fun DoubleArrayList.unop(block: (r: Double) -> Double) {
            last = block(last)
        }

        // https://github.com/caryll/otfcc/blob/617837bc6a74b0cf6be9e77a8ed1d2ffb39ea425/lib/libcff/cff-parser.c#L342
        fun eval(ctx: Context2d, s: FastByteArrayInputStream, heap: DoubleArray = DoubleArray(32)) {
            val stack = DoubleArrayList(32)

            while (true) {
                val v = s.readCS2Token()
                when (v) {
                    is Op -> {
                        when (v) {
                            // Special
                            Op.`return` -> return
                            Op.callsubr -> TODO()
                            Op.callgsubr -> TODO()

                            // Stack & heap (32 elements)
                            Op.drop -> stack.pop()
                            Op.dup -> stack.pop().also { stack.add(it, it) }
                            Op.exch -> stack.swap(stack.size - 2, stack.size - 1)
                            Op.roll -> TODO()
                            Op.index -> TODO()
                            Op.put -> {
                                val index = stack.pop()
                                val value = stack.pop()
                                heap[index.toInt()] = value
                            }
                            Op.get -> stack.unop { heap[it.toInt()] }

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
                            Op.vmoveto -> TODO()
                            Op.rmoveto -> TODO()
                            Op.hmoveto -> TODO()
                            Op.rlineto -> {
                                for (n in 0 until stack.size step 2) ctx.rLineTo(stack[n], stack[n + 1])
                                stack.clear()
                            }
                            Op.hlineto -> TODO()
                            Op.vlineto -> TODO()
                            Op.rrcurveto -> TODO()
                            Op.rcurveline -> TODO()
                            Op.rlinecurve -> TODO()
                            Op.vvcurveto -> TODO()
                            Op.hhcurveto -> TODO()
                            Op.vhcurveto -> TODO()
                            Op.hvcurveto -> TODO()
                            Op.hflex -> TODO()
                            Op.flex -> TODO()
                            Op.hflex1 -> TODO()
                            Op.flex1 -> TODO()
                            Op.hstem, Op.vstem, Op.hstemhm, Op.vstemhm -> {
                                TODO()
                            }
                            // Other
                            Op.hintmask, Op.cntrmask -> TODO()
                            Op.endchar -> TODO()
                            Op.cff2vsidx -> TODO()
                            Op.cff2blend -> TODO()
                        }
                    }

                    is Value -> {
                        stack.add(v.value)
                    }

                    else -> Unit
                }
            }
        }


        // Table 1 Type 2 Charstring Encoding Values
        fun FastByteArrayInputStream.readCS2Token(): Token {
            val v0 = readU8()
            return when (v0) {
                // Operator
                in 0..27 -> Op(when (v0) {
                    12 -> (v0 shl 8) or readU8()
                    else -> v0
                })
                28 -> Value((readU8() shl 8) or readU8())
                in 29..31 -> Op(v0)
                in 32..254 -> Value(
                    when (v0) {
                        in 32..246 -> v0 - 139
                        in 247..250 -> (v0 - 247) * 256 + readU8() + 108
                        in 251..254 -> -((v0 - 251) * 256) - readU8() - 108
                        else -> unreachable
                    }
                )
                255 -> {
                    val integerPart = (readU8() shl 8) or readU8()
                    val fractionPart = (readU8() shl 8) or readU8()
                    Value(integerPart.toDouble() + fractionPart.toDouble() / 65536.0)
                }
                else -> unreachable
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

                operator fun invoke(id: Int): Op {
                    return MAP[id] ?: error("Unknown CFF CS2 operator $id")
                }
            }
        }
    }
}
