/*******************************************************************************
 * Copyright (c) 2009 Luaj.org. All rights reserved.
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
package org.luaj.vm2.compiler

import org.luaj.vm2.LocVars
import org.luaj.vm2.Lua
import org.luaj.vm2.LuaError
import org.luaj.vm2.LuaInteger
import org.luaj.vm2.LuaString
import org.luaj.vm2.LuaValue
import org.luaj.vm2.Prototype
import org.luaj.vm2.compiler.FuncState.BlockCnt
import org.luaj.vm2.internal.*
import org.luaj.vm2.io.*
import org.luaj.vm2.lib.MathLib
import kotlin.js.*
import kotlin.math.*

@Suppress("MemberVisibilityCanBePrivate")
class LexState(internal var L: LuaC.CompileState, internal var z: LuaBinInput  /* input stream */) : Constants() {

    internal var current: Int = 0  /* current character (charint) */
    internal var linenumber: Int = 0  /* input line counter */
    internal var lastline: Int = 0  /* line of last token `consumed' */
    internal val t = Token()  /* current token */
    @JsName("lookahead_field")
    internal val lookahead = Token()  /* look ahead token */
    internal var fs: FuncState? = null  /* `FuncState' is private to the parser */
    internal var buff: CharArray? = null  /* buffer for tokens */
    internal var nbuff: Int = 0 /* length of buffer */
    internal var dyd = Dyndata()  /* dynamic structures used by the parser */
    internal lateinit var source: LuaString  /* current source name */
    internal lateinit var envn: LuaString  /* environment variable name */
    internal var decpoint: Byte = 0  /* locale decimal point */

    /* semantics information */
    class SemInfo {
        internal var r: LuaValue? = null
        internal var ts: LuaString? = null
    }

    class Token {
        internal var token: Int = 0
        internal val seminfo = SemInfo()
        fun set(other: Token) {
            this.token = other.token
            this.seminfo.r = other.seminfo.r
            this.seminfo.ts = other.seminfo.ts
        }
    }

    private fun isalnum(c: Int): Boolean {
        return (c >= '0'.toInt() && c <= '9'.toInt()
                || c >= 'a'.toInt() && c <= 'z'.toInt()
                || c >= 'A'.toInt() && c <= 'Z'.toInt()
                || c == '_'.toInt())
        // return Character.isLetterOrDigit(c);
    }

    private fun isalpha(c: Int): Boolean {
        return c >= 'a'.toInt() && c <= 'z'.toInt() || c >= 'A'.toInt() && c <= 'Z'.toInt()
    }

    private fun isdigit(c: Int): Boolean {
        return c >= '0'.toInt() && c <= '9'.toInt()
    }

    private fun isxdigit(c: Int): Boolean {
        return (c >= '0'.toInt() && c <= '9'.toInt()
                || c >= 'a'.toInt() && c <= 'f'.toInt()
                || c >= 'A'.toInt() && c <= 'F'.toInt())
    }

    private fun isspace(c: Int): Boolean {
        return c <= ' '.toInt()
    }


    init {
        this.buff = CharArray(32)
    }

    internal fun nextChar() {
        try {
            current = z.read().toInt()
        } catch (e: IOException) {
            e.printStackTrace()
            current = EOZ
        }

    }

    internal fun currIsNewline(): Boolean {
        return current == '\n'.toInt() || current == '\r'.toInt()
    }

    internal fun save_and_next() {
        save(current)
        nextChar()
    }

    internal fun save(c: Int) {
        if (buff == null || nbuff + 1 > buff!!.size)
            buff = Constants.realloc(buff, nbuff * 2 + 1)
        buff!![nbuff++] = c.toChar()
    }


    internal fun token2str(token: Int): String {
        return if (token < FIRST_RESERVED) {
            if (iscntrl(token))
                L.pushfstring("char($token)")
            else
                L.pushfstring(token.toChar().toString())
        } else {
            luaX_tokens[token - FIRST_RESERVED]
        }
    }

    internal fun txtToken(token: Int): String {
        when (token) {
            TK_NAME, TK_STRING, TK_NUMBER -> return buff!!.concatToString(0, nbuff)
            else -> return token2str(token)
        }
    }

    internal fun lexerror(msg: String, token: Int) {
        val cid = Lua.chunkid(source.tojstring())
        L.pushfstring("$cid:$linenumber: $msg")
        if (token != 0)
            L.pushfstring("syntax error: " + msg + " near " + txtToken(token))
        throw LuaError("$cid:$linenumber: $msg")
    }

    internal fun syntaxerror(msg: String) {
        lexerror(msg, t.token)
    }

    // only called by new_localvarliteral() for var names.
    internal fun newstring(s: String): LuaString {
        return L.newTString(s)
    }

    internal fun newstring(chars: CharArray?, offset: Int, len: Int): LuaString {
        return L.newTString(chars!!.concatToString(offset, offset + len))
    }

    internal fun inclinenumber() {
        val old = current
        Constants._assert(currIsNewline())
        nextChar() /* skip '\n' or '\r' */
        if (currIsNewline() && current != old)
            nextChar() /* skip '\n\r' or '\r\n' */
        if (++linenumber >= MAX_INT)
            syntaxerror("chunk has too many lines")
    }

    internal fun setinput(L: LuaC.CompileState, firstByte: Int, z: LuaBinInput, source: LuaString) {
        this.decpoint = '.'.toByte()
        this.L = L
        this.lookahead.token = TK_EOS /* no look-ahead token */
        this.z = z
        this.fs = null
        this.linenumber = 1
        this.lastline = 1
        this.source = source
        this.envn = LuaValue.ENV  /* environment variable name */
        this.nbuff = 0   /* initialize buffer */
        this.current = firstByte /* read first char */
        this.skipShebang()
    }

    private fun skipShebang() {
        if (current == '#'.toInt())
            while (!currIsNewline() && current != EOZ)
                nextChar()
    }


    /*
	** =======================================================
	** LEXICAL ANALYZER
	** =======================================================
	*/


    internal fun check_next(set: String): Boolean {
        if (set.indexOf(current.toChar()) < 0)
            return false
        save_and_next()
        return true
    }

    internal fun buffreplace(from: Char, to: Char) {
        var n = nbuff
        val p = buff
        while (--n >= 0)
            if (p!![n] == from)
                p[n] = to
    }

    @OptIn(ExperimentalStdlibApi::class)
    internal fun strx2number(str: String, seminfo: SemInfo): LuaValue {
        val c = str.toCharArray()
        var s = 0
        while (s < c.size && isspace(c[s].toInt()))
            ++s
        // Check for negative sign
        var sgn = 1.0
        if (s < c.size && c[s] == '-') {
            sgn = -1.0
            ++s
        }
        /* Check for "0x" */
        if (s + 2 >= c.size)
            return LuaValue.ZERO
        if (c[s++] != '0')
            return LuaValue.ZERO
        if (c[s] != 'x' && c[s] != 'X')
            return LuaValue.ZERO
        ++s

        // read integer part.
        var m = 0.0
        var e = 0
        while (s < c.size && isxdigit(c[s].toInt()))
            m = m * 16 + hexvalue(c[s++].toInt())
        if (s < c.size && c[s] == '.') {
            ++s  // skip dot
            while (s < c.size && isxdigit(c[s].toInt())) {
                m = m * 16 + hexvalue(c[s++].toInt())
                e -= 4  // Each fractional part shifts right by 2^4
            }
        }
        if (s < c.size && (c[s] == 'p' || c[s] == 'P')) {
            ++s
            var exp1 = 0
            var neg1 = false
            if (s < c.size && c[s] == '-') {
                neg1 = true
                ++s
            }
            while (s < c.size && isdigit(c[s].toInt()))
                exp1 = exp1 * 10 + c[s++].toInt() - '0'.toInt()
            if (neg1)
                exp1 = -exp1
            e += exp1
        }
        return LuaValue.valueOf(sgn * m * MathLib.dpow_d(2.0, e.toDouble()))
    }

    internal fun str2d(str: String, seminfo: SemInfo): Boolean {
        if (str.indexOf('n') >= 0 || str.indexOf('N') >= 0)
            seminfo.r = LuaValue.ZERO
        else if (str.indexOf('x') >= 0 || str.indexOf('X') >= 0)
            seminfo.r = strx2number(str, seminfo)
        else
            seminfo.r = LuaValue.valueOf((str.trim { it <= ' ' }).toDouble())
        return true
    }

    internal fun read_numeral(seminfo: SemInfo) {
        var expo = "Ee"
        val first = current
        Constants._assert(isdigit(current))
        save_and_next()
        if (first == '0'.toInt() && check_next("Xx"))
            expo = "Pp"
        while (true) {
            if (check_next(expo))
                check_next("+-")
            if (isxdigit(current) || current == '.'.toInt())
                save_and_next()
            else
                break
        }
        save('\u0000'.toInt())
        val str = buff!!.concatToString(0, nbuff)
        str2d(str, seminfo)
    }

