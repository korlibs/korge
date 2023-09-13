package korlibs.template.internal

import korlibs.template.util.ListReader

internal object Yaml {
    fun decode(str: String) = read(ListReader(tokenize(str)), level = 0)
    fun read(str: String) = read(ListReader(tokenize(str)), level = 0)

    private fun parseStr(toks: List<Token>): Any? {
        if (toks.size == 1 && toks[0] is Token.STR) return toks[0].ustr
        return parseStr(toks.joinToString("") { it.ustr })
    }

    private fun parseStr(str: String) = when (str) {
        "null" -> null
        "true" -> true
        "false" -> false
        else -> str.toIntOrNull() ?: str.toDoubleOrNull() ?: str
    }

    //const val TRACE = true
    const val TRACE = false
    private val EMPTY_SET = setOf<String>()
    private val SET_COMMA_END_ARRAY = setOf(",", "]")

    private fun read(s: ListReader<Token>, level: Int): Any? = s.run {
        var list: ArrayList<Any?>? = null
        var map: MutableMap<String, Any?>? = null
        var lastMapKey: String? = null
        var lastMapValue: Any? = null

        val levelStr = if (TRACE) "  ".repeat(level) else ""

        linehandle@ while (s.hasMore) {
            val token = s.peek()
            val line = token as? Token.LINE
            val lineLevel = line?.level
            if (TRACE && line != null) println("${levelStr}LINE($lineLevel)")
            if (lineLevel != null && lineLevel > level) {
                // child level
                val res = read(s, lineLevel)
                if (list != null) {
                    if (TRACE) println("${levelStr}CHILD.list.add: $res")
                    list.add(res)
                } else {
                    if (TRACE) println("${levelStr}CHILD.return: $res")
                    return res
                }
            } else if (lineLevel != null && lineLevel < level) {
                // parent level
                if (TRACE) println("${levelStr}PARENT: level < line.level")
                break
            } else {
                // current level
                if (line != null) s.read()
                if (s.eof) break
                val item = s.peek()
                when (item.str) {
                    "-" -> {
                        if (s.read().str != "-") invalidOp
                        if (list == null) {
                            list = arrayListOf()
                            if (map != null && lastMapKey != null && lastMapValue == null) {
                                map[lastMapKey] = list
                            }
                        }
                        if (TRACE) println("${levelStr}LIST_ITEM...")
                        val res = read(s, level + 1)
                        if (TRACE) println("${levelStr}LIST_ITEM: $res")
                        list.add(res)
                    }
                    "[" -> {
                        if (s.read().str != "[") invalidOp
                        val olist = arrayListOf<Any?>()
                        array@ while (s.peek().str != "]") {
                            olist += readOrString(s, level, SET_COMMA_END_ARRAY, supportNonSpaceSymbols = false)
                            val p = s.peek().str
                            when (p) {
                                "," -> { s.read(); continue@array }
                                "]" -> break@array
                                else -> invalidOp("Unexpected '$p'")
                            }
                        }
                        if (s.read().str != "]") invalidOp
                        return olist
                    }
                    else -> {
                        val keyIds = s.readId()
                        val sp = s.peekOrNull() ?: Token.EOF
                        if (s.eof || (sp.str != ":" || (sp is Token.SYMBOL && !sp.isNextWhite))) {
                            val key = parseStr(keyIds)
                            if (TRACE) println("${levelStr}LIT: $key")
                            return key
                        } else {
                            val key = parseStr(keyIds).toString()
                            if (map == null) map = LinkedHashMap()
                            if (s.read().str != ":") invalidOp
                            if (TRACE) println("${levelStr}MAP[$key]...")
                            val next = s.peekOrNull()
                            val nextStr = next?.str
                            val hasSpaces = next is Token.SYMBOL && next.isNextWhite
                            val nextIsSpecialSymbol = nextStr == "[" || nextStr == "{" || (nextStr == "-" && hasSpaces)
                            val value = readOrString(s, level, EMPTY_SET, supportNonSpaceSymbols = !nextIsSpecialSymbol)
                            lastMapKey = key
                            lastMapValue = value
                            map[key] = value
                            list = null
                            if (TRACE) println("${levelStr}MAP[$key]: $value")
                        }
                    }
                }
            }
        }

        if (TRACE) println("${levelStr}RETURN: list=$list, map=$map")

        return map ?: list
    }

    fun ListReader<Token>.readId(): List<Token> {
        val tokens = arrayListOf<Token>()
        while (hasMore) {
            val token = peek()
            if (token is Token.ID || token is Token.STR || ((token is Token.SYMBOL) && token.str == "-") || ((token is Token.SYMBOL) && token.str == ":" && !token.isNextWhite)) {
                tokens.add(token)
                read()
            } else {
                break
            }
        }
        return tokens
    }

    fun readOrString(s: ListReader<Token>, level: Int, delimiters: Set<String>, supportNonSpaceSymbols: Boolean): Any? {
        val sp = s.peek()
        return if (sp is Token.ID || (supportNonSpaceSymbols && sp is Token.SYMBOL && !sp.isNextWhite)) {
            var str = ""
            str@while (s.hasMore) {
                val p = s.peek()
                if (p is Token.LINE) break@str
                if (p.str in delimiters) break@str
                str += s.read().str
            }
            parseStr(str)
        } else {
            read(s, level + 1)
        }
    }

    fun tokenize(str: String): List<Token> = StrReader(str.replace("\r\n", "\n")).tokenize()

    private fun StrReader.tokenize(): List<Token> {
        val out = arrayListOf<Token>()

        val s = this
        var str = ""
        fun flush() {
            if (str.isNotBlank() && str.isNotEmpty()) {
                out += Token.ID(str.trim()); str = ""
            }
        }

        val indents = ArrayList<Int>()
        linestart@ while (hasMore) {
            // Line start
            flush()
            val indentStr = readWhile(Char::isWhitespace).replace("\t", "     ")
            if (indentStr.contains('\n')) continue@linestart  // ignore empty lines with possible additional indent
            val indent = indentStr.length
            if (indents.isEmpty() || indent > indents.last()) {
                indents += indent
            } else {
                while (indents.isNotEmpty() && indent < indents.last()) indents.removeAt(indents.size - 1)
                if (indents.isEmpty()) invalidOp
            }
            val indentLevel = indents.size - 1
            while (out.isNotEmpty() && out.last() is Token.LINE) out.removeAt(out.size - 1)
            out += Token.LINE(indentStr, indentLevel)
            while (hasMore) {
                val c = read()
                when (c) {
                    ':', '-', '[', ']', ',' -> {
                        flush(); out += Token.SYMBOL("$c", peek())
                    }
                    '#' -> {
                        flush(); readUntilLineEnd(); skip(); continue@linestart
                    }
                    '\n' -> {
                        flush(); continue@linestart
                    }
                    '"', '\'' -> {
                        flush()
                        s.unread()
                        out += Token.STR(s.readStringLit())
                    }
                    else -> str += c
                }
            }
        }
        flush()
        return out
    }

    interface Token {
        val str: String
        val ustr get() = str

        object EOF : Token {
            override val str: String = ""
        }

        data class LINE(override val str: String, val level: Int) : Token {
            override fun toString(): String = "LINE($level)"
        }

        data class ID(override val str: String) : Token
        data class STR(override val str: String) : Token {
            override val ustr = str.unquote()
        }

        data class SYMBOL(override val str: String, val next: Char) : Token {
            val isNextWhite: Boolean get() = next == ' ' || next == '\t' || next == '\n' || next == '\r'
        }
    }

    private fun StrReader.readUntilLineEnd() = this.readUntil { it == '\n' }
}
