/*******************************************************************************
 * Copyright (c) 2009-2011 Luaj.org. All rights reserved.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package org.luaj.vm2


import org.luaj.vm2.internal.*
import org.luaj.vm2.io.*

import org.luaj.vm2.lib.MathLib
import kotlin.jvm.*
import kotlin.native.concurrent.*

/**
 * Subclass of [LuaValue] for representing lua strings.
 *
 *
 * Because lua string values are more nearly sequences of bytes than
 * sequences of characters or unicode code points, the [LuaString]
 * implementation holds the string value in an internal byte array.
 *
 *
 * [LuaString] values are not considered mutable once constructed,
 * so multiple [LuaString] values can chare a single byte array.
 *
 *
 * Currently [LuaString]s are pooled via a centrally managed weak table.
 * To ensure that as many string values as possible take advantage of this,
 * Constructors are not exposed directly.  As with number, booleans, and nil,
 * instance construction should be via [LuaValue.valueOf] or similar API.
 *
 *
 * Because of this pooling, users of LuaString *must not directly alter the
 * bytes in a LuaString*, or undefined behavior will result.
 *
 *
 * When Java Strings are used to initialize [LuaString] data, the UTF8 encoding is assumed.
 * The functions
 * [.lengthAsUtf8],
 * [.encodeToUtf8], and
 * [.decodeAsUtf8]
 * are used to convert back and forth between UTF8 byte arrays and character arrays.
 *
 * @see LuaValue
 *
 * @see LuaValue.valueOf
 * @see LuaValue.valueOf
 */
class LuaString
/** Construct a [LuaString] around a byte array without copying the contents.
 *
 *
 * The array is used directly after this is called, so clients must not change contents.
 *
 *
 * @param bytes byte buffer
 * @param offset offset into the byte buffer
 * @param length length of the byte buffer
 * @return [LuaString] wrapping the byte buffer
 */