    internal fun skip_sep(): Int {
        var count = 0
        val s = current
        Constants._assert(s == '['.toInt() || s == ']'.toInt())
        save_and_next()
        while (current == '='.toInt()) {
            save_and_next()
            count++
        }
        return if (current == s) count else -count - 1
    }

    internal fun read_long_string(seminfo: SemInfo?, sep: Int) {
        var cont = 0
        save_and_next() /* skip 2nd `[' */
        if (currIsNewline())
        /* string starts with a newline? */
            inclinenumber() /* skip it */
        var endloop = false
        loop@while (!endloop) {
            when (current) {
                EOZ -> lexerror(
                    if (seminfo != null)
                        "unfinished long string"
                    else
                        "unfinished long comment", TK_EOS
                )
                '['.toInt() -> {
                    if (skip_sep() == sep) {
                        save_and_next() /* skip 2nd `[' */
                        cont++
                        if (LUA_COMPAT_LSTR == 1) {
                            if (sep == 0)
                                lexerror("nesting of [[...]] is deprecated", '['.toInt())
                        }
                    }
                }
                ']'.toInt() -> {
                    if (skip_sep() == sep) {
                        save_and_next() /* skip 2nd `]' */
                        if (LUA_COMPAT_LSTR == 2) {
                            cont--
                            if (sep == 0 && cont >= 0)
                                break@loop
                        }
                        endloop = true
                    }
                }
                '\n'.toInt(), '\r'.toInt() -> {
                    save('\n'.toInt())
                    inclinenumber()
                    if (seminfo == null)
                        nbuff = 0 /* avoid wasting space */
                }
                else -> {
                    if (seminfo != null)
                        save_and_next()
                    else
                        nextChar()
                }
            }/* to avoid warnings */
        }
        if (seminfo != null)
            seminfo.ts = L.newTString(LuaString.valueOf(buff!!, 2 + sep, nbuff - 2 * (2 + sep)))
    }

    internal fun hexvalue(c: Int): Int {
        return if (c <= '9'.toInt()) c - '0'.toInt() else if (c <= 'F'.toInt()) c + 10 - 'A'.toInt() else c + 10 - 'a'.toInt()
    }

    internal fun readhexaesc(): Int {
        nextChar()
        val c1 = current
        nextChar()
        val c2 = current
        if (!isxdigit(c1) || !isxdigit(c2))
            lexerror("hexadecimal digit expected 'x" + c1.toChar() + c2.toChar(), TK_STRING)
        return (hexvalue(c1) shl 4) + hexvalue(c2)
    }

    internal fun read_string(del: Int, seminfo: SemInfo) {
        save_and_next()
        loop@while (current != del) {
            when (current) {
                EOZ -> {
                    lexerror("unfinished string", TK_EOS)
                    continue@loop /* to avoid warnings */
                }
                '\n'.toInt(), '\r'.toInt() -> {
                    lexerror("unfinished string", TK_STRING)
                    continue@loop /* to avoid warnings */
                }
                '\\'.toInt() -> {
                    var c: Int
                    nextChar() /* do not save the `\' */
                    when (current) {
                        'a'.toInt() /* bell */ -> c = '\u0007'.toInt()
                        'b'.toInt() /* backspace */ -> c = '\b'.toInt()
                        //'f'.toInt() /* form feed */ -> c = '\f'.toInt()
                        'f'.toInt() /* form feed */ -> c = '\u000c'.toInt()
                        'n'.toInt() /* newline */ -> c = '\n'.toInt()
                        'r'.toInt() /* carriage return */ -> c = '\r'.toInt()
                        't'.toInt() /* tab */ -> c = '\t'.toInt()
                        'v'.toInt() /* vertical tab */ -> c = '\u000B'.toInt()
                        'x'.toInt() -> c = readhexaesc()
                        '\n'.toInt() /* go through */, '\r'.toInt() -> {
                            save('\n'.toInt())
                            inclinenumber()
                            continue@loop
                        }
                        EOZ -> continue@loop /* will raise an error next loop */
                        'z'.toInt() -> {  /* zap following span of spaces */
                            nextChar()  /* skip the 'z' */
                            while (isspace(current)) {
                                if (currIsNewline())
                                    inclinenumber()
                                else
                                    nextChar()
                            }
                            continue@loop
                        }
                        else -> {
                            if (!isdigit(current))
                                save_and_next() /* handles \\, \", \', and \? */
                            else { /* \xxx */
                                var i = 0
                                c = 0
                                do {
                                    c = 10 * c + (current - '0'.toInt())
                                    nextChar()
                                } while (++i < 3 && isdigit(current))
                                if (c > UCHAR_MAX)
                                    lexerror("escape sequence too large", TK_STRING)
                                save(c)
                            }
                            continue@loop
                        }
                    }
                    save(c)
                    nextChar()
                    continue@loop
                }
                else -> save_and_next()
            }
        }
        save_and_next() /* skip delimiter */
        seminfo.ts = L.newTString(LuaString.valueOf(buff!!, 1, nbuff - 2))
    }

    internal fun llex(seminfo: SemInfo): Int {
        nbuff = 0
        loop@while (true) {
            when (current) {
                '\n'.toInt(), '\r'.toInt() -> {
                    inclinenumber()
                    continue@loop
                }
                '-'.toInt() -> {
                    nextChar()
                    if (current != '-'.toInt())
                        return '-'.toInt()
                    /* else is a comment */
                    nextChar()
                    if (current == '['.toInt()) {
                        val sep = skip_sep()
                        nbuff = 0 /* `skip_sep' may dirty the buffer */
                        if (sep >= 0) {
                            read_long_string(null, sep) /* long comment */
                            nbuff = 0
                            continue@loop
                        }
                    }
                    /* else short comment */
                    while (!currIsNewline() && current != EOZ)
                        nextChar()
                    continue@loop
                }
                '['.toInt() -> {
                    run {
                        val sep = skip_sep()
                        if (sep >= 0) {
                            read_long_string(seminfo, sep)
                            return TK_STRING
                        } else if (sep == -1)
                            return '['.toInt()
                        else
                            lexerror("invalid long string delimiter", TK_STRING)
                    }
                    run {
                        nextChar()
                        if (current != '='.toInt())
                            return '='.toInt()
                        else {
                            nextChar()
                            return TK_EQ
                        }
                    }
                }
                '='.toInt() -> {
                    nextChar()
                    if (current != '='.toInt())
                        return '='.toInt()
                    else {
                        nextChar()
                        return TK_EQ
                    }
                }
                '<'.toInt() -> {
                    nextChar()
                    if (current != '='.toInt())
                        return '<'.toInt()
                    else {
                        nextChar()
                        return TK_LE
                    }
                }
                '>'.toInt() -> {
                    nextChar()
                    if (current != '='.toInt())
                        return '>'.toInt()
                    else {
                        nextChar()
                        return TK_GE
                    }
                }
                '~'.toInt() -> {
                    nextChar()
                    if (current != '='.toInt())
                        return '~'.toInt()
                    else {
                        nextChar()
                        return TK_NE
                    }
                }
                ':'.toInt() -> {
                    nextChar()
                    if (current != ':'.toInt())
                        return ':'.toInt()
                    else {
                        nextChar()
                        return TK_DBCOLON
                    }
                }
                '"'.toInt(), '\''.toInt() -> {
                    read_string(current, seminfo)
                    return TK_STRING
                }
                '.'.toInt() -> {
                    save_and_next()
                    if (check_next(".")) {
                        return if (check_next("."))
                            TK_DOTS /* ... */
                        else
                            TK_CONCAT /* .. */
                    } else if (!isdigit(current))
                        return '.'.toInt()
                    else {
                        read_numeral(seminfo)
                        return TK_NUMBER
                    }
                }
                '0'.toInt(), '1'.toInt(), '2'.toInt(), '3'.toInt(), '4'.toInt(), '5'.toInt(), '6'.toInt(), '7'.toInt(), '8'.toInt(), '9'.toInt() -> {
                    read_numeral(seminfo)
                    return TK_NUMBER
                }
                EOZ -> {
                    return TK_EOS
                }
                else -> {
                    if (isspace(current)) {
                        Constants._assert(!currIsNewline())
                        nextChar()
                        continue@loop
                    } else if (isdigit(current)) {
                        read_numeral(seminfo)
                        return TK_NUMBER
                    } else if (isalpha(current) || current == '_'.toInt()) {
                        /* identifier or reserved word */
                        val ts: LuaString
                        do {
                            save_and_next()
                        } while (isalnum(current) || current == '_'.toInt())
                        ts = newstring(buff, 0, nbuff)
                        if (RESERVED.containsKey(ts))
                            return (RESERVED.get(ts) as Int).toInt()
                        else {
                            seminfo.ts = ts
                            return TK_NAME
                        }
                    } else {
                        val c = current
                        nextChar()
                        return c /* single-char tokens (+ - / ...) */
                    }
                }
            }
        }
    }

