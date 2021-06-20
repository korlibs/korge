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
package org.luaj.vm2.lib

import org.luaj.vm2.LuaClosure
import org.luaj.vm2.Buffer
import org.luaj.vm2.LuaString
import org.luaj.vm2.LuaTable
import org.luaj.vm2.LuaValue
import org.luaj.vm2.Varargs
import org.luaj.vm2.compiler.DumpState
import org.luaj.vm2.internal.*
import org.luaj.vm2.io.*
import kotlin.math.*

/**
 * Subclass of [LibFunction] which implements the lua standard `string`
 * library.
 *
 *
 * Typically, this library is included as part of a call to either
 * [org.luaj.vm2.lib.jse.JsePlatform.standardGlobals] or [org.luaj.vm2.lib.jme.JmePlatform.standardGlobals]
 * <pre> `Globals globals = JsePlatform.standardGlobals();
 * System.out.println( globals.get("string").get("upper").call( LuaValue.valueOf("abcde") ) );
` *  </pre>
 *
 *
 * To instantiate and use it directly,
 * link it into your globals table via [LuaValue.load] using code such as:
 * <pre> `Globals globals = new Globals();
 * globals.load(new JseBaseLib());
 * globals.load(new PackageLib());
 * globals.load(new StringLib());
 * System.out.println( globals.get("string").get("upper").call( LuaValue.valueOf("abcde") ) );
` *  </pre>
 *
 *
 * This is a direct port of the corresponding library in C.
 * @see LibFunction
 *
 * @see org.luaj.vm2.lib.jse.JsePlatform
 *
 * @see org.luaj.vm2.lib.jme.JmePlatform
 *
 * @see [Lua 5.2 String Lib Reference](http://www.lua.org/manual/5.2/manual.html.6.4)
 */
/** Construct a StringLib, which can be initialized by calling it with a
 * modname string, and a global environment table as arguments using
 * [.call].  */
class StringLib : TwoArgFunction() {

    /** Perform one-time initialization on the library by creating a table
     * containing the library functions, adding that table to the supplied environment,
     * adding the table to package.loaded, and returning table as the return value.
     * Creates a metatable that uses __INDEX to fall back on itself to support string
     * method operations.
     * If the shared strings metatable instance is null, will set the metatable as
     * the global shared metatable for strings.
     * <P>
     * All tables and metatables are read-write by default so if this will be used in
     * a server environment, sandboxing should be used.  In particular, the
     * [LuaString.s_metatable] table should probably be made read-only.
     * @param modname the module name supplied if this is loaded via 'require'.
     * @param env the environment to load into, typically a Globals instance.
    </P> */
    override fun call(modname: LuaValue, env: LuaValue): LuaValue {
        val string = LuaTable()
        string["byte"] = Byte_()
        string["char"] = Char_()
        string["dump"] = Dump()
        string["find"] = Find()
        string["format"] = Format()
        string["gmatch"] = Gmatch()
        string["gsub"] = Gsub()
        string["len"] = Len()
        string["lower"] = Lower()
        string["match"] = Match()
        string["rep"] = Rep()
        string["reverse"] = Reverse()
        string["sub"] = Sub()
        string["upper"] = Upper()
        val mt = LuaValue.tableOf(
            arrayOf(LuaValue.INDEX, string)
        )
        env["string"] = string
        env["package"]["loaded"]["string"] = string
        //env.checkglobals().runtime
        if (LuaString.s_metatable == null)
            LuaString.s_metatable = mt
        return string
    }

