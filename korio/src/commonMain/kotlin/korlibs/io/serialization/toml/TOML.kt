@file:Suppress("PackageDirectoryMismatch")

package korlibs.io.serialization.toml

//import org.intellij.lang.annotations.Language
import kotlin.math.min

// @TODO: Dates
// @TODO: Special cases for ' & ''' & """
// @TODO: Check for bugs and for invalid scenarios that shouldn't be accepted
object TOML {
    fun parseToml(
        //@Language("toml")
        str: String, out: MutableMap<String, Any?> = LinkedHashMap()): Map<String, Any?> {
        return StrReader(str).parseToml(out)
    }

    private fun Char.isLetterOrDigitOrUndescore(): Boolean = this.isLetterOrDigit() || this == '_'

    fun Char.isValidLiteralChar(): Boolean {
        val it = this
        return it.isLetterOrDigitOrUndescore() || it == '+' || it == '-' || it == ':' || it == '.'
    }

    private fun StrReader.parseToml(out: MutableMap<String, Any?>): Map<String, Any?> {
        var current: MutableMap<String, Any?> = out
        while (hasMore) {
            val c = peek()
            //println("PEEK: '$c'")
            when (c) {
                ' ', '\t', '\n', '\r' -> {
                    skip()
                    continue
                }
                '#' -> {
                    skipUntil { it == '\n' }
                }
                '[' -> {
                    expect('[')
                    val append = expectOpt('[')
                    val keys = parseKeys()
                    if (append) expect(']')
                    expect(']')
                    current = out
                    for ((index, key) in keys.withIndex()) {
                        val isLast = index == keys.size -1
                        if (isLast && append) {
                            val array = current.getOrPut(key) { arrayListOf<MutableMap<String, Any?>>() } as MutableList<MutableMap<String, Any?>>
                            val obj = LinkedHashMap<String, Any?>()
                            array.add(obj)
                            current = obj
                        } else {
                            current = current.getOrPut(key) { LinkedHashMap<String, Any?>() } as MutableMap<String, Any?>
                        }
                    }
                    //println("SECTION: $keys")
                }
                else -> {
                    skipSpacesOrNLs()
                    //println(" ---> reader=$this")
                    val keys = parseKeys()
                    skipSpacesOrNLs()
                    expect('=')
                    skipSpacesOrNLs()
                    val value = parseLiteral()
                    //println("KEYS[a]: $keys, value=$value, reader=$this")
                    skipSpacesOrNLs()
                    //println("KEYS[b]: $keys, value=$value, reader=$this")
                    var ccurrent = current
                    for ((index, key) in keys.withIndex()) {
                        val isLast = index == keys.size -1
                        if (isLast) {
                            ccurrent[key] = value
                        } else {
                            ccurrent = ccurrent.getOrPut(key) { LinkedHashMap<String, Any?>() } as MutableMap<String, Any?>
                        }
                    }
                    //println("ITEM: $keys=[${value!!::class}]$value")
                }
            }
        }
        //println(out)
        return out
    }
    private fun StrReader.parseLiteral(): Any? {
        skipSpacesOrNLs()
        return when (val c = peek()) {
            '{' -> {
                val items = LinkedHashMap<String, Any?>()
                expect('{')
                while (hasMore) {
                    skipSpacesOrNLs()
                    if (expectOpt('}')) break
                    val keys = parseKeys()
                    skipSpacesOrNLs()
                    expect('=')
                    skipSpacesOrNLs()
                    val item = parseLiteral()
                    var citems = items
                    for ((index, key) in keys.withIndex()) {
                        val isLast = index == keys.size - 1
                        if (isLast) {
                            citems[key] = item
                        } else {
                            citems = citems.getOrPut(key) { LinkedHashMap<String, Any?>() } as LinkedHashMap<String, Any?>
                        }
                    }
                    if (expectOpt(',')) continue
                }
                return items
            }
            '[' -> {
                val items = arrayListOf<Any?>()
                expect('[')
                while (hasMore) {
                    skipSpacesOrNLs()
                    if (expectOpt(']')) break
                    items += parseLiteral()
                    skipSpacesOrNLs()
                    if (expectOpt(',')) continue
                }
                return items
            }
            '"', '\'' -> parseStringLiteral()
            else -> {
                if (!c.isValidLiteralChar()) error("Expected literal but found = '$c'")
                val value = readWhile { it.isValidLiteralChar() }
                return when (value) {
                    "null" -> null
                    "true" -> true
                    "false" -> false
                    else -> value.toIntOrNull() ?: value.toDoubleOrNull() ?: value
                }.also {
                    //println("VALUE[${it!!::class}]: $it")
                }
            }
        }
    }