    internal operator fun next() {
        lastline = linenumber
        if (lookahead.token != TK_EOS) { /* is there a look-ahead token? */
            t.set(lookahead) /* use this one */
            lookahead.token = TK_EOS /* and discharge it */
        } else
            t.token = llex(t.seminfo) /* read next token */
    }

    internal fun lookahead() {
        Constants._assert(lookahead.token == TK_EOS)
        lookahead.token = llex(lookahead.seminfo)
    }

    class expdesc {
        internal var k: Int = 0 // expkind, from enumerated list, above
        internal val u = U()
        internal val t = IntPtr() /* patch list of `exit when true' */
        internal val f = IntPtr() /* patch list of `exit when false' */

        internal class U { // originally a union
            var ind_idx: Short = 0 // index (R/K)
            var ind_t: Short = 0 // table(register or upvalue)
            var ind_vt: Short = 0 // whether 't' is register (VLOCAL) or (UPVALUE)
            var _nval: LuaValue? = null
            var info: Int = 0
            fun setNval(r: LuaValue?) {
                _nval = r
            }

            fun nval(): LuaValue {
                return if (_nval == null) LuaInteger.valueOf(info) else _nval!!
            }
        }

        internal fun init(k: Int, i: Int) {
            this.f.i = NO_JUMP
            this.t.i = NO_JUMP
            this.k = k
            this.u.info = i
        }

        internal fun hasjumps(): Boolean {
            return t.i != f.i
        }

        internal fun isnumeral(): Boolean {
            return k == VKNUM && t.i == NO_JUMP && f.i == NO_JUMP
        }

        fun setvalue(other: expdesc) {
            this.f.i = other.f.i
            this.k = other.k
            this.t.i = other.t.i
            this.u._nval = other.u._nval
            this.u.ind_idx = other.u.ind_idx
            this.u.ind_t = other.u.ind_t
            this.u.ind_vt = other.u.ind_vt
            this.u.info = other.u.info
        }
    }


    /* description of active local variable */
    class Vardesc internal constructor(idx: Int) {
        internal val idx: Short  /* variable index in stack */

        init {
            this.idx = idx.toShort()
        }
    }


    /* description of pending goto statements and label statements */
    class Labeldesc(
        internal var name: LuaString?  /* label identifier */,
        internal var pc: Int  /* position in code */,
        internal var line: Int  /* line where it appeared */,
        internal var nactvar: Short  /* local level where it appears in current block */
    )


    /* dynamic structures used by the parser */
    internal class Dyndata {
        var actvar: Array<Vardesc>? = null  /* list of active local variables */
        var n_actvar = 0
        var gt: Array<Labeldesc?>? = null  /* list of pending gotos */
        var n_gt = 0
        var label: Array<Labeldesc>? = null   /* list of active labels */
        var n_label = 0
    }


    internal fun hasmultret(k: Int): Boolean {
        return k == VCALL || k == VVARARG
    }

    /*----------------------------------------------------------------------
	name		args	description
	------------------------------------------------------------------------*/

    internal fun anchor_token() {
        /* last token from outer function must be EOS */
        Constants._assert(fs != null || t.token == TK_EOS)
        if (t.token == TK_NAME || t.token == TK_STRING) {
            val ts = t.seminfo.ts
            // TODO: is this necessary?
            L.cachedLuaString(t.seminfo.ts!!)
        }
    }

    /* semantic error */
    internal fun semerror(msg: String) {
        t.token = 0  /* remove 'near to' from final message */
        syntaxerror(msg)
    }

    internal fun error_expected(token: Int) {
        syntaxerror(L.pushfstring(LUA_QS(token2str(token)) + " expected"))
    }

    internal fun testnext(c: Int): Boolean {
        if (t.token == c) {
            next()
            return true
        } else
            return false
    }

    internal fun check(c: Int) {
        if (t.token != c)
            error_expected(c)
    }

    internal fun checknext(c: Int) {
        check(c)
        next()
    }

    internal fun check_condition(c: Boolean, msg: String) {
        if (!c)
            syntaxerror(msg)
    }


    internal fun check_match(what: Int, who: Int, where: Int) {
        if (!testnext(what)) {
            if (where == linenumber)
                error_expected(what)
            else {
                syntaxerror(
                    L.pushfstring(
                        LUA_QS(token2str(what))
                                + " expected " + "(to close " + LUA_QS(token2str(who))
                                + " at line " + where + ")"
                    )
                )
            }
        }
    }

    internal fun str_checkname(): LuaString? {
        val ts: LuaString?
        check(TK_NAME)
        ts = t.seminfo.ts
        next()
        return ts
    }

    internal fun codestring(e: expdesc, s: LuaString?) {
        e.init(VK, fs!!.stringK(s!!))
    }

    internal fun checkname(e: expdesc) {
        codestring(e, str_checkname())
    }


    internal fun registerlocalvar(varname: LuaString?): Int {
        val fs = this.fs
        val f = fs!!.f
        if (f!!.locvars == null || fs.nlocvars + 1 > f.locvars.size)
            f.locvars = Constants.realloc(f.locvars, fs.nlocvars * 2 + 1)
        f.locvars[fs.nlocvars.toInt()] = LocVars(varname!!, 0, 0)
        return fs.nlocvars++.toInt()
    }

    internal fun new_localvar(name: LuaString?) {
        val reg = registerlocalvar(name)
        fs!!.checklimit(dyd.n_actvar + 1, Constants.LUAI_MAXVARS, "local variables")
        if (dyd.actvar == null || dyd.n_actvar + 1 > dyd.actvar!!.size)
            dyd.actvar = Constants.realloc(dyd.actvar, max(1, dyd.n_actvar * 2))
        dyd.actvar!![dyd.n_actvar++] = Vardesc(reg)
    }

    internal fun new_localvarliteral(v: String) {
        val ts = newstring(v)
        new_localvar(ts)
    }

    internal fun adjustlocalvars(nvars: Int) {
        var nvars = nvars
        val fs = this.fs
        fs!!.nactvar = (fs.nactvar + nvars).toShort()
        while (nvars > 0) {
            fs.getlocvar(fs.nactvar - nvars).startpc = fs.pc
            nvars--
        }
    }

    internal fun removevars(tolevel: Int) {
        val fs = this.fs
        while (fs!!.nactvar > tolevel)
            fs.getlocvar((--fs.nactvar).toInt()).endpc = fs.pc
    }

    internal fun singlevar(`var`: expdesc) {
        val varname = this.str_checkname()
        val fs = this.fs
        if (FuncState.singlevaraux(fs, varname!!, `var`, 1) == VVOID) { /* global name? */
            val key = expdesc()
            FuncState.singlevaraux(fs, this.envn, `var`, 1)  /* get environment variable */
            Constants._assert(`var`.k == VLOCAL || `var`.k == VUPVAL)
            this.codestring(key, varname)  /* key is variable name */
            fs!!.indexed(`var`, key)  /* env[varname] */
        }
    }

    internal fun adjust_assign(nvars: Int, nexps: Int, e: expdesc) {
        val fs = this.fs
        var extra = nvars - nexps
        if (hasmultret(e.k)) {
            /* includes call itself */
            extra++
            if (extra < 0)
                extra = 0
            /* last exp. provides the difference */
            fs!!.setreturns(e, extra)
            if (extra > 1)
                fs.reserveregs(extra - 1)
        } else {
            /* close last expression */
            if (e.k != VVOID)
                fs!!.exp2nextreg(e)
            if (extra > 0) {
                val reg = fs!!.freereg.toInt()
                fs.reserveregs(extra)
                fs.nil(reg, extra)
            }
        }
    }

    internal fun enterlevel() {
        if (++L.nCcalls > LUAI_MAXCCALLS)
            lexerror("chunk has too many syntax levels", 0)
    }

    internal fun leavelevel() {
        L.nCcalls--
    }

    internal fun closegoto(g: Int, label: Labeldesc) {
        val fs = this.fs
        val gl = this.dyd.gt
        val gt = gl!![g]!!
        Constants._assert(gt.name!!.eq_b(label.name!!))
        if (gt.nactvar < label.nactvar) {
            val vname = fs!!.getlocvar(gt.nactvar.toInt()).varname
            val msg = L.pushfstring(
                "<goto " + gt.name + "> at line "
                        + gt.line + " jumps into the scope of local '"
                        + vname.tojstring() + "'"
            )
            semerror(msg)
        }
        fs!!.patchlist(gt.pc, label.pc)
        /* remove goto from pending list */
        arraycopy(gl, g + 1, gl, g, this.dyd.n_gt - g - 1)
        gl[--this.dyd.n_gt] = null
    }