private constructor(
    /** The bytes for the string.  These ***must not be mutated directly*** because
     * the backing may be shared by multiple LuaStrings, and the hash code is
     * computed only at construction time.
     * It is exposed only for performance and legacy reasons.  */
    @kotlin.jvm.JvmField val m_bytes: ByteArray,
    /** The offset into the byte array, 0 means start at the first byte  */
    @kotlin.jvm.JvmField val m_offset: Int,
    /** The number of bytes that comprise this string  */
    @kotlin.jvm.JvmField val m_length: Int
) : LuaValue() {

    /** The hashcode for this string.  Computed at construct time.  */
    private val m_hashcode: Int = hashCode(m_bytes, m_offset, m_length)

    /** Check that a byte sequence is valid UTF-8
     * @return true if it is valid UTF-8, otherwise false
     * @see .lengthAsUtf8
     * @see .encodeToUtf8
     * @see .decodeAsUtf8
     */
    val isValidUtf8: Boolean
        get() {
            var i = m_offset
            val j = m_offset + m_length
            while (i < j) {
                val c = m_bytes[i++].toInt()
                if (c >= 0) continue
                if (c and 0xE0 == 0xC0
                    && i < j
                    && m_bytes[i++].toInt() and 0xC0 == 0x80
                )
                    continue
                if (c and 0xF0 == 0xE0
                    && i + 1 < j
                    && m_bytes[i++].toInt() and 0xC0 == 0x80
                    && m_bytes[i++].toInt() and 0xC0 == 0x80
                )
                    continue
                return false
            }
            return true
        }

    override fun isstring(): Boolean = true
    override fun getmetatable(): LuaValue? = s_metatable
    override fun type(): Int = LuaValue.TSTRING
    override fun typename(): String = "string"
    override fun tojstring(): String = decodeAsUtf8(m_bytes, m_offset, m_length)

    // unary operators
    override fun neg(): LuaValue = scannumber().let { d -> if ((d).isNaN()) super.neg() else LuaValue.valueOf(-d) }

    // basic binary arithmetic
    override fun add(rhs: LuaValue): LuaValue = scannumber().let { d -> if ((d).isNaN()) arithmt(LuaValue.ADD, rhs) else rhs.add(d) }
    override fun add(rhs: Double): LuaValue = LuaValue.valueOf(checkarith() + rhs)
    override fun add(rhs: Int): LuaValue = LuaValue.valueOf(checkarith() + rhs)
    override fun sub(rhs: LuaValue): LuaValue = scannumber().let { d -> if ((d).isNaN()) arithmt(LuaValue.SUB, rhs) else rhs.subFrom(d) }
    override fun sub(rhs: Double): LuaValue = LuaValue.valueOf(checkarith() - rhs)
    override fun sub(rhs: Int): LuaValue = LuaValue.valueOf(checkarith() - rhs)
    override fun subFrom(lhs: Double): LuaValue = LuaValue.valueOf(lhs - checkarith())
    override fun mul(rhs: LuaValue): LuaValue = scannumber().let { d -> if ((d).isNaN()) arithmt(LuaValue.MUL, rhs) else rhs.mul(d) }
    override fun mul(rhs: Double): LuaValue = LuaValue.valueOf(checkarith() * rhs)
    override fun mul(rhs: Int): LuaValue = LuaValue.valueOf(checkarith() * rhs)
    override fun pow(rhs: LuaValue): LuaValue = scannumber().let { d -> if ((d).isNaN()) arithmt(LuaValue.POW, rhs) else rhs.powWith(d) }
    override fun pow(rhs: Double): LuaValue = MathLib.dpow(checkarith(), rhs)
    override fun pow(rhs: Int): LuaValue = MathLib.dpow(checkarith(), rhs.toDouble())
    override fun powWith(lhs: Double): LuaValue = MathLib.dpow(lhs, checkarith())
    override fun powWith(lhs: Int): LuaValue = MathLib.dpow(lhs.toDouble(), checkarith())
    override fun div(rhs: LuaValue): LuaValue = scannumber().let { d -> if ((d).isNaN()) arithmt(LuaValue.DIV, rhs) else rhs.divInto(d) }
    override fun div(rhs: Double): LuaValue = LuaDouble.ddiv(checkarith(), rhs)
    override fun div(rhs: Int): LuaValue = LuaDouble.ddiv(checkarith(), rhs.toDouble())
    override fun divInto(lhs: Double): LuaValue = LuaDouble.ddiv(lhs, checkarith())
    override fun mod(rhs: LuaValue): LuaValue = scannumber().let { d -> if ((d).isNaN()) arithmt(LuaValue.MOD, rhs) else rhs.modFrom(d) }
    override fun mod(rhs: Double): LuaValue = LuaDouble.dmod(checkarith(), rhs)
    override fun mod(rhs: Int): LuaValue = LuaDouble.dmod(checkarith(), rhs.toDouble())
    override fun modFrom(lhs: Double): LuaValue = LuaDouble.dmod(lhs, checkarith())

    // relational operators, these only work with other strings
    override fun lt(rhs: LuaValue): LuaValue = if (rhs.strcmp(this) > 0) LuaValue.BTRUE else LuaValue.BFALSE
    override fun lt_b(rhs: LuaValue): Boolean = rhs.strcmp(this) > 0

    override fun lt_b(rhs: Int): Boolean = typerror("attempt to compare string with number")
    override fun lt_b(rhs: Double): Boolean = typerror("attempt to compare string with number")
    override fun lteq(rhs: LuaValue): LuaValue = if (rhs.strcmp(this) >= 0) LuaValue.BTRUE else LuaValue.BFALSE
    override fun lteq_b(rhs: LuaValue): Boolean = rhs.strcmp(this) >= 0
    override fun lteq_b(rhs: Int): Boolean = typerror("attempt to compare string with number")
    override fun lteq_b(rhs: Double): Boolean = typerror("attempt to compare string with number")
    override fun gt(rhs: LuaValue): LuaValue = if (rhs.strcmp(this) < 0) LuaValue.BTRUE else LuaValue.BFALSE
    override fun gt_b(rhs: LuaValue): Boolean = rhs.strcmp(this) < 0
    override fun gt_b(rhs: Int): Boolean = typerror("attempt to compare string with number")
    override fun gt_b(rhs: Double): Boolean = typerror("attempt to compare string with number")
    override fun gteq(rhs: LuaValue): LuaValue = if (rhs.strcmp(this) <= 0) LuaValue.BTRUE else LuaValue.BFALSE
    override fun gteq_b(rhs: LuaValue): Boolean = rhs.strcmp(this) <= 0
    override fun gteq_b(rhs: Int): Boolean = typerror("attempt to compare string with number")
    override fun gteq_b(rhs: Double): Boolean = typerror("attempt to compare string with number")

    // concatenation
    override fun concat(rhs: LuaValue): LuaValue = rhs.concatTo(this)
    override fun concat(rhs: Buffer): Buffer = rhs.concatTo(this)
    override fun concatTo(lhs: LuaNumber): LuaValue = concatTo(lhs.strvalue()!!)
    override fun concatTo(lhs: LuaString): LuaValue {
        val b = ByteArray(lhs.m_length + this.m_length)
        arraycopy(lhs.m_bytes, lhs.m_offset, b, 0, lhs.m_length)
        arraycopy(this.m_bytes, this.m_offset, b, lhs.m_length, this.m_length)
        return valueUsing(b, 0, b.size)
    }

    // string comparison
    override fun strcmp(lhs: LuaValue): Int = -lhs.strcmp(this)

    override fun strcmp(rhs: LuaString): Int {
        var i = 0
        var j = 0
        while (i < m_length && j < rhs.m_length) {
            if (m_bytes[m_offset + i] != rhs.m_bytes[rhs.m_offset + j]) {
                return m_bytes[m_offset + i].toInt() - rhs.m_bytes[rhs.m_offset + j].toInt()
            }
            ++i
            ++j
        }
        return m_length - rhs.m_length
    }

    /** Check for number in arithmetic, or throw aritherror  */
    private fun checkarith(): Double {
        val d = scannumber()
        if ((d).isNaN()) aritherror()
        return d
    }

    override fun checkint(): Int = checkdouble().toLong().toInt()
    override fun checkinteger(): LuaInteger? = LuaValue.valueOf(checkint())
    override fun checklong(): Long = checkdouble().toLong()

    override fun checkdouble(): Double {
        val d = scannumber()
        if ((d).isNaN()) argerror("number")
        return d
    }

    override fun checknumber(): LuaNumber? = LuaValue.valueOf(checkdouble())
    override fun checknumber(msg: String): LuaNumber {
        val d = scannumber()
        if ((d).isNaN())
            LuaValue.error(msg)
        return LuaValue.valueOf(d)
    }

    override fun isnumber(): Boolean = scannumber().let { d -> !(d.isNaN()) }

    override fun isint(): Boolean {
        val d = scannumber()
        if (d.isNaN())
            return false
        val i = d.toInt()
        return i.toDouble() == d
    }

    override fun islong(): Boolean {
        val d = scannumber()
        if ((d.isNaN()))
            return false
        val l = d.toLong()
        return l.toDouble() == d
    }

    override fun tobyte(): Byte = toint().toByte()
    override fun tochar(): Char = toint().toChar()

    override fun todouble(): Double = scannumber().let { d -> if ((d.isNaN())) 0.0 else d }
    override fun tofloat(): Float = todouble().toFloat()
    override fun toint(): Int = tolong().toInt()
    override fun tolong(): Long = todouble().toLong()
    override fun toshort(): Short = toint().toShort()
    override fun optdouble(defval: Double): Double = checknumber()!!.checkdouble()
    override fun optint(defval: Int): Int = checknumber()!!.checkint()
    override fun optinteger(defval: LuaInteger?): LuaInteger? = checknumber()!!.checkinteger()
    override fun optlong(defval: Long): Long = checknumber()!!.checklong()
    override fun optnumber(defval: LuaNumber?): LuaNumber? = checknumber()!!.checknumber()
    override fun optstring(defval: LuaString?): LuaString? = this
    override fun tostring(): LuaValue = this
    override fun optjstring(defval: String?): String? = tojstring()
    override fun strvalue(): LuaString? = this

    /** Take a substring using Java zero-based indexes for begin and end or range.
     * @param beginIndex  The zero-based index of the first character to include.
     * @param endIndex  The zero-based index of position after the last character.
     * @return LuaString which is a substring whose first character is at offset
     * beginIndex and extending for (endIndex - beginIndex ) characters.
     */
    fun substring(beginIndex: Int, endIndex: Int): LuaString {
        val off = m_offset + beginIndex
        val len = endIndex - beginIndex
        return if (len >= m_length / 2) valueUsing(m_bytes, off, len) else valueOf(m_bytes, off, len)
    }

    override fun hashCode(): Int = m_hashcode

    // object comparison, used in key comparison
    override fun equals(o: Any?): Boolean = if (o is LuaString) raweq(o) else false

    // equality w/ metatable processing
    override fun eq(`val`: LuaValue): LuaValue = if (`val`.raweq(this)) LuaValue.BTRUE else LuaValue.BFALSE
    override fun eq_b(`val`: LuaValue): Boolean = `val`.raweq(this)

    // equality w/o metatable processing
    override fun raweq(`val`: LuaValue): Boolean = `val`.raweq(this)

    override fun raweq(s: LuaString): Boolean {
        if (this === s) return true
        if (s.m_length != m_length) return false
        if (s.m_bytes == m_bytes && s.m_offset == m_offset) return true
        if (s.hashCode() != hashCode()) return false
        for (i in 0 until m_length) if (s.m_bytes[s.m_offset + i] != m_bytes[m_offset + i]) return false
        return true
    }

    /** Return true if the bytes in the supplied range match this LuaStrings bytes.  */
    private fun byteseq(bytes: ByteArray, off: Int, len: Int): Boolean = m_length == len && equals(m_bytes, m_offset, bytes, off, len)
    fun write(writer: LuaBinOutput, i: Int, len: Int) = writer.write(m_bytes, m_offset + i, len)
    override fun len(): LuaValue = LuaInteger.valueOf(m_length)
    override fun length(): Int = m_length
    override fun rawlen(): Int = m_length
    fun luaByte(index: Int): Int = m_bytes[m_offset + index].toInt() and 0x0FF
    fun charAt(index: Int): Int {
        if (index < 0 || index >= m_length) throw IndexOutOfBoundsException()
        return luaByte(index)
    }

    override fun checkjstring(): String? = tojstring()
    override fun checkstring(): LuaString = this

    /** Convert value to an input stream.
     *
     * @return [InputStream] whose data matches the bytes in this [LuaString]
     */
    fun toLuaBinInput(): BytesLuaBinInput = BytesLuaBinInput(m_bytes, m_offset, m_length)

    /**
     * Copy the bytes of the string into the given byte array.
     * @param strOffset offset from which to copy
     * @param bytes destination byte array
     * @param arrayOffset offset in destination
     * @param len number of bytes to copy
     */
    fun copyInto(strOffset: Int, bytes: ByteArray, arrayOffset: Int, len: Int) =
        arraycopy(m_bytes, m_offset + strOffset, bytes, arrayOffset, len)

    /** Java version of strpbrk - find index of any byte that in an accept string.
     * @param accept [LuaString] containing characters to look for.
     * @return index of first match in the `accept` string, or -1 if not found.
     */
    fun indexOfAny(accept: LuaString): Int {
        val ilimit = m_offset + m_length
        val jlimit = accept.m_offset + accept.m_length
        for (i in m_offset until ilimit) {
            for (j in accept.m_offset until jlimit) {
                if (m_bytes[i] == accept.m_bytes[j]) {
                    return i - m_offset
                }
            }
        }
        return -1
    }

    /**
     * Find the index of a byte starting at a point in this string
     * @param b the byte to look for
     * @param start the first index in the string
     * @return index of first match found, or -1 if not found.
     */
    fun indexOf(b: Byte, start: Int): Int {
        for (i in start until m_length) if (m_bytes[m_offset + i] == b) return i
        return -1
    }

    /**
     * Find the index of a string starting at a point in this string
     * @param s the string to search for
     * @param start the first index in the string
     * @return index of first match found, or -1 if not found.
     */
    fun indexOf(s: LuaString, start: Int): Int {
        val slen = s.length()
        val limit = m_length - slen
        for (i in start..limit) if (equals(m_bytes, m_offset + i, s.m_bytes, s.m_offset, slen)) return i
        return -1
    }

    /**
     * Find the last index of a string in this string
     * @param s the string to search for
     * @return index of last match found, or -1 if not found.
     */
    fun lastIndexOf(s: LuaString): Int {
        val slen = s.length()
        val limit = m_length - slen
        for (i in limit downTo 0) if (equals(m_bytes, m_offset + i, s.m_bytes, s.m_offset, slen)) return i
        return -1
    }

    // --------------------- number conversion -----------------------

    /**
     * convert to a number using baee 10 or base 16 if it starts with '0x',
     * or NIL if it can't be converted
     * @return IntValue, DoubleValue, or NIL depending on the content of the string.
     * @see LuaValue.tonumber
     */
    override fun tonumber(): LuaValue {
        val d = scannumber()
        return if ((d.isNaN())) LuaValue.NIL else LuaValue.valueOf(d)
    }

    /**
     * convert to a number using a supplied base, or NIL if it can't be converted
     * @param base the base to use, such as 10
     * @return IntValue, DoubleValue, or NIL depending on the content of the string.
     * @see LuaValue.tonumber
     */
    fun tonumber(base: Int): LuaValue {
        val d = scannumber(base)
        return if ((d.isNaN())) LuaValue.NIL else LuaValue.valueOf(d)
    }

    /**
     * Convert to a number in base 10, or base 16 if the string starts with '0x',
     * or return Double.NaN if it cannot be converted to a number.
     * @return double value if conversion is valid, or Double.NaN if not
     */
    fun scannumber(): Double {
        var i = m_offset
        var j = m_offset + m_length
        while (i < j && m_bytes[i] == ' '.toByte()) ++i
        while (i < j && m_bytes[j - 1] == ' '.toByte()) --j
        if (i >= j)
            return Double.NaN
        if (m_bytes[i] == '0'.toByte() && i + 1 < j && (m_bytes[i + 1] == 'x'.toByte() || m_bytes[i + 1] == 'X'.toByte()))
            return scanlong(16, i + 2, j)
        val l = scanlong(10, i, j)
        return if ((l.isNaN())) scandouble(i, j) else l
    }

    /**
     * Convert to a number in a base, or return Double.NaN if not a number.
     * @param base the base to use between 2 and 36
     * @return double value if conversion is valid, or Double.NaN if not
     */
    fun scannumber(base: Int): Double {
        if (base < 2 || base > 36)
            return Double.NaN
        var i = m_offset
        var j = m_offset + m_length
        while (i < j && m_bytes[i] == ' '.toByte()) ++i
        while (i < j && m_bytes[j - 1] == ' '.toByte()) --j
        return if (i >= j) Double.NaN else scanlong(base, i, j)
    }

    /**
     * Scan and convert a long value, or return Double.NaN if not found.
     * @param base the base to use, such as 10
     * @param start the index to start searching from
     * @param end the first index beyond the search range
     * @return double value if conversion is valid,
     * or Double.NaN if not
     */
    private fun scanlong(base: Int, start: Int, end: Int): Double {
        var x: Long = 0
        val neg = m_bytes[start] == '-'.toByte()
        for (i in (if (neg) start + 1 else start) until end) {
            val digit = (m_bytes[i].toInt() and 0xFF) - if (base <= 10 || m_bytes[i] >= '0'.toByte() && m_bytes[i] <= '9'.toByte())
                '0'.toInt()
            else if (m_bytes[i] >= 'A'.toByte() && m_bytes[i] <= 'Z'.toByte()) 'A'.toInt() - 10 else 'a'.toInt() - 10
            if (digit < 0 || digit >= base)
                return Double.NaN
            x = x * base + digit
            if (x < 0)
                return Double.NaN // overflow
        }
        return (if (neg) -x else x).toDouble()
    }

    /**
     * Scan and convert a double value, or return Double.NaN if not a double.
     * @param start the index to start searching from
     * @param end the first index beyond the search range
     * @return double value if conversion is valid,
     * or Double.NaN if not
     */
    private fun scandouble(start: Int, end: Int): Double {
        var end = end
        if (end > start + 64) end = start + 64
        for (i in start until end) {
            when (m_bytes[i].toChar()) {
                '-', '+', '.', 'e', 'E', '0', '1', '2', '3', '4', '5', '6', '7', '8', '9' -> {
                }
                else -> return Double.NaN
            }
        }
        val c = CharArray(end - start)
        for (i in start until end)
            c[i - start] = m_bytes[i].toChar()
        try {
            return (c.concatToString()).toDouble()
        } catch (e: Exception) {
            return Double.NaN
        }

    }

    /**
     * Print the bytes of the LuaString to a PrintStream as if it were
     * an ASCII string, quoting and escaping control characters.
     * @param ps PrintStream to print to.
     */
    fun printToStream(ps: LuaWriter) {
        var i = 0
        val n = m_length
        while (i < n) {
            val c = m_bytes[m_offset + i].toInt()
            ps.print(c.toChar())
            i++
        }
    }

    companion object {

        /** The singleton instance for string metatables that forwards to the string functions.
         * Typically, this is set to the string metatable as a side effect of loading the string
         * library, and is read-write to provide flexible behavior by default.  When used in a
         * server environment where there may be roge scripts, this should be replaced with a
         * read-only table since it is shared across all lua code in this Java VM.
         */
        var s_metatable: LuaValue?
            get() = LuaString_metatable
            set(value) {
                LuaString_metatable = value
            }

        /** Size of cache of recent short strings. This is the maximum number of LuaStrings that
         * will be retained in the cache of recent short strings.  Exposed to package for testing.  */
        const val RECENT_STRINGS_CACHE_SIZE = 128

        /** Maximum length of a string to be considered for recent short strings caching.
         * This effectively limits the total memory that can be spent on the recent strings cache,
         * because no LuaString whose backing exceeds this length will be put into the cache.
         * Exposed to package for testing.  */
        const val RECENT_STRINGS_MAX_LENGTH = 32

        /**
         * Get a [LuaString] instance whose bytes match
         * the supplied Java String using the UTF8 encoding.
         * @param string Java String containing characters to encode as UTF8
         * @return [LuaString] with UTF8 bytes corresponding to the supplied String
         */
        @OptIn(ExperimentalStdlibApi::class)
        @JvmName("valueOf2")
        fun valueOf(string: String): LuaString {
            val c = string.toCharArray()
            val b = ByteArray(lengthAsUtf8(c))
            encodeToUtf8(c, c.size, b, 0)
            return valueUsing(b, 0, b.size)
        }

        /** Construct a [LuaString] for a portion of a byte array.
         *
         *
         * The array is first be used as the backing for this object, so clients must not change contents.
         * If the supplied value for 'len' is more than half the length of the container, the
         * supplied byte array will be used as the backing, otherwise the bytes will be copied to a
         * new byte array, and cache lookup may be performed.
         *
         *
         * @param bytes byte buffer
         * @param off offset into the byte buffer
         * @param len length of the byte buffer
         * @return [LuaString] wrapping the byte buffer
         */
        @JvmName("valueOf2")

        fun valueOf(bytes: ByteArray, off: Int, len: Int, runtime: LuaRuntime? = LuaRuntime.default): LuaString {
            if (len > RECENT_STRINGS_MAX_LENGTH)
                return valueFromCopy(bytes, off, len)
            val hash = hashCode(bytes, off, len)
            val bucket = hash and RECENT_STRINGS_CACHE_SIZE - 1
            val t = runtime?.recent_short_strings?.get(bucket)
            if (t != null && t.m_hashcode == hash && t.byteseq(bytes, off, len)) return t
            return valueFromCopy(bytes, off, len).also {
                if (runtime != null) runtime.recent_short_strings[bucket] = it
            }
        }

        /** Construct a new LuaString using a copy of the bytes array supplied  */

        private fun valueFromCopy(bytes: ByteArray, off: Int, len: Int): LuaString {
            val copy = ByteArray(len)
            for (i in 0 until len) copy[i] = bytes[off + i]
            return LuaString(copy, 0, len)
        }

        /** Construct a [LuaString] around, possibly using the the supplied
         * byte array as the backing store.
         *
         *
         * The caller must ensure that the array is not mutated after the call.
         * However, if the string is short enough the short-string cache is checked
         * for a match which may be used instead of the supplied byte array.
         *
         *
         * @param bytes byte buffer
         * @return [LuaString] wrapping the byte buffer, or an equivalent string.
         */
        @JvmOverloads

        fun valueUsing(bytes: ByteArray, off: Int = 0, len: Int = bytes.size, runtime: LuaRuntime? = LuaRuntime.default): LuaString {
            if (bytes.size > RECENT_STRINGS_MAX_LENGTH)
                return LuaString(bytes, off, len)
            val hash = hashCode(bytes, off, len)
            val bucket = hash and RECENT_STRINGS_CACHE_SIZE - 1
            val t = runtime?.recent_short_strings?.get(bucket)
            if (t != null && t.m_hashcode == hash && t.byteseq(bytes, off, len)) return t
            return LuaString(bytes, off, len).also {
                if (runtime != null) {
                    runtime.recent_short_strings[bucket] = it
                }
            }
        }

        /** Construct a [LuaString] using the supplied characters as byte values.
         *
         *
         * Only the low-order 8-bits of each character are used, the remainder is ignored.
         *
         *
         * This is most useful for constructing byte sequences that do not conform to UTF8.
         * @param bytes array of char, whose values are truncated at 8-bits each and put into a byte array.
         * @return [LuaString] wrapping a copy of the byte buffer
         */
        @JvmOverloads

        fun valueOf(bytes: CharArray, off: Int = 0, len: Int = bytes.size): LuaString {
            val b = ByteArray(len)
            for (i in 0 until len)
                b[i] = bytes[i + off].toByte()
            return valueUsing(b, 0, len)
        }

        /** Construct a [LuaString] for all the bytes in a byte array.
         *
         *
         * The LuaString returned will either be a new LuaString containing a copy
         * of the bytes array, or be an existing LuaString used already having the same value.
         *
         *
         * @param bytes byte buffer
         * @return [LuaString] wrapping the byte buffer
         */
        @JvmName("valueOf2")

        fun valueOf(bytes: ByteArray): LuaString {
            return valueOf(bytes, 0, bytes.size)
        }

        /** Compute the hash code of a sequence of bytes within a byte array using
         * lua's rules for string hashes.  For long strings, not all bytes are hashed.
         * @param bytes  byte array containing the bytes.
         * @param offset  offset into the hash for the first byte.
         * @param length number of bytes starting with offset that are part of the string.
         * @return hash for the string defined by bytes, offset, and length.
         */

        fun hashCode(bytes: ByteArray, offset: Int, length: Int): Int {
            var h = length  /* seed */
            val step = (length shr 5) + 1  /* if string is too long, don't hash all its chars */
            var l1 = length
            while (l1 >= step) {
                /* compute hash */
                h = h xor (h shl 5) + (h shr 2) + (bytes[offset + l1 - 1].toInt() and 0x0FF)
                l1 -= step
            }
            return h
        }


        fun equals(a: LuaString, i: Int, b: LuaString, j: Int, n: Int): Boolean {
            return equals(a.m_bytes, a.m_offset + i, b.m_bytes, b.m_offset + j, n)
        }


        fun equals(a: ByteArray, i: Int, b: ByteArray, j: Int, n: Int): Boolean {
            var i = i
            var j = j
            var n = n
            if (a.size < i + n || b.size < j + n) return false
            while (--n >= 0) if (a[i++] != b[j++]) return false
            return true
        }


        /**
         * Convert to Java String interpreting as utf8 characters.
         *
         * @param bytes byte array in UTF8 encoding to convert
         * @param offset starting index in byte array
         * @param length number of bytes to convert
         * @return Java String corresponding to the value of bytes interpreted using UTF8
         * @see .lengthAsUtf8
         * @see .encodeToUtf8
         * @see .isValidUtf8
         */

        fun decodeAsUtf8(bytes: ByteArray, offset: Int, length: Int): String {
            var i: Int
            var j: Int
            var n: Int
            var b: Int
            i = offset
            j = offset + length
            n = 0
            while (i < j) {
                when (0xE0 and bytes[i++].toInt()) {
                    0xE0 -> {
                        ++i
                        ++i
                    }
                    0xC0 -> ++i
                }
                ++n
            }
            val chars = CharArray(n)
            i = offset
            j = offset + length
            n = 0
            while (i < j) {
                chars[n++] = (if ((run { b = bytes[i++].toInt(); b }) >= 0 || i >= j)
                    b
                else if (b < -32 || i + 1 >= j)
                    b and 0x3f shl 6 or (bytes[i++].toInt() and 0x3f)
                else
                    b and 0xf shl 12 or (bytes[i++].toInt() and 0x3f shl 6) or (bytes[i++].toInt() and 0x3f)).toChar()
            }
            return chars.concatToString()
        }

        /**
         * Count the number of bytes required to encode the string as UTF-8.
         * @param chars Array of unicode characters to be encoded as UTF-8
         * @return count of bytes needed to encode using UTF-8
         * @see .encodeToUtf8
         * @see .decodeAsUtf8
         * @see .isValidUtf8
         */

        fun lengthAsUtf8(chars: CharArray): Int {
            var i: Int
            var c: Char
            var b = chars.size
            i = b
            while (--i >= 0) if ((run { c = chars[i]; c }).toInt() >= 0x80) b += if (c.toInt() >= 0x800) 2 else 1
            return b
        }

        /**
         * Encode the given Java string as UTF-8 bytes, writing the result to bytes
         * starting at offset.
         *
         *
         * The string should be measured first with lengthAsUtf8
         * to make sure the given byte array is large enough.
         * @param chars Array of unicode characters to be encoded as UTF-8
         * @param nchars Number of characters in the array to convert.
         * @param bytes byte array to hold the result
         * @param off offset into the byte array to start writing
         * @return number of bytes converted.
         * @see .lengthAsUtf8
         * @see .decodeAsUtf8
         * @see .isValidUtf8
         */

        fun encodeToUtf8(chars: CharArray, nchars: Int, bytes: ByteArray, off: Int): Int {
            var c: Char
            var j = off
            for (i in 0 until nchars) {
                when {
                    (run { c = chars[i]; c }).toInt() < 0x80 -> bytes[j++] = c.toByte()
                    c.toInt() < 0x800 -> {
                        bytes[j++] = (0xC0 or (c.toInt() shr 6 and 0x1f)).toByte()
                        bytes[j++] = (0x80 or (c.toInt() and 0x3f)).toByte()
                    }
                    else -> {
                        bytes[j++] = (0xE0 or (c.toInt() shr 12 and 0x0f)).toByte()
                        bytes[j++] = (0x80 or (c.toInt() shr 6 and 0x3f)).toByte()
                        bytes[j++] = (0x80 or (c.toInt() and 0x3f)).toByte()
                    }
                }
            }
            return j - off
        }
    }
}
/** Construct a [LuaString] using the supplied characters as byte values.
 *
 *
 * Only the low-order 8-bits of each character are used, the remainder is ignored.
 *
 *
 * This is most useful for constructing byte sequences that do not conform to UTF8.
 * @param bytes array of char, whose values are truncated at 8-bits each and put into a byte array.
 * @return [LuaString] wrapping a copy of the byte buffer
 */
/** Construct a [LuaString] for all the bytes in a byte array, possibly using
 * the supplied array as the backing store.
 *
 *
 * The LuaString returned will either be a new LuaString containing the byte array,
 * or be an existing LuaString used already having the same value.
 *
 *
 * The caller must not mutate the contents of the byte array after this call, as
 * it may be used elsewhere due to recent short string caching.
 * @param bytes byte buffer
 * @return [LuaString] wrapping the byte buffer
 */

@ThreadLocal
private var LuaString_metatable: LuaValue? = null