    private fun StrReader.parseKeys(): List<String> {
        val keys = arrayListOf<String>()
        do {
            skipSpacesOrNLs()
            keys += parseKey()
            skipSpacesOrNLs()
        } while (expectOpt('.'))
        return keys
    }

    private fun StrReader.parseKey(): String {
        skipSpacesOrNLs()
        val c = peek()
        return if (c == '"' || c == '\'') {
            parseStringLiteral()
        } else if (c.isLetterOrDigitOrUndescore()) {
            val key = readWhile { it.isLetterOrDigitOrUndescore() }
            //println("KEY: '$key'")
            key
        } else {
            return ""
        }
    }

    private fun StrReader.parseStringLiteral(): String {
        val sb = StringBuilder()
        val parseStart = peek(0)
        if (parseStart != '\'' && parseStart != '"') error("Invalid string $parseStart")

        val triplet = (peek(1) == parseStart && peek(2) == parseStart)
        if (triplet) skip(3) else skip(1)

        while (hasMore) {
            val c = peek(0)
            if (triplet && peek(0) == parseStart && peek(1) == parseStart && peek(2) == parseStart) {
                skip(3)
                break
            }
            if (c == parseStart) {
                skip(1)
                break
            }
            if (c == '\\') {
                skip()
                sb.append(when (val cc = read()) {
                    'b' -> '\u0008'
                    't' -> '\u0009'
                    'n' -> '\u000a'
                    'f' -> '\u000c'
                    'r' -> '\u000d'
                    //'"' -> '"'
                    //'\\' -> '\\'
                    'u' -> (readString { skip(4) }.toIntOrNull(16) ?: 0).toChar()
                    'U' -> (readString { skip(8) }.toIntOrNull(16) ?: 0).toChar()
                    else -> cc
                })
            } else {
                skip()
                sb.append(c)
            }
        }
        var str = sb.toString()
        if (triplet && str.isNotEmpty() && str[0] == '\n') str = str.substring(1)
        return str
    }

    private class StrReader(val str: String, var pos: Int = 0) {
        val len: Int get() = str.length
        val available: Int get() = len - pos
        val hasMore: Boolean get() = pos < len
        val eof: Boolean get() = !hasMore
        fun peek(offset: Int = 0): Char = str.getOrElse(pos + offset) { '\u0000' }
        fun skip(n: Int = 1): Unit {
            pos += n
        }
        fun read(): Char = peek().also { skip(1) }
        fun expectOpt(c: Char): Boolean {
            if (peek() == c) {
                skip()
                return true
            }
            return false
        }
        fun expect(c: Char) {
            val p = read()
            if (p != c) error("Expected '$c' but found '$p'")
        }

        inline fun readString(block: () -> Unit): String {
            val spos = pos
            block()
            return str.substring(spos, pos)
        }

        inline fun skipWhile(block: (Char) -> Boolean) {
            while (hasMore) {
                if (!block(peek())) break
                skip()
            }
        }
        inline fun skipUntil(block: (Char) -> Boolean) = readWhile { !block(it) }
        inline fun readWhile(block: (Char) -> Boolean): String = readString { skipWhile(block) }
        inline fun readUntil(block: (Char) -> Boolean): String = readWhile { !block(it) }

        fun skipSpaces() = skipWhile { it == ' ' || it == '\t' }
        fun skipSpacesOrNLs() = skipWhile { it == ' ' || it == '\t' || it == '\r' || it == '\n' }

        override fun toString(): String = "StrReader[$len](pos=$pos, peek='${str.substring(pos, min(len, pos + 10))}')"
    }
}