    /*
	 ** try to close a goto with existing labels; this solves backward jumps
	 */
    internal fun findlabel(g: Int): Boolean {
        var i: Int
        val bl = fs!!.bl
        val dyd = this.dyd
        val gt = dyd.gt!![g]!!
        /* check labels in current block for a match */
        i = bl!!.firstlabel.toInt()
        while (i < dyd.n_label) {
            val lb = dyd.label!![i]
            if (lb.name!!.eq_b(gt.name!!)) {  /* correct label? */
                if (gt.nactvar > lb.nactvar && (bl.upval || dyd.n_label > bl.firstlabel))
                    fs!!.patchclose(gt.pc, lb.nactvar.toInt())
                closegoto(g, lb)  /* close it */
                return true
            }
            i++
        }
        return false  /* label not found; cannot close goto */
    }

    /* Caller must grow() the vector before calling this. */
    internal fun newlabelentry(l: Array<Labeldesc?>, index: Int, name: LuaString?, line: Int, pc: Int): Int {
        l[index] = Labeldesc(name, pc, line, fs!!.nactvar)
        return index
    }

    /*
	 ** check whether new label 'lb' matches any pending gotos in current
	 ** block; solves forward jumps
	 */
    internal fun findgotos(lb: Labeldesc) {
        val gl = dyd.gt
        var i = fs!!.bl!!.firstgoto.toInt()
        while (i < dyd.n_gt) {
            if (gl!![i]!!.name!!.eq_b(lb.name!!))
                closegoto(i, lb)
            else
                i++
        }
    }


    /*
	** create a label named "break" to resolve break statements
	*/
    internal fun breaklabel() {
        val n = LuaString.valueOf("break")
        val l = newlabelentry(run {
            dyd.label = Constants.grow(dyd.label, dyd.n_label + 1)
            dyd.label!! as Array<LexState.Labeldesc?>
        }, dyd.n_label++, n, 0, fs!!.pc)
        findgotos(dyd.label!![l])
    }

    /*
	** generates an error for an undefined 'goto'; choose appropriate
	** message when label name is a reserved word (which can only be 'break')
	*/
    internal fun undefgoto(gt: Labeldesc) {
        val msg = L.pushfstring(
            if (isReservedKeyword(gt.name!!.tojstring()))
                "<" + gt.name + "> at line " + gt.line + " not inside a loop"
            else
                "no visible label '" + gt.name + "' for <goto> at line " + gt.line
        )
        semerror(msg)
    }

    internal fun addprototype(): Prototype {
        val clp: Prototype
        val f = fs!!.f  /* prototype of current function */
        if (f!!.p == null || fs!!.np >= f.p.size) {
            f.p = Constants.realloc(f.p, max(1, fs!!.np * 2))
        }
        clp = Prototype()
        f.p[fs!!.np++] = clp
        return clp
    }

    internal fun codeclosure(v: expdesc) {
        val fs = this.fs!!.prev
        v.init(VRELOCABLE, fs!!.codeABx(Lua.OP_CLOSURE, 0, fs.np - 1))
        fs.exp2nextreg(v)  /* fix it at stack top (for GC) */
    }

    internal fun open_func(fs: FuncState, bl: BlockCnt) {
        fs.prev = this.fs  /* linked list of funcstates */
        fs.ls = this
        this.fs = fs
        fs.pc = 0
        fs.lasttarget = -1
        fs.jpc = IntPtr(NO_JUMP)
        fs.freereg = 0
        fs.nk = 0
        fs.np = 0
        fs.nups = 0
        fs.nlocvars = 0
        fs.nactvar = 0
        fs.firstlocal = dyd.n_actvar
        fs.bl = null
        fs.f!!.source = this.source
        fs.f!!.maxstacksize = 2  /* registers 0/1 are always valid */
        fs.enterblock(bl, false)
    }

    internal fun close_func() {
        val fs = this.fs
        val f = fs!!.f
        fs.ret(0, 0) /* final return */
        fs.leaveblock()
        f!!.code = Constants.realloc(f.code, fs.pc)
        f.lineinfo = Constants.realloc(f.lineinfo, fs.pc)
        f.k = Constants.realloc(f.k, fs.nk)
        f.p = Constants.realloc(f.p, fs.np)
        f.locvars = Constants.realloc(f.locvars, fs.nlocvars.toInt())
        f.upvalues = Constants.realloc(f.upvalues, fs.nups.toInt())
        Constants._assert(fs.bl == null)
        this.fs = fs.prev
        // last token read was anchored in defunct function; must reanchor it
        // ls.anchor_token();
    }

    /*============================================================*/
    /* GRAMMAR RULES */
    /*============================================================*/

    internal fun fieldsel(v: expdesc) {
        /* fieldsel -> ['.' | ':'] NAME */
        val fs = this.fs
        val key = expdesc()
        fs!!.exp2anyregup(v)
        this.next() /* skip the dot or colon */
        this.checkname(key)
        fs.indexed(v, key)
    }

    internal fun yindex(v: expdesc) {
        /* index -> '[' expr ']' */
        this.next() /* skip the '[' */
        this.expr(v)
        this.fs!!.exp2val(v)
        this.checknext(']'.toInt())
    }


    /*
	** {======================================================================
	** Rules for Constructors
	** =======================================================================
	*/


    class ConsControl {
        internal var v = expdesc() /* last list item read */
        internal var t: expdesc? = null /* table descriptor */
        internal var nh: Int = 0 /* total number of `record' elements */
        internal var na: Int = 0 /* total number of array elements */
        internal var tostore: Int = 0 /* number of array elements pending to be stored */
    }


    internal fun recfield(cc: ConsControl) {
        /* recfield -> (NAME | `['exp1`]') = exp1 */
        val fs = this.fs
        val reg = this.fs!!.freereg.toInt()
        val key = expdesc()
        val `val` = expdesc()
        val rkkey: Int
        if (this.t.token == TK_NAME) {
            fs!!.checklimit(cc.nh, MAX_INT, "items in a constructor")
            this.checkname(key)
        } else
        /* this.t.token == '[' */
            this.yindex(key)
        cc.nh++
        this.checknext('='.toInt())
        rkkey = fs!!.exp2RK(key)
        this.expr(`val`)
        fs.codeABC(Lua.OP_SETTABLE, cc.t!!.u.info, rkkey, fs.exp2RK(`val`))
        fs.freereg = reg.toShort() /* free registers */
    }

    internal fun listfield(cc: ConsControl) {
        this.expr(cc.v)
        fs!!.checklimit(cc.na, MAX_INT, "items in a constructor")
        cc.na++
        cc.tostore++
    }


    internal fun constructor(t: expdesc) {
        /* constructor -> ?? */
        val fs = this.fs
        val line = this.linenumber
        val pc = fs!!.codeABC(Lua.OP_NEWTABLE, 0, 0, 0)
        val cc = ConsControl()
        cc.tostore = 0
        cc.nh = cc.tostore
        cc.na = cc.nh
        cc.t = t
        t.init(VRELOCABLE, pc)
        cc.v.init(VVOID, 0) /* no value (yet) */
        fs.exp2nextreg(t) /* fix it at stack top (for gc) */
        this.checknext('{'.toInt())
        do {
            Constants._assert(cc.v.k == VVOID || cc.tostore > 0)
            if (this.t.token == '}'.toInt())
                break
            fs.closelistfield(cc)
            when (this.t.token) {
                TK_NAME -> { /* may be listfields or recfields */
                    this.lookahead()
                    if (this.lookahead.token != '='.toInt())
                    /* expression? */
                        this.listfield(cc)
                    else
                        this.recfield(cc)
                }
                '['.toInt() -> { /* constructor_item -> recfield */
                    this.recfield(cc)
                }
                else -> { /* constructor_part -> listfield */
                    this.listfield(cc)
                }
            }
        } while (this.testnext(','.toInt()) || this.testnext(';'.toInt()))
        this.check_match('}'.toInt(), '{'.toInt(), line)
        fs.lastlistfield(cc)
        val i = InstructionPtr(fs.f!!.code, pc)
        Constants.SETARG_B(i, luaO_int2fb(cc.na)) /* set initial array size */
        Constants.SETARG_C(i, luaO_int2fb(cc.nh))  /* set initial table size */
    }


    /* }====================================================================== */