    /**
     * string.byte (s [, i [, j]])
     *
     * Returns the internal numerical codes of the
     * characters s[i], s[i+1], ..., s[j]. The default value for i is 1; the
     * default value for j is i.
     *
     * Note that numerical codes are not necessarily portable across platforms.
     *
     * @param args the calling args
     */
    internal class Byte_ : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs {
            val s = args.checkstring(1)
            val l = s!!.m_length
            var posi = posrelat(args.optint(2, 1), l)
            var pose = posrelat(args.optint(3, posi), l)
            val n: Int
            var i: Int
            if (posi <= 0) posi = 1
            if (pose > l) pose = l
            if (posi > pose) return LuaValue.NONE  /* empty interval; return no values */
            n = pose - posi + 1
            if (posi + n <= pose)
            /* overflow? */
                LuaValue.error("string slice too long")
            return LuaValue.varargsOf(Array<LuaValue>(n) { LuaValue.valueOf(s.luaByte(posi + it - 1)) })
        }
    }

    /**
     * string.char (...)
     *
     * Receives zero or more integers. Returns a string with length equal
     * to the number of arguments, in which each character has the internal
     * numerical code equal to its corresponding argument.
     *
     * Note that numerical codes are not necessarily portable across platforms.
     *
     * @param args the calling VM
     */
    internal class Char_ : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs {
            val n = args.narg()
            val bytes = ByteArray(n)
            var i = 0
            var a = 1
            while (i < n) {
                val c = args.checkint(a)
                if (c < 0 || c >= 256) LuaValue.argerror(a, "invalid value")
                bytes[i] = c.toByte()
                i++
                a++
            }
            return LuaString.valueUsing(bytes)
        }
    }

    /**
     * string.dump (function)
     *
     * Returns a string containing a binary representation of the given function,
     * so that a later loadstring on this string returns a copy of the function.
     * function must be a Lua function without upvalues.
     *
     * TODO: port dumping code as optional add-on
     */
    internal class Dump : OneArgFunction() {
        override fun call(arg: LuaValue): LuaValue {
            val f = arg.checkfunction()
            val baos = ByteArrayLuaBinOutput()
            try {
                DumpState.dump((f as LuaClosure).p, baos, true)
                return LuaString.valueUsing(baos.toByteArray())
            } catch (e: IOException) {
                return LuaValue.error(e.message!!)
            }

        }
    }

    /**
     * string.find (s, pattern [, init [, plain]])
     *
     * Looks for the first match of pattern in the string s.
     * If it finds a match, then find returns the indices of s
     * where this occurrence starts and ends; otherwise, it returns nil.
     * A third, optional numerical argument init specifies where to start the search;
     * its default value is 1 and may be negative. A value of true as a fourth,
     * optional argument plain turns off the pattern matching facilities,
     * so the function does a plain "find substring" operation,
     * with no characters in pattern being considered "magic".
     * Note that if plain is given, then init must be given as well.
     *
     * If the pattern has captures, then in a successful match the captured values
     * are also returned, after the two indices.
     */
    internal class Find : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs {
            return str_find_aux(args, true)
        }
    }

    /**
     * string.format (formatstring, ...)
     *
     * Returns a formatted version of its variable number of arguments following
     * the description given in its first argument (which must be a string).
     * The format string follows the same rules as the printf family of standard C functions.
     * The only differences are that the options/modifiers *, l, L, n, p, and h are not supported
     * and that there is an extra option, q. The q option formats a string in a form suitable
     * to be safely read back by the Lua interpreter: the string is written between double quotes,
     * and all double quotes, newlines, embedded zeros, and backslashes in the string are correctly
     * escaped when written. For instance, the call
     * string.format('%q', 'a string with "quotes" and \n new line')
     *
     * will produce the string:
     * "a string with \"quotes\" and \
     * new line"
     *
     * The options c, d, E, e, f, g, G, i, o, u, X, and x all expect a number as argument,
     * whereas q and s expect a string.
     *
     * This function does not accept string values containing embedded zeros,
     * except as arguments to the q option.
     */
    internal class Format : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs {
            val fmt = args.checkstring(1)
            val n = fmt!!.length()
            val result = Buffer(n)
            var arg = 1
            var c: Int

            var i = 0
            while (i < n) {
                when (run { c = fmt.luaByte(i++); c }) {
                    '\n'.toInt() -> result.append("\n")
                    L_ESC -> if (i < n) {
                        if ((run { c = fmt.luaByte(i); c }) == L_ESC) {
                            ++i
                            result.append(L_ESC.toByte())
                        } else {
                            arg++
                            val fdsc = FormatDesc(args, fmt, i)
                            i += fdsc.length
                            when (fdsc.conversion.toChar()) {
                                'c' -> fdsc.format(result, args.checkint(arg).toByte())
                                'i', 'd' -> fdsc.format(result, args.checkint(arg).toLong())
                                'o', 'u', 'x', 'X' -> fdsc.format(result, args.checklong(arg))
                                'e', 'E', 'f', 'g', 'G' -> fdsc.format(result, args.checkdouble(arg))
                                'q' -> addquoted(result, args.checkstring(arg)!!)
                                's' -> {
                                    val s = args.checkstring(arg)
                                    if (fdsc.precision == -1 && s!!.length() >= 100) {
                                        result.append(s)
                                    } else {
                                        fdsc.format(result, s!!)
                                    }
                                }
                                else -> LuaValue.error("invalid option '%" + fdsc.conversion.toChar() + "' to 'format'")
                            }
                        }
                    }
                    else -> result.append(c.toByte())
                }
            }

            return result.tostring()
        }
    }

    internal class FormatDesc(args: Varargs, strfrmt: LuaString, start: Int) {

        private var leftAdjust: Boolean = false
        private var zeroPad: Boolean = false
        private var explicitPlus: Boolean = false
        private var space: Boolean = false
        private var alternateForm: Boolean = false

        private var width: Int = 0
        var precision: Int = 0

        val conversion: Int
        val length: Int

        init {
            var p = start
            val n = strfrmt.length()
            var c = 0

            var moreFlags = true
            while (moreFlags) {
                when (run { c = (if (p < n) strfrmt.luaByte(p++) else 0); c }.toChar()) {
                    '-' -> leftAdjust = true
                    '+' -> explicitPlus = true
                    ' ' -> space = true
                    '#' -> alternateForm = true
                    '0' -> zeroPad = true
                    else -> moreFlags = false
                }
            }
            if (p - start > MAX_FLAGS)
                LuaValue.error("invalid format (repeated flags)")

            width = -1
            if (c.toChar().isDigit()) {
                width = c - '0'.toInt()
                c = if (p < n) strfrmt.luaByte(p++) else 0
                if (c.toChar().isDigit()) {
                    width = width * 10 + (c - '0'.toInt())
                    c = if (p < n) strfrmt.luaByte(p++) else 0
                }
            }

            precision = -1
            if (c == '.'.toInt()) {
                c = if (p < n) strfrmt.luaByte(p++) else 0
                if (c.toChar().isDigit()) {
                    precision = c - '0'.toInt()
                    c = if (p < n) strfrmt.luaByte(p++) else 0
                    if (c.toChar().isDigit()) {
                        precision = precision * 10 + (c - '0'.toInt())
                        c = if (p < n) strfrmt.luaByte(p++) else 0
                    }
                }
            }

            if (c.toChar().isDigit())
                LuaValue.error("invalid format (width or precision too long)")

            zeroPad = zeroPad and !leftAdjust // '-' overrides '0'
            conversion = c
            length = p - start
        }

        fun format(buf: Buffer, c: Byte) {
            // TODO: not clear that any of width, precision, or flags apply here.
            buf.append(c)
        }

        fun format(buf: Buffer, number: Long) {
            var digits: String

            if (number == 0L && precision == 0) {
                digits = ""
            } else {
                val radix: Int
                when (conversion.toChar()) {
                    'x', 'X' -> radix = 16
                    'o' -> radix = 8
                    else -> radix = 10
                }
                digits = number.toString(radix)
                if (conversion == 'X'.toInt())
                    digits = digits.toUpperCase()
            }

            var minwidth = digits.length
            var ndigits = minwidth
            val nzeros: Int

            if (number < 0) {
                ndigits--
            } else if (explicitPlus || space) {
                minwidth++
            }

            if (precision > ndigits)
                nzeros = precision - ndigits
            else if (precision == -1 && zeroPad && width > minwidth)
                nzeros = width - minwidth
            else
                nzeros = 0

            minwidth += nzeros
            val nspaces = if (width > minwidth) width - minwidth else 0

            if (!leftAdjust)
                pad(buf, ' ', nspaces)

            if (number < 0) {
                if (nzeros > 0) {
                    buf.append('-'.toByte())
                    digits = digits.substring(1)
                }
            } else if (explicitPlus) {
                buf.append('+'.toByte())
            } else if (space) {
                buf.append(' '.toByte())
            }

            if (nzeros > 0)
                pad(buf, '0', nzeros)

            buf.append(digits)

            if (leftAdjust)
                pad(buf, ' ', nspaces)
        }

        fun format(buf: Buffer, x: Double) {
            // TODO
            buf.append(x.toString())
        }

        fun format(buf: Buffer, s: LuaString) {
            var s = s
            val nullindex = s.indexOf('\u0000'.toByte(), 0)
            if (nullindex != -1)
                s = s.substring(0, nullindex)
            buf.append(s)
        }

        companion object {
            private val MAX_FLAGS = 5

            fun pad(buf: Buffer, c: Char, n: Int) {
                var n = n
                val b = c.toByte()
                while (n-- > 0)
                    buf.append(b)
            }
        }
    }

    /**
     * string.gmatch (s, pattern)
     *
     * Returns an iterator function that, each time it is called, returns the next captures
     * from pattern over string s. If pattern specifies no captures, then the
     * whole match is produced in each call.
     *
     * As an example, the following loop
     * s = "hello world from Lua"
     * for w in string.gmatch(s, "%a+") do
     * print(w)
     * end
     *
     * will iterate over all the words from string s, printing one per line.
     * The next example collects all pairs key=value from the given string into a table:
     * t = {}
     * s = "from=world, to=Lua"
     * for k, v in string.gmatch(s, "(%w+)=(%w+)") do
     * t[k] = v
     * end
     *
     * For this function, a '^' at the start of a pattern does not work as an anchor,
     * as this would prevent the iteration.
     */
    internal class Gmatch : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs {
            val src = args.checkstring(1)
            val pat = args.checkstring(2)
            return GMatchAux(args, src!!, pat!!)
        }
    }

    internal class GMatchAux(args: Varargs, src: LuaString, pat: LuaString) : VarArgFunction() {
        private val srclen: Int = src.length()
        private val ms: MatchState = MatchState(args, src, pat)
        private var soffset: Int = 0

        override fun invoke(args: Varargs): Varargs {
            while (soffset < srclen) {
                ms.reset()
                val res = ms.match(soffset, 0)
                if (res >= 0) {
                    val soff = soffset
                    soffset = res
                    return ms.push_captures(true, soff, res)
                }
                soffset++
            }
            return LuaValue.NIL
        }
    }


    /**
     * string.gsub (s, pattern, repl [, n])
     * Returns a copy of s in which all (or the first n, if given) occurrences of the
     * pattern have been replaced by a replacement string specified by repl, which
     * may be a string, a table, or a function. gsub also returns, as its second value,
     * the total number of matches that occurred.
     *
     * If repl is a string, then its value is used for replacement.
     * The character % works as an escape character: any sequence in repl of the form %n,
     * with n between 1 and 9, stands for the value of the n-th captured substring (see below).
     * The sequence %0 stands for the whole match. The sequence %% stands for a single %.
     *
     * If repl is a table, then the table is queried for every match, using the first capture
     * as the key; if the pattern specifies no captures, then the whole match is used as the key.
     *
     * If repl is a function, then this function is called every time a match occurs,
     * with all captured substrings passed as arguments, in order; if the pattern specifies
     * no captures, then the whole match is passed as a sole argument.
     *
     * If the value returned by the table query or by the function call is a string or a number,
     * then it is used as the replacement string; otherwise, if it is false or nil,
     * then there is no replacement (that is, the original match is kept in the string).
     *
     * Here are some examples:
     * x = string.gsub("hello world", "(%w+)", "%1 %1")
     * --> x="hello hello world world"
     *
     * x = string.gsub("hello world", "%w+", "%0 %0", 1)
     * --> x="hello hello world"
     *
     * x = string.gsub("hello world from Lua", "(%w+)%s*(%w+)", "%2 %1")
     * --> x="world hello Lua from"
     *
     * x = string.gsub("home = $HOME, user = $USER", "%$(%w+)", os.getenv)
     * --> x="home = /home/roberto, user = roberto"
     *
     * x = string.gsub("4+5 = $return 4+5$", "%$(.-)%$", function (s)
     * return loadstring(s)()
     * end)
     * --> x="4+5 = 9"
     *
     * local t = {name="lua", version="5.1"}
     * x = string.gsub("$name-$version.tar.gz", "%$(%w+)", t)
     * --> x="lua-5.1.tar.gz"
     */
    internal class Gsub : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs {
            val src = args.checkstring(1)
            val srclen = src!!.length()
            val p = args.checkstring(2)
            val repl = args.arg(3)
            val max_s = args.optint(4, srclen + 1)
            val anchor = p!!.length() > 0 && p.charAt(0) == '^'.toInt()

            val lbuf = Buffer(srclen)
            val ms = MatchState(args, src, p)

            var soffset = 0
            var n = 0
            while (n < max_s) {
                ms.reset()
                val res = ms.match(soffset, if (anchor) 1 else 0)
                if (res != -1) {
                    n++
                    ms.add_value(lbuf, soffset, res, repl)
                }
                if (res != -1 && res > soffset)
                    soffset = res
                else if (soffset < srclen)
                    lbuf.append(src.luaByte(soffset++).toByte())
                else
                    break
                if (anchor)
                    break
            }
            lbuf.append(src.substring(soffset, srclen))
            return LuaValue.varargsOf(lbuf.tostring(), LuaValue.valueOf(n))
        }
    }

    /**
     * string.len (s)
     *
     * Receives a string and returns its length. The empty string "" has length 0.
     * Embedded zeros are counted, so "a\000bc\000" has length 5.
     */
    internal class Len : OneArgFunction() {
        override fun call(arg: LuaValue): LuaValue {
            return arg.checkstring()!!.len()
        }
    }

    /**
     * string.lower (s)
     *
     * Receives a string and returns a copy of this string with all uppercase letters
     * changed to lowercase. All other characters are left unchanged.
     * The definition of what an uppercase letter is depends on the current locale.
     */
    internal class Lower : OneArgFunction() {
        override fun call(arg: LuaValue): LuaValue {
            return LuaValue.valueOf(arg.checkjstring()!!.toLowerCase())
        }
    }

    /**
     * string.match (s, pattern [, init])
     *
     * Looks for the first match of pattern in the string s. If it finds one,
     * then match returns the captures from the pattern; otherwise it returns
     * nil. If pattern specifies no captures, then the whole match is returned.
     * A third, optional numerical argument init specifies where to start the
     * search; its default value is 1 and may be negative.
     */
    internal class Match : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs {
            return str_find_aux(args, false)
        }
    }

    /**
     * string.rep (s, n)
     *
     * Returns a string that is the concatenation of n copies of the string s.
     */
    internal class Rep : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs {
            val s = args.checkstring(1)
            val n = args.checkint(2)
            val bytes = ByteArray(s!!.length() * n)
            val len = s.length()
            var offset = 0
            while (offset < bytes.size) {
                s.copyInto(0, bytes, offset, len)
                offset += len
            }
            return LuaString.valueUsing(bytes)
        }
    }

    /**
     * string.reverse (s)
     *
     * Returns a string that is the string s reversed.
     */
    internal class Reverse : OneArgFunction() {
        override fun call(arg: LuaValue): LuaValue {
            val s = arg.checkstring()
            val n = s!!.length()
            val b = ByteArray(n)
            var i = 0
            var j = n - 1
            while (i < n) {
                b[j] = s.luaByte(i).toByte()
                i++
                j--
            }
            return LuaString.valueUsing(b)
        }
    }

    /**
     * string.sub (s, i [, j])
     *
     * Returns the substring of s that starts at i and continues until j;
     * i and j may be negative. If j is absent, then it is assumed to be equal to -1
     * (which is the same as the string length). In particular, the call
     * string.sub(s,1,j)
     * returns a prefix of s with length j, and
     * string.sub(s, -i)
     * returns a suffix of s with length i.
     */
    internal class Sub : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs {
            val s = args.checkstring(1)
            val l = s!!.length()

            var start = posrelat(args.checkint(2), l)
            var end = posrelat(args.optint(3, -1), l)

            if (start < 1)
                start = 1
            if (end > l)
                end = l

            return if (start <= end) {
                s.substring(start - 1, end)
            } else {
                LuaValue.EMPTYSTRING
            }
        }
    }

    /**
     * string.upper (s)
     *
     * Receives a string and returns a copy of this string with all lowercase letters
     * changed to uppercase. All other characters are left unchanged.
     * The definition of what a lowercase letter is depends on the current locale.
     */
    internal class Upper : OneArgFunction() {
        override fun call(arg: LuaValue): LuaValue {
            return LuaValue.valueOf(arg.checkjstring()!!.toUpperCase())
        }
    }

    internal class MatchState(val args: Varargs, val s: LuaString, val p: LuaString) {
        var level: Int = 0
        var cinit: IntArray = IntArray(MAX_CAPTURES)
        var clen: IntArray = IntArray(MAX_CAPTURES)

        fun reset() {
            level = 0
        }

        private fun add_s(lbuf: Buffer, news: LuaString, soff: Int, e: Int) {
            val l = news.length()
            var i = 0
            while (i < l) {
                var b = news.luaByte(i).toByte()
                if (b.toInt() != L_ESC) {
                    lbuf.append(b)
                } else {
                    ++i // skip ESC
                    b = news.luaByte(i).toByte()
                    if (!b.toChar().isDigit()) {
                        lbuf.append(b)
                    } else if (b == '0'.toByte()) {
                        lbuf.append(s.substring(soff, e))
                    } else {
                        lbuf.append(push_onecapture(b - '1'.toByte(), soff, e).strvalue()!!)
                    }
                }
                ++i
            }
        }

        fun add_value(lbuf: Buffer, soffset: Int, end: Int, repl: LuaValue) {
            var repl = repl
            when (repl.type()) {
                LuaValue.TSTRING, LuaValue.TNUMBER -> {
                    add_s(lbuf, repl.strvalue()!!, soffset, end)
                    return
                }

                LuaValue.TFUNCTION -> repl = repl.invoke(push_captures(true, soffset, end)).arg1()

                LuaValue.TTABLE ->
                    // Need to call push_onecapture here for the error checking
                    repl = repl[push_onecapture(0, soffset, end)]

                else -> {
                    LuaValue.error("bad argument: string/function/table expected")
                    return
                }
            }

            if (!repl.toboolean()) {
                repl = s.substring(soffset, end)
            } else if (!repl.isstring()) {
                LuaValue.error("invalid replacement value (a " + repl.typename() + ")")
            }
            lbuf.append(repl.strvalue()!!)
        }

        fun push_captures(wholeMatch: Boolean, soff: Int, end: Int): Varargs {
            val nlevels = if (this.level == 0 && wholeMatch) 1 else this.level
            when (nlevels) {
                0 -> return LuaValue.NONE
                1 -> return push_onecapture(0, soff, end)
            }
            val v = Array<LuaValue>(nlevels) { push_onecapture(it, soff, end) }
            return LuaValue.varargsOf(v)
        }

        private fun push_onecapture(i: Int, soff: Int, end: Int): LuaValue {
            if (i >= this.level) {
                return if (i == 0) {
                    s.substring(soff, end)
                } else {
                    LuaValue.error("invalid capture index")
                }
            } else {
                val l = clen[i]
                if (l == CAP_UNFINISHED) {
                    return LuaValue.error("unfinished capture")
                }
                if (l == CAP_POSITION) {
                    return LuaValue.valueOf(cinit[i] + 1)
                } else {
                    val begin = cinit[i]
                    return s.substring(begin, begin + l)
                }
            }
        }

        private fun check_capture(l: Int): Int {
            var l = l
            l -= '1'.toInt()
            if (l < 0 || l >= level || this.clen[l] == CAP_UNFINISHED) {
                LuaValue.error("invalid capture index")
            }
            return l
        }

        private fun capture_to_close(): Int {
            var level = this.level
            level--
            while (level >= 0) {
                if (clen[level] == CAP_UNFINISHED)
                    return level
                level--
            }
            LuaValue.error("invalid pattern capture")
            return 0
        }

        fun classend(poffset: Int): Int {
            var poffset = poffset
            when (p.luaByte(poffset++)) {
                L_ESC -> {
                    if (poffset == p.length()) {
                        LuaValue.error("malformed pattern (ends with %)")
                    }
                    return poffset + 1
                }

                '['.toInt() -> {
                    if (p.luaByte(poffset) == '^'.toInt()) poffset++
                    do {
                        if (poffset == p.length()) {
                            LuaValue.error("malformed pattern (missing ])")
                        }
                        if (p.luaByte(poffset++) == L_ESC && poffset != p.length())
                            poffset++
                    } while (p.luaByte(poffset) != ']'.toInt())
                    return poffset + 1
                }
                else -> return poffset
            }
        }

        fun matchbracketclass(c: Int, poff: Int, ec: Int): Boolean {
            var poff = poff
            var sig = true
            if (p.luaByte(poff + 1) == '^'.toInt()) {
                sig = false
                poff++
            }
            while (++poff < ec) {
                if (p.luaByte(poff) == L_ESC) {
                    poff++
                    if (match_class(c, p.luaByte(poff)))
                        return sig
                } else if (p.luaByte(poff + 1) == '-'.toInt() && poff + 2 < ec) {
                    poff += 2
                    if (p.luaByte(poff - 2) <= c && c <= p.luaByte(poff))
                        return sig
                } else if (p.luaByte(poff) == c) return sig
            }
            return !sig
        }

        fun singlematch(c: Int, poff: Int, ep: Int): Boolean {
            when (p.luaByte(poff)) {
                '.'.toInt() -> return true
                L_ESC -> return match_class(c, p.luaByte(poff + 1))
                '['.toInt() -> return matchbracketclass(c, poff, ep - 1)
                else -> return p.luaByte(poff) == c
            }
        }

        /**
         * Perform pattern matching. If there is a match, returns offset into s
         * where match ends, otherwise returns -1.
         */
        fun match(soffset: Int, poffset: Int): Int {
            var soffset = soffset
            var poffset = poffset
            loop2@while (true) {
                // Check if we are at the end of the pattern -
                // equivalent to the '\0' case in the C version, but our pattern
                // string is not NUL-terminated.
                if (poffset == p.length())
                    return soffset
                when (p.luaByte(poffset)) {
                    '('.toInt() -> return if (++poffset < p.length() && p.luaByte(poffset) == ')'.toInt())
                        start_capture(soffset, poffset + 1, CAP_POSITION)
                    else
                        start_capture(soffset, poffset, CAP_UNFINISHED)
                    ')'.toInt() -> return end_capture(soffset, poffset + 1)
                    L_ESC -> {
                        if (poffset + 1 == p.length())
                            LuaValue.error("malformed pattern (ends with '%')")
                        when (p.luaByte(poffset + 1)) {
                            'b'.toInt() -> {
                                soffset = matchbalance(soffset, poffset + 2)
                                if (soffset == -1) return -1
                                poffset += 4
                                continue@loop2
                            }
                            'f'.toInt() -> {
                                poffset += 2
                                if (p.luaByte(poffset) != '['.toInt()) {
                                    LuaValue.error("Missing [ after %f in pattern")
                                }
                                val ep = classend(poffset)
                                val previous = if (soffset == 0) -1 else s.luaByte(soffset - 1)
                                if (matchbracketclass(
                                        previous,
                                        poffset,
                                        ep - 1
                                    ) || matchbracketclass(s.luaByte(soffset), poffset, ep - 1)
                                )
                                    return -1
                                poffset = ep
                                continue@loop2
                            }
                            else -> {
                                val c = p.luaByte(poffset + 1)
                                if (c.toChar().isDigit()) {
                                    soffset = match_capture(soffset, c)
                                    return if (soffset == -1) -1 else match(soffset, poffset + 2)
                                }
                            }
                        }
                        if (poffset + 1 == p.length())
                            return if (soffset == s.length()) soffset else -1
                    }
                    '$'.toInt() -> if (poffset + 1 == p.length())
                        return if (soffset == s.length()) soffset else -1
                }
                val ep = classend(poffset)
                val m = soffset < s.length() && singlematch(s.luaByte(soffset), poffset, ep)
                val pc = if (ep < p.length()) p.luaByte(ep).toInt() else '\u0000'.toInt()

                when (pc) {
                    '?'.toInt() -> {
                        var res: Int = 0
                        if (m && (run { res = match(soffset + 1, ep + 1); res }) != -1)
                            return res
                        poffset = ep + 1
                        continue@loop2
                    }
                    '*'.toInt() -> return max_expand(soffset, poffset, ep)
                    '+'.toInt() -> return if (m) max_expand(soffset + 1, poffset, ep) else -1
                    '-'.toInt() -> return min_expand(soffset, poffset, ep)
                    else -> {
                        if (!m)
                            return -1
                        soffset++
                        poffset = ep
                        continue@loop2
                    }
                }
            }
        }

        fun max_expand(soff: Int, poff: Int, ep: Int): Int {
            var i = 0
            while (soff + i < s.length() && singlematch(s.luaByte(soff + i), poff, ep))
                i++
            while (i >= 0) {
                val res = match(soff + i, ep + 1)
                if (res != -1)
                    return res
                i--
            }
            return -1
        }

        fun min_expand(soff: Int, poff: Int, ep: Int): Int {
            var soff = soff
            while (true) {
                val res = match(soff, ep + 1)
                if (res != -1)
                    return res
                else if (soff < s.length() && singlematch(s.luaByte(soff), poff, ep))
                    soff++
                else
                    return -1
            }
        }

        fun start_capture(soff: Int, poff: Int, what: Int): Int {
            val res: Int
            val level = this.level
            if (level >= MAX_CAPTURES) {
                LuaValue.error("too many captures")
            }
            cinit[level] = soff
            clen[level] = what
            this.level = level + 1
            if ((run { res = match(soff, poff); res }) == -1)
                this.level--
            return res
        }

        fun end_capture(soff: Int, poff: Int): Int {
            val l = capture_to_close()
            val res: Int
            clen[l] = soff - cinit[l]
            if ((run { res = match(soff, poff); res }) == -1)
                clen[l] = CAP_UNFINISHED
            return res
        }

        fun match_capture(soff: Int, l: Int): Int {
            var l = l
            l = check_capture(l)
            val len = clen[l]
            return if (s.length() - soff >= len && LuaString.equals(s, cinit[l], s, soff, len))
                soff + len
            else
                -1
        }

        fun matchbalance(soff: Int, poff: Int): Int {
            var soff = soff
            val plen = p.length()
            if (poff == plen || poff + 1 == plen) {
                LuaValue.error("unbalanced pattern")
            }
            val slen = s.length()
            if (soff >= slen)
                return -1
            val b = p.luaByte(poff)
            if (s.luaByte(soff) != b)
                return -1
            val e = p.luaByte(poff + 1)
            var cont = 1
            while (++soff < slen) {
                if (s.luaByte(soff) == e) {
                    if (--cont == 0) return soff + 1
                } else if (s.luaByte(soff) == b) cont++
            }
            return -1
        }

        companion object {

            fun match_class(c: Int, cl: Int): Boolean {
                val lcl = cl.toChar().toLowerCase()
                val cdata = CHAR_TABLE[c].toInt()

                val res: Boolean
                when (lcl) {
                    'a' -> res = cdata and MASK_ALPHA != 0
                    'd' -> res = cdata and MASK_DIGIT != 0
                    'l' -> res = cdata and MASK_LOWERCASE != 0
                    'u' -> res = cdata and MASK_UPPERCASE != 0
                    'c' -> res = cdata and MASK_CONTROL != 0
                    'p' -> res = cdata and MASK_PUNCT != 0
                    's' -> res = cdata and MASK_SPACE != 0
                    'w' -> res = cdata and (MASK_ALPHA or MASK_DIGIT) != 0
                    'x' -> res = cdata and MASK_HEXDIGIT != 0
                    'z' -> res = c == 0
                    else -> return cl == c
                }
                return if (lcl.toInt() == cl) res else !res
            }
        }
    }

    companion object {

        private fun addquoted(buf: Buffer, s: LuaString) {
            var c: Int
            buf.append('"'.toByte())
            var i = 0
            val n = s.length()
            while (i < n) {
                when (run { c = s.luaByte(i); c }.toChar()) {
                    '"', '\\', '\n' -> {
                        buf.append('\\'.toByte())
                        buf.append(c.toByte())
                    }
                    else -> if (c <= 0x1F || c == 0x7F) {
                        buf.append('\\'.toByte())
                        if (i + 1 == n || s.luaByte(i + 1) < '0'.toInt() || s.luaByte(i + 1) > '9'.toInt()) {
                            buf.append(c.toString(10))
                        } else {
                            buf.append('0'.toByte())
                            buf.append(('0'.toInt() + c / 10).toChar().toByte())
                            buf.append(('0'.toInt() + c % 10).toChar().toByte())
                        }
                    } else {
                        buf.append(c.toByte())
                    }
                }
                i++
            }
            buf.append('"'.toByte())
        }

        private val FLAGS = "-+ #0"

        /**
         * This utility method implements both string.find and string.match.
         */
        internal fun str_find_aux(args: Varargs, find: Boolean): Varargs {
            val s = args.checkstring(1)
            val pat = args.checkstring(2)
            var init = args.optint(3, 1)

            if (init > 0) {
                init = min(init - 1, s!!.length())
            } else if (init < 0) {
                init = max(0, s!!.length() + init)
            }

            val fastMatch = find && (args.arg(4).toboolean() || pat!!.indexOfAny(SPECIALS) == -1)

            if (fastMatch) {
                val result = s!!.indexOf(pat!!, init)
                if (result != -1) {
                    return LuaValue.varargsOf(LuaValue.valueOf(result + 1), LuaValue.valueOf(result + pat.length()))
                }
            } else {
                val ms = MatchState(args, s!!, pat!!)

                var anchor = false
                var poff = 0
                if (pat!!.luaByte(0) == '^'.toInt()) {
                    anchor = true
                    poff = 1
                }

                var soff = init
                do {
                    val res: Int
                    ms.reset()
                    if ((run { res = ms.match(soff, poff); res }) != -1) {
                        return if (find) {
                            LuaValue.varargsOf(
                                LuaValue.valueOf(soff + 1),
                                LuaValue.valueOf(res),
                                ms.push_captures(false, soff, res)
                            )
                        } else {
                            ms.push_captures(true, soff, res)
                        }
                    }
                } while (soff++ < s!!.length() && !anchor)
            }
            return LuaValue.NIL
        }

        private fun posrelat(pos: Int, len: Int): Int {
            return if (pos >= 0) pos else len + pos + 1
        }

        // Pattern matching implementation

        private val L_ESC: Int = '%'.toInt()
        private val SPECIALS = LuaValue.valueOf("^$*+?.([%-")
        private const val MAX_CAPTURES = 32

        private const val CAP_UNFINISHED = -1
        private const val CAP_POSITION = -2

        private const val MASK_ALPHA: Int = 0x01
        private const val MASK_LOWERCASE: Int = 0x02
        private const val MASK_UPPERCASE: Int = 0x04
        private const val MASK_DIGIT: Int = 0x08
        private const val MASK_PUNCT: Int = 0x10
        private const val MASK_SPACE: Int = 0x20
        private const val MASK_CONTROL: Int = 0x40
        private const val MASK_HEXDIGIT: Int = 0x80

        private val CHAR_TABLE: ByteArray = ByteArray(256).also { CHAR_TABLE ->
            for (i in 0..255) {
                val c = i.toChar()
                CHAR_TABLE[i] = ((if (c.isDigit()) MASK_DIGIT else 0).toInt() or
                    (if (c.isLowerCase()) MASK_LOWERCASE else 0).toInt() or
                    (if (c.isUpperCase()) MASK_UPPERCASE else 0).toInt() or
                    (if (c < ' ' || c.toInt() == 0x7F) MASK_CONTROL else 0).toInt()).toByte()
                if (c >= 'a' && c <= 'f' || c >= 'A' && c <= 'F' || c >= '0' && c <= '9') {
                    CHAR_TABLE[i] = (CHAR_TABLE[i].toInt() or MASK_HEXDIGIT).toByte()
                }
                if (c >= '!' && c <= '/' || c >= ':' && c <= '@') {
                    CHAR_TABLE[i] = (CHAR_TABLE[i].toInt() or MASK_PUNCT).toByte()
                }
                if (CHAR_TABLE[i].toInt() and (MASK_LOWERCASE or MASK_UPPERCASE) != 0) {
                    CHAR_TABLE[i] = (CHAR_TABLE[i].toInt() or MASK_ALPHA).toByte()
                }
            }

            CHAR_TABLE[' '.toInt()] = MASK_SPACE.toByte()
            CHAR_TABLE['\r'.toInt()] = (CHAR_TABLE['\r'.toInt()].toInt() or MASK_SPACE).toByte()
            CHAR_TABLE['\n'.toInt()] = (CHAR_TABLE['\n'.toInt()].toInt() or MASK_SPACE).toByte()
            CHAR_TABLE['\t'.toInt()] = (CHAR_TABLE['\t'.toInt()].toInt() or MASK_SPACE).toByte()
            CHAR_TABLE[0x0C /* '\v' */] = (CHAR_TABLE[0x0C].toInt() or MASK_SPACE).toByte()
            CHAR_TABLE['\u000c'.toInt()] = (CHAR_TABLE['\u000c'.toInt()].toInt() or MASK_SPACE).toByte()
        }
    }
}
