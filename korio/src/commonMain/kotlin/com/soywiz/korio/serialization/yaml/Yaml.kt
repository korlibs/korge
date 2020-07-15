package com.soywiz.korio.serialization.yaml

import com.soywiz.kds.*
import com.soywiz.korio.*
import com.soywiz.korio.lang.*
import com.soywiz.korio.serialization.*
import com.soywiz.korio.util.*
import kotlin.collections.ArrayList
import kotlin.collections.LinkedHashMap
import kotlin.collections.List
import kotlin.collections.MutableMap
import kotlin.collections.arrayListOf
import kotlin.collections.isNotEmpty
import kotlin.collections.last
import kotlin.collections.plusAssign
import kotlin.collections.set
import kotlin.reflect.*

object Yaml {
    fun decode(str: String) = read(ListReader(StrReader(str).tokenize()), level = 0)
    fun read(str: String) = read(ListReader(StrReader(str).tokenize()), level = 0)

    private fun parseStr(tok: Token) = when (tok) {
        is Token.STR -> tok.ustr
        else -> parseStr(tok.str)
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
                            olist += readOrString(s, level, SET_COMMA_END_ARRAY)
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
                        val kkey = s.read()
                        val key = kkey.str
                        if (s.eof || s.peek().str != ":") {
                            if (TRACE) println("${levelStr}LIT: $key")
                            return parseStr(kkey)
                        } else {
                            if (map == null) map = LinkedHashMap()
                            if (s.read().str != ":") invalidOp
                            if (TRACE) println("${levelStr}MAP[$key]...")
                            val value = readOrString(s, level, EMPTY_SET)
                            lastMapKey = key
                            lastMapValue = value
                            map[key] = value
                            if (TRACE) println("${levelStr}MAP[$key]: $value")
                        }
                    }
                }
            }
        }

        if (TRACE) println("${levelStr}RETURN: list=$list, map=$map")

        return map ?: list
    }

    fun readOrString(s: ListReader<Token>, level: Int, delimiters: Set<String>): Any? {
        return if (s.peek() is Token.ID) {
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

    fun tokenize(str: String): List<Token> {
        return StrReader(str).tokenize()
    }

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
                        flush(); out += Token.SYMBOL("$c")
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

        data class LINE(override val str: String, val level: Int) : Token {
            override fun toString(): String = "LINE($level)"
        }

        data class ID(override val str: String) : Token
        data class STR(override val str: String) : Token {
            val ustr = str.unquote()
        }

        data class SYMBOL(override val str: String) : Token
    }

    private fun StrReader.readUntilLineEnd() = this.readUntil { it == '\n' }
}