    internal fun parlist() {
        /* parlist -> [ param { `,' param } ] */
        val fs = this.fs
        val f = fs!!.f
        var nparams = 0
        f!!.is_vararg = 0
        if (this.t.token != ')'.toInt()) {  /* is `parlist' not empty? */
            do {
                when (this.t.token) {
                    TK_NAME -> {  /* param . NAME */
                        this.new_localvar(this.str_checkname())
                        ++nparams
                    }
                    TK_DOTS -> {  /* param . `...' */
                        this.next()
                        f.is_vararg = 1
                    }
                    else -> this.syntaxerror("<name> or " + LUA_QL("...") + " expected")
                }
            } while (f.is_vararg == 0 && this.testnext(','.toInt()))
        }
        this.adjustlocalvars(nparams)
        f.numparams = fs.nactvar.toInt()
        fs.reserveregs(fs.nactvar.toInt())  /* reserve register for parameters */
    }


    internal fun body(e: expdesc, needself: Boolean, line: Int) {
        /* body -> `(' parlist `)' chunk END */
        val new_fs = FuncState()
        val bl = BlockCnt()
        new_fs.f = addprototype()
        new_fs.f!!.linedefined = line
        open_func(new_fs, bl)
        this.checknext('('.toInt())
        if (needself) {
            new_localvarliteral("self")
            adjustlocalvars(1)
        }
        this.parlist()
        this.checknext(')'.toInt())
        this.statlist()
        new_fs.f!!.lastlinedefined = this.linenumber
        this.check_match(TK_END, TK_FUNCTION, line)
        this.codeclosure(e)
        this.close_func()
    }

    internal fun explist(v: expdesc): Int {
        /* explist1 -> expr { `,' expr } */
        var n = 1 /* at least one expression */
        this.expr(v)
        while (this.testnext(','.toInt())) {
            fs!!.exp2nextreg(v)
            this.expr(v)
            n++
        }
        return n
    }


    internal fun funcargs(f: expdesc, line: Int) {
        val fs = this.fs
        val args = expdesc()
        val base: Int
        val nparams: Int
        when (this.t.token) {
            '('.toInt() -> { /* funcargs -> `(' [ explist1 ] `)' */
                this.next()
                if (this.t.token == ')'.toInt())
                /* arg list is empty? */
                    args.k = VVOID
                else {
                    this.explist(args)
                    fs!!.setmultret(args)
                }
                this.check_match(')'.toInt(), '('.toInt(), line)
            }
            '{'.toInt() -> { /* funcargs -> constructor */
                this.constructor(args)
            }
            TK_STRING -> { /* funcargs -> STRING */
                this.codestring(args, this.t.seminfo.ts)
                this.next() /* must use `seminfo' before `next' */
            }
            else -> {
                this.syntaxerror("function arguments expected")
                return
            }
        }
        Constants._assert(f.k == VNONRELOC)
        base = f.u.info /* base register for call */
        if (hasmultret(args.k))
            nparams = Lua.LUA_MULTRET /* open call */
        else {
            if (args.k != VVOID)
                fs!!.exp2nextreg(args) /* close last argument */
            nparams = fs!!.freereg - (base + 1)
        }
        f.init(VCALL, fs!!.codeABC(Lua.OP_CALL, base, nparams + 1, 2))
        fs.fixline(line)
        fs.freereg = (base + 1).toShort()  /* call remove function and arguments and leaves
							 * (unless changed) one result */
    }


    /*
	** {======================================================================
	** Expression parsing
	** =======================================================================
	*/

    internal fun primaryexp(v: expdesc) {
        /* primaryexp -> NAME | '(' expr ')' */
        when (t.token) {
            '('.toInt() -> {
                val line = linenumber
                this.next()
                this.expr(v)
                this.check_match(')'.toInt(), '('.toInt(), line)
                fs!!.dischargevars(v)
                return
            }
            TK_NAME -> {
                singlevar(v)
                return
            }
            else -> {
                this.syntaxerror("unexpected symbol " + t.token + " (" + t.token.toChar() + ")")
                return
            }
        }
    }


    internal fun suffixedexp(v: expdesc) {
        /* suffixedexp ->
       	primaryexp { '.' NAME | '[' exp ']' | ':' NAME funcargs | funcargs } */
        val line = linenumber
        primaryexp(v)
        while (true) {
            when (t.token) {
                '.'.toInt() -> { /* fieldsel */
                    this.fieldsel(v)
                }
                '['.toInt() -> { /* `[' exp1 `]' */
                    val key = expdesc()
                    fs!!.exp2anyregup(v)
                    this.yindex(key)
                    fs!!.indexed(v, key)
                }
                ':'.toInt() -> { /* `:' NAME funcargs */
                    val key = expdesc()
                    this.next()
                    this.checkname(key)
                    fs!!.self(v, key)
                    this.funcargs(v, line)
                }
                '('.toInt(), TK_STRING, '{'.toInt() -> { /* funcargs */
                    fs!!.exp2nextreg(v)
                    this.funcargs(v, line)
                }
                else -> return
            }
        }
    }


    internal fun simpleexp(v: expdesc) {
        /*
		 * simpleexp -> NUMBER | STRING | NIL | true | false | ... | constructor |
		 * FUNCTION body | primaryexp
		 */
        when (this.t.token) {
            TK_NUMBER -> {
                v.init(VKNUM, 0)
                v.u.setNval(this.t.seminfo.r)
            }
            TK_STRING -> {
                this.codestring(v, this.t.seminfo.ts)
            }
            TK_NIL -> {
                v.init(VNIL, 0)
            }
            TK_TRUE -> {
                v.init(VTRUE, 0)
            }
            TK_FALSE -> {
                v.init(VFALSE, 0)
            }
            TK_DOTS -> { /* vararg */
                val fs = this.fs
                this.check_condition(
                    fs!!.f!!.is_vararg != 0, "cannot use " + LUA_QL("...")
                            + " outside a vararg function"
                )
                v.init(VVARARG, fs.codeABC(Lua.OP_VARARG, 0, 1, 0))
            }
            '{'.toInt() -> { /* constructor */
                this.constructor(v)
                return
            }
            TK_FUNCTION -> {
                this.next()
                this.body(v, false, this.linenumber)
                return
            }
            else -> {
                this.suffixedexp(v)
                return
            }
        }
        this.next()
    }


    internal fun getunopr(op: Int): Int {
        when (op) {
            TK_NOT -> return OPR_NOT
            '-'.toInt() -> return OPR_MINUS
            '#'.toInt() -> return OPR_LEN
            else -> return OPR_NOUNOPR
        }
    }


    internal fun getbinopr(op: Int): Int {
        when (op) {
            '+'.toInt() -> return OPR_ADD
            '-'.toInt() -> return OPR_SUB
            '*'.toInt() -> return OPR_MUL
            '/'.toInt() -> return OPR_DIV
            '%'.toInt() -> return OPR_MOD
            '^'.toInt() -> return OPR_POW
            TK_CONCAT -> return OPR_CONCAT
            TK_NE -> return OPR_NE
            TK_EQ -> return OPR_EQ
            '<'.toInt() -> return OPR_LT
            TK_LE -> return OPR_LE
            '>'.toInt() -> return OPR_GT
            TK_GE -> return OPR_GE
            TK_AND -> return OPR_AND
            TK_OR -> return OPR_OR
            else -> return OPR_NOBINOPR
        }
    }

    internal class Priority(i: Int, j: Int) {
        val left: Byte /* left priority for each binary operator */

        val right: Byte /* right priority */

        init {
            left = i.toByte()
            right = j.toByte()
        }
    }


    /*
	** subexpr -> (simpleexp | unop subexpr) { binop subexpr }
	** where `binop' is any binary operator with a priority higher than `limit'
	*/
    internal fun subexpr(v: expdesc, limit: Int): Int {
        var op: Int
        val uop: Int
        this.enterlevel()
        uop = getunopr(this.t.token)
        if (uop != OPR_NOUNOPR) {
            val line = linenumber
            this.next()
            this.subexpr(v, UNARY_PRIORITY)
            fs!!.prefix(uop, v, line)
        } else
            this.simpleexp(v)
        /* expand while operators have priorities higher than `limit' */
        op = getbinopr(this.t.token)
        while (op != OPR_NOBINOPR && priority[op].left > limit) {
            val v2 = expdesc()
            val line = linenumber
            this.next()
            fs!!.infix(op, v)
            /* read sub-expression with higher priority */
            val nextop = this.subexpr(v2, priority[op].right.toInt())
            fs!!.posfix(op, v, v2, line)
            op = nextop
        }
        this.leavelevel()
        return op /* return first untreated operator */
    }

    internal fun expr(v: expdesc) {
        this.subexpr(v, 0)
    }

    /* }==================================================================== */


    /*
	** {======================================================================
	** Rules for Statements
	** =======================================================================
	*/


    internal fun block_follow(withuntil: Boolean): Boolean {
        when (t.token) {
            TK_ELSE, TK_ELSEIF, TK_END, TK_EOS -> return true
            TK_UNTIL -> return withuntil
            else -> return false
        }
    }


    internal fun block() {
        /* block -> chunk */
        val fs = this.fs
        val bl = BlockCnt()
        fs!!.enterblock(bl, false)
        this.statlist()
        fs.leaveblock()
    }


    /*
	** structure to chain all variables in the left-hand side of an
	** assignment
	*/
    internal class LHS_assign {
        var prev: LHS_assign? = null
        /* variable (global, local, upvalue, or indexed) */
        var v = expdesc()
    }


    /*
	** check whether, in an assignment to a local variable, the local variable
	** is needed in a previous assignment (to a table). If so, save original
	** local value in a safe place and use this safe copy in the previous
	** assignment.
	*/
    internal fun check_conflict(lh: LHS_assign?, v: expdesc) {
        var lh = lh
        val fs = this.fs
        val extra = fs!!.freereg  /* eventual position to save local variable */
        var conflict = false
        while (lh != null) {
            if (lh.v.k == VINDEXED) {
                /* table is the upvalue/local being assigned now? */
                if (lh.v.u.ind_vt.toInt() == v.k && lh.v.u.ind_t.toInt() == v.u.info) {
                    conflict = true
                    lh.v.u.ind_vt = VLOCAL.toShort()
                    lh.v.u.ind_t = extra  /* previous assignment will use safe copy */
                }
                /* index is the local being assigned? (index cannot be upvalue) */
                if (v.k == VLOCAL && lh.v.u.ind_idx.toInt() == v.u.info) {
                    conflict = true
                    lh.v.u.ind_idx = extra  /* previous assignment will use safe copy */
                }
            }
            lh = lh.prev
        }
        if (conflict) {
            /* copy upvalue/local value to a temporary (in position 'extra') */
            val op = if (v.k == VLOCAL) Lua.OP_MOVE else Lua.OP_GETUPVAL
            fs.codeABC(op, extra.toInt(), v.u.info, 0)
            fs.reserveregs(1)
        }
    }


    internal fun assignment(lh: LHS_assign, nvars: Int) {
        val e = expdesc()
        this.check_condition(
            VLOCAL <= lh.v.k && lh.v.k <= VINDEXED,
            "syntax error"
        )
        if (this.testnext(','.toInt())) {  /* assignment -> `,' primaryexp assignment */
            val nv = LHS_assign()
            nv.prev = lh
            this.suffixedexp(nv.v)
            if (nv.v.k != VINDEXED)
                this.check_conflict(lh, nv.v)
            this.assignment(nv, nvars + 1)
        } else {  /* assignment . `=' explist1 */
            val nexps: Int
            this.checknext('='.toInt())
            nexps = this.explist(e)
            if (nexps != nvars) {
                this.adjust_assign(nvars, nexps, e)
                if (nexps > nvars)
                    this.fs!!.freereg = (this.fs!!.freereg - (nexps - nvars)).toShort()  /* remove extra values */
            } else {
                fs!!.setoneret(e)  /* close last expression */
                fs!!.storevar(lh.v, e)
                return   /* avoid default */
            }
        }
        e.init(VNONRELOC, this.fs!!.freereg - 1)  /* default assignment */
        fs!!.storevar(lh.v, e)
    }


    internal fun cond(): Int {
        /* cond -> exp */
        val v = expdesc()
        /* read condition */
        this.expr(v)
        /* `falses' are all equal here */
        if (v.k == VNIL)
            v.k = VFALSE
        fs!!.goiftrue(v)
        return v.f.i
    }

    internal fun gotostat(pc: Int) {
        val line = linenumber
        val label: LuaString?
        val g: Int
        if (testnext(TK_GOTO))
            label = str_checkname()
        else {
            next()  /* skip break */
            label = LuaString.valueOf("break")
        }
        g = newlabelentry(run {
            dyd.gt = Constants.grow(dyd.gt as Array<LexState.Labeldesc>?, dyd.n_gt + 1) as Array<LexState.Labeldesc?>
            dyd.gt!!
        }, dyd.n_gt++, label, line, pc)
        findlabel(g)  /* close it if label already defined */
    }


    /* skip no-op statements */
    internal fun skipnoopstat() {
        while (t.token == ';'.toInt() || t.token == TK_DBCOLON)
            statement()
    }


    internal fun labelstat(label: LuaString?, line: Int) {
        /* label -> '::' NAME '::' */
        val l: Int  /* index of new label being created */
        fs!!.checkrepeated(dyd.label, dyd.n_label, label!!)  /* check for repeated labels */
        checknext(TK_DBCOLON)  /* skip double colon */
        /* create new entry for this label */
        l = newlabelentry(run {
            dyd.label = Constants.grow(dyd.label, dyd.n_label + 1)
            dyd.label!! as Array<LexState.Labeldesc?>
        }, dyd.n_label++, label, line, fs!!.pc)
        skipnoopstat()  /* skip other no-op statements */
        if (block_follow(false)) {  /* label is last no-op statement in the block? */
            /* assume that locals are already out of scope */
            dyd.label!![l].nactvar = fs!!.bl!!.nactvar
        }
        findgotos(dyd.label!![l])
    }


    internal fun whilestat(line: Int) {
        /* whilestat -> WHILE cond DO block END */
        val fs = this.fs
        val whileinit: Int
        val condexit: Int
        val bl = BlockCnt()
        this.next()  /* skip WHILE */
        whileinit = fs!!.getlabel()
        condexit = this.cond()
        fs.enterblock(bl, true)
        this.checknext(TK_DO)
        this.block()
        fs.patchlist(fs.jump(), whileinit)
        this.check_match(TK_END, TK_WHILE, line)
        fs.leaveblock()
        fs.patchtohere(condexit)  /* false conditions finish the loop */
    }

    internal fun repeatstat(line: Int) {
        /* repeatstat -> REPEAT block UNTIL cond */
        val condexit: Int
        val fs = this.fs
        val repeat_init = fs!!.getlabel()
        val bl1 = BlockCnt()
        val bl2 = BlockCnt()
        fs.enterblock(bl1, true) /* loop block */
        fs.enterblock(bl2, false) /* scope block */
        this.next() /* skip REPEAT */
        this.statlist()
        this.check_match(TK_UNTIL, TK_REPEAT, line)
        condexit = this.cond() /* read condition (inside scope block) */
        if (bl2.upval) { /* upvalues? */
            fs.patchclose(condexit, bl2.nactvar.toInt())
        }
        fs.leaveblock() /* finish scope */
        fs.patchlist(condexit, repeat_init) /* close the loop */
        fs.leaveblock() /* finish loop */
    }


    internal fun exp1(): Int {
        val e = expdesc()
        val k: Int
        this.expr(e)
        k = e.k
        fs!!.exp2nextreg(e)
        return k
    }


    internal fun forbody(base: Int, line: Int, nvars: Int, isnum: Boolean) {
        /* forbody -> DO block */
        val bl = BlockCnt()
        val fs = this.fs
        val prep: Int
        val endfor: Int
        this.adjustlocalvars(3) /* control variables */
        this.checknext(TK_DO)
        prep = if (isnum) fs!!.codeAsBx(Lua.OP_FORPREP, base, NO_JUMP) else fs!!.jump()
        fs.enterblock(bl, false) /* scope for declared variables */
        this.adjustlocalvars(nvars)
        fs.reserveregs(nvars)
        this.block()
        fs.leaveblock() /* end of scope for declared variables */
        fs.patchtohere(prep)
        if (isnum)
        /* numeric for? */
            endfor = fs.codeAsBx(Lua.OP_FORLOOP, base, NO_JUMP)
        else {  /* generic for */
            fs.codeABC(Lua.OP_TFORCALL, base, 0, nvars)
            fs.fixline(line)
            endfor = fs.codeAsBx(Lua.OP_TFORLOOP, base + 2, NO_JUMP)
        }
        fs.patchlist(endfor, prep + 1)
        fs.fixline(line)
    }


    internal fun fornum(varname: LuaString?, line: Int) {
        /* fornum -> NAME = exp1,exp1[,exp1] forbody */
        val fs = this.fs
        val base = fs!!.freereg.toInt()
        this.new_localvarliteral(RESERVED_LOCAL_VAR_FOR_INDEX)
        this.new_localvarliteral(RESERVED_LOCAL_VAR_FOR_LIMIT)
        this.new_localvarliteral(RESERVED_LOCAL_VAR_FOR_STEP)
        this.new_localvar(varname)
        this.checknext('='.toInt())
        this.exp1() /* initial value */
        this.checknext(','.toInt())
        this.exp1() /* limit */
        if (this.testnext(','.toInt()))
            this.exp1() /* optional step */
        else { /* default step = 1 */
            fs.codeABx(Lua.OP_LOADK, fs.freereg.toInt(), fs.numberK(LuaInteger.valueOf(1)))
            fs.reserveregs(1)
        }
        this.forbody(base, line, 1, true)
    }


    internal fun forlist(indexname: LuaString?) {
        /* forlist -> NAME {,NAME} IN explist1 forbody */
        val fs = this.fs
        val e = expdesc()
        var nvars = 4   /* gen, state, control, plus at least one declared var */
        val line: Int
        val base = fs!!.freereg.toInt()
        /* create control variables */
        this.new_localvarliteral(RESERVED_LOCAL_VAR_FOR_GENERATOR)
        this.new_localvarliteral(RESERVED_LOCAL_VAR_FOR_STATE)
        this.new_localvarliteral(RESERVED_LOCAL_VAR_FOR_CONTROL)
        /* create declared variables */
        this.new_localvar(indexname)
        while (this.testnext(','.toInt())) {
            this.new_localvar(this.str_checkname())
            ++nvars
        }
        this.checknext(TK_IN)
        line = this.linenumber
        this.adjust_assign(3, this.explist(e), e)
        fs.checkstack(3) /* extra space to call generator */
        this.forbody(base, line, nvars - 3, false)
    }


    internal fun forstat(line: Int) {
        /* forstat -> FOR (fornum | forlist) END */
        val fs = this.fs
        val varname: LuaString?
        val bl = BlockCnt()
        fs!!.enterblock(bl, true) /* scope for loop and control variables */
        this.next() /* skip `for' */
        varname = this.str_checkname() /* first variable name */
        when (this.t.token) {
            '='.toInt() -> this.fornum(varname, line)
            ','.toInt(), TK_IN -> this.forlist(varname)
            else -> this.syntaxerror(LUA_QL("=") + " or " + LUA_QL("in") + " expected")
        }
        this.check_match(TK_END, TK_FOR, line)
        fs.leaveblock() /* loop scope (`break' jumps to this point) */
    }


    internal fun test_then_block(escapelist: IntPtr) {
        /* test_then_block -> [IF | ELSEIF] cond THEN block */
        val v = expdesc()
        val bl = BlockCnt()
        val jf: Int  /* instruction to skip 'then' code (if condition is false) */
        this.next() /* skip IF or ELSEIF */
        expr(v)  /* read expression */
        this.checknext(TK_THEN)
        if (t.token == TK_GOTO || t.token == TK_BREAK) {
            fs!!.goiffalse(v) /* will jump to label if condition is true */
            fs!!.enterblock(bl, false) /* must enter block before 'goto' */
            gotostat(v.t.i) /* handle goto/break */
            skipnoopstat() /* skip other no-op statements */
            if (block_follow(false)) { /* 'goto' is the entire block? */
                fs!!.leaveblock()
                return  /* and that is it */
            } else
            /* must skip over 'then' part if condition is false */
                jf = fs!!.jump()
        } else { /* regular case (not goto/break) */
            fs!!.goiftrue(v) /* skip over block if condition is false */
            fs!!.enterblock(bl, false)
            jf = v.f.i
        }
        statlist() /* `then' part */
        fs!!.leaveblock()
        if (t.token == TK_ELSE || t.token == TK_ELSEIF)
            fs!!.concat(escapelist, fs!!.jump()) /* must jump over it */
        fs!!.patchtohere(jf)
    }


    internal fun ifstat(line: Int) {
        val escapelist = IntPtr(NO_JUMP)  /* exit list for finished parts */
        test_then_block(escapelist)  /* IF cond THEN block */
        while (t.token == TK_ELSEIF)
            test_then_block(escapelist)  /* ELSEIF cond THEN block */
        if (testnext(TK_ELSE))
            block()  /* `else' part */
        check_match(TK_END, TK_IF, line)
        fs!!.patchtohere(escapelist.i)  /* patch escape list to 'if' end */
    }

    internal fun localfunc() {
        val b = expdesc()
        val fs = this.fs
        this.new_localvar(this.str_checkname())
        this.adjustlocalvars(1)
        this.body(b, false, this.linenumber)
        /* debug information will only see the variable after this point! */
        fs!!.getlocvar(fs.nactvar - 1).startpc = fs.pc
    }


    internal fun localstat() {
        /* stat -> LOCAL NAME {`,' NAME} [`=' explist1] */
        var nvars = 0
        val nexps: Int
        val e = expdesc()
        do {
            this.new_localvar(this.str_checkname())
            ++nvars
        } while (this.testnext(','.toInt()))
        if (this.testnext('='.toInt()))
            nexps = this.explist(e)
        else {
            e.k = VVOID
            nexps = 0
        }
        this.adjust_assign(nvars, nexps, e)
        this.adjustlocalvars(nvars)
    }


    internal fun funcname(v: expdesc): Boolean {
        /* funcname -> NAME {field} [`:' NAME] */
        var ismethod = false
        this.singlevar(v)
        while (this.t.token == '.'.toInt())
            this.fieldsel(v)
        if (this.t.token == ':'.toInt()) {
            ismethod = true
            this.fieldsel(v)
        }
        return ismethod
    }


    internal fun funcstat(line: Int) {
        /* funcstat -> FUNCTION funcname body */
        val needself: Boolean
        val v = expdesc()
        val b = expdesc()
        this.next() /* skip FUNCTION */
        needself = this.funcname(v)
        this.body(b, needself, line)
        fs!!.storevar(v, b)
        fs!!.fixline(line) /* definition `happens' in the first line */
    }


    internal fun exprstat() {
        /* stat -> func | assignment */
        val fs = this.fs
        val v = LHS_assign()
        this.suffixedexp(v.v)
        if (t.token == '='.toInt() || t.token == ','.toInt()) { /* stat -> assignment ? */
            v.prev = null
            assignment(v, 1)
        } else {  /* stat -> func */
            check_condition(v.v.k == VCALL, "syntax error")
            Constants.SETARG_C(fs!!.getcodePtr(v.v), 1)  /* call statement uses no results */
        }
    }

    internal fun retstat() {
        /* stat -> RETURN explist */
        val fs = this.fs
        val e = expdesc()
        val first: Int
        var nret: Int /* registers with returned values */
        if (block_follow(true) || this.t.token == ';'.toInt()) {
            nret = 0
            first = nret /* return no values */
        } else {
            nret = this.explist(e) /* optional return values */
            if (hasmultret(e.k)) {
                fs!!.setmultret(e)
                if (e.k == VCALL && nret == 1) { /* tail call? */
                    Constants.SET_OPCODE(fs.getcodePtr(e), Lua.OP_TAILCALL)
                    Constants._assert(Lua.GETARG_A(fs.getcode(e)) == fs.nactvar.toInt())
                }
                first = fs.nactvar.toInt()
                nret = Lua.LUA_MULTRET /* return all values */
            } else {
                if (nret == 1)
                /* only one single value? */
                    first = fs!!.exp2anyreg(e)
                else {
                    fs!!.exp2nextreg(e) /* values must go to the `stack' */
                    first = fs.nactvar.toInt() /* return all `active' values */
                    Constants._assert(nret == fs.freereg - first)
                }
            }
        }
        fs!!.ret(first, nret)
        testnext(';'.toInt())  /* skip optional semicolon */
    }

    internal fun statement() {
        val line = this.linenumber /* may be needed for error messages */
        enterlevel()
        when (this.t.token) {
            ';'.toInt() -> { /* stat -> ';' (empty statement) */
                next() /* skip ';' */
            }
            TK_IF -> { /* stat -> ifstat */
                this.ifstat(line)
            }
            TK_WHILE -> { /* stat -> whilestat */
                this.whilestat(line)
            }
            TK_DO -> { /* stat -> DO block END */
                this.next() /* skip DO */
                this.block()
                this.check_match(TK_END, TK_DO, line)
            }
            TK_FOR -> { /* stat -> forstat */
                this.forstat(line)
            }
            TK_REPEAT -> { /* stat -> repeatstat */
                this.repeatstat(line)
            }
            TK_FUNCTION -> {
                this.funcstat(line) /* stat -> funcstat */
            }
            TK_LOCAL -> { /* stat -> localstat */
                this.next() /* skip LOCAL */
                if (this.testnext(TK_FUNCTION))
                /* local function? */
                    this.localfunc()
                else
                    this.localstat()
            }
            TK_DBCOLON -> { /* stat -> label */
                next() /* skip double colon */
                labelstat(str_checkname(), line)
            }
            TK_RETURN -> { /* stat -> retstat */
                next()  /* skip RETURN */
                this.retstat()
            }
            TK_BREAK, TK_GOTO -> { /* stat -> breakstat */
                this.gotostat(fs!!.jump())
            }
            else -> {
                this.exprstat()
            }
        }
        Constants._assert(fs!!.f!!.maxstacksize >= fs!!.freereg && fs!!.freereg >= fs!!.nactvar)
        fs!!.freereg = fs!!.nactvar /* free registers */
        leavelevel()
    }

    internal fun statlist() {
        /* statlist -> { stat [`;'] } */
        while (!block_follow(true)) {
            if (t.token == TK_RETURN) {
                statement()
                return  /* 'return' must be last statement */
            }
            statement()
        }
    }

    /*
	** compiles the main function, which is a regular vararg function with an
	** upvalue named LUA_ENV
	*/
    fun mainfunc(funcstate: FuncState) {
        val bl = BlockCnt()
        open_func(funcstate, bl)
        fs!!.f!!.is_vararg = 1  /* main function is always vararg */
        val v = expdesc()
        v.init(VLOCAL, 0)  /* create and... */
        fs!!.newupvalue(envn, v)  /* ...set environment upvalue */
        next()  /* read first token */
        statlist()  /* parse main body */
        check(TK_EOS)
        close_func()
    }

    companion object {

        protected val RESERVED_LOCAL_VAR_FOR_CONTROL = "(for control)"
        protected val RESERVED_LOCAL_VAR_FOR_STATE = "(for state)"
        protected val RESERVED_LOCAL_VAR_FOR_GENERATOR = "(for generator)"
        protected val RESERVED_LOCAL_VAR_FOR_STEP = "(for step)"
        protected val RESERVED_LOCAL_VAR_FOR_LIMIT = "(for limit)"
        protected val RESERVED_LOCAL_VAR_FOR_INDEX = "(for index)"

        // keywords array
        protected val RESERVED_LOCAL_VAR_KEYWORDS = arrayOf(
            RESERVED_LOCAL_VAR_FOR_CONTROL,
            RESERVED_LOCAL_VAR_FOR_GENERATOR,
            RESERVED_LOCAL_VAR_FOR_INDEX,
            RESERVED_LOCAL_VAR_FOR_LIMIT,
            RESERVED_LOCAL_VAR_FOR_STATE,
            RESERVED_LOCAL_VAR_FOR_STEP
        )
        private val RESERVED_LOCAL_VAR_KEYWORDS_TABLE = HashMap<String, Boolean>()

        init {
            for (i in RESERVED_LOCAL_VAR_KEYWORDS.indices)
                RESERVED_LOCAL_VAR_KEYWORDS_TABLE[RESERVED_LOCAL_VAR_KEYWORDS[i]] = true
        }

        private val EOZ = -1
        private val MAX_INT = Int.MAX_VALUE - 2
        private val UCHAR_MAX = 255 // TODO, convert to unicode CHAR_MAX?
        private val LUAI_MAXCCALLS = 200

        private fun LUA_QS(s: String): String {
            return "'$s'"
        }

        private fun LUA_QL(o: Any): String {
            return LUA_QS(o.toString())
        }

        private val LUA_COMPAT_LSTR = 1 // 1 for compatibility, 2 for old behavior
        private val LUA_COMPAT_VARARG = true

        fun isReservedKeyword(varName: String): Boolean {
            return RESERVED_LOCAL_VAR_KEYWORDS_TABLE.containsKey(varName)
        }

        /*
	** Marks the end of a patch list. It is an invalid value both as an absolute
	** address, and as a list link (would link an element to itself).
	*/
        internal val NO_JUMP = -1

        /*
	** grep "ORDER OPR" if you change these enums
	*/
        internal val OPR_ADD = 0
        internal val OPR_SUB = 1
        internal val OPR_MUL = 2
        internal val OPR_DIV = 3
        internal val OPR_MOD = 4
        internal val OPR_POW = 5
        internal val OPR_CONCAT = 6
        internal val OPR_NE = 7
        internal val OPR_EQ = 8
        internal val OPR_LT = 9
        internal val OPR_LE = 10
        internal val OPR_GT = 11
        internal val OPR_GE = 12
        internal val OPR_AND = 13
        internal val OPR_OR = 14
        internal val OPR_NOBINOPR = 15

        internal val OPR_MINUS = 0
        internal val OPR_NOT = 1
        internal val OPR_LEN = 2
        internal val OPR_NOUNOPR = 3

        /* exp kind */
        internal val VVOID = 0
        /* no value */
        internal val VNIL = 1
        internal val VTRUE = 2
        internal val VFALSE = 3
        internal val VK = 4
        /* info = index of constant in `k' */
        internal val VKNUM = 5
        /* nval = numerical value */
        internal val VNONRELOC = 6
        /* info = result register */
        internal val VLOCAL = 7
        /* info = local register */
        internal val VUPVAL = 8
        /* info = index of upvalue in `upvalues' */
        internal val VINDEXED = 9
        /* info = table register, aux = index register (or `k') */
        internal val VJMP = 10
        /* info = instruction pc */
        internal val VRELOCABLE = 11
        /* info = instruction pc */
        internal val VCALL = 12
        /* info = instruction pc */
        internal val VVARARG = 13    /* info = instruction pc */

        /* ORDER RESERVED */
        internal val luaX_tokens = arrayOf(
            "and",
            "break",
            "do",
            "else",
            "elseif",
            "end",
            "false",
            "for",
            "function",
            "goto",
            "if",
            "in",
            "local",
            "nil",
            "not",
            "or",
            "repeat",
            "return",
            "then",
            "true",
            "until",
            "while",
            "..",
            "...",
            "==",
            ">=",
            "<=",
            "~=",
            "::",
            "<eos>",
            "<number>",
            "<name>",
            "<string>",
            "<eof>"
        )

        internal val
                /* terminal symbols denoted by reserved words */
                TK_AND = 257
        internal val TK_BREAK = 258
        internal val TK_DO = 259
        internal val TK_ELSE = 260
        internal val TK_ELSEIF = 261
        internal val TK_END = 262
        internal val TK_FALSE = 263
        internal val TK_FOR = 264
        internal val TK_FUNCTION = 265
        internal val TK_GOTO = 266
        internal val TK_IF = 267
        internal val TK_IN = 268
        internal val TK_LOCAL = 269
        internal val TK_NIL = 270
        internal val TK_NOT = 271
        internal val TK_OR = 272
        internal val TK_REPEAT = 273
        internal val TK_RETURN = 274
        internal val TK_THEN = 275
        internal val TK_TRUE = 276
        internal val TK_UNTIL = 277
        internal val TK_WHILE = 278
        /* other terminal symbols */
        internal val TK_CONCAT = 279
        internal val TK_DOTS = 280
        internal val TK_EQ = 281
        internal val TK_GE = 282
        internal val TK_LE = 283
        internal val TK_NE = 284
        internal val TK_DBCOLON = 285
        internal val TK_EOS = 286
        internal val TK_NUMBER = 287
        internal val TK_NAME = 288
        internal val TK_STRING = 289

        internal val FIRST_RESERVED = TK_AND
        internal val NUM_RESERVED = TK_WHILE + 1 - FIRST_RESERVED

        internal val RESERVED = HashMap<LuaString, Int>()

        init {
            for (i in 0 until NUM_RESERVED) {
                val ts = LuaValue.valueOf(luaX_tokens[i])
                RESERVED[ts] = FIRST_RESERVED + i
            }
        }

        private fun iscntrl(token: Int): Boolean {
            return token < ' '.toInt()
        }

        // =============================================================
        // from lcode.h
        // =============================================================


        // =============================================================
        // from lparser.c
        // =============================================================

        internal fun vkisvar(k: Int): Boolean {
            return VLOCAL <= k && k <= VINDEXED
        }

        internal fun vkisinreg(k: Int): Boolean {
            return k == VNONRELOC || k == VLOCAL
        }

        /*
	** converts an integer to a "floating point byte", represented as
	** (eeeeexxx), where the real value is (1xxx) * 2^(eeeee - 1) if
	** eeeee != 0 and (xxx) otherwise.
	*/
        internal fun luaO_int2fb(x: Int): Int {
            var x = x
            var e = 0  /* expoent */
            while (x >= 16) {
                x = x + 1 shr 1
                e++
            }
            return if (x < 8)
                x
            else
                e + 1 shl 3 or x - 8
        }

        internal var priority = arrayOf(/* ORDER OPR */
            Priority(6, 6), Priority(6, 6), Priority(7, 7), Priority(7, 7), Priority(7, 7), /* `+' `-' `/' `%' */
            Priority(10, 9), Priority(5, 4), /* power and concat (right associative) */
            Priority(3, 3), Priority(3, 3), /* equality and inequality */
            Priority(3, 3), Priority(3, 3), Priority(3, 3), Priority(3, 3), /* order */
            Priority(2, 2), Priority(1, 1)                   /* logical (and/or) */
        )

        internal val UNARY_PRIORITY = 8  /* priority for unary operators */
    }

    /* }====================================================================== */

}
