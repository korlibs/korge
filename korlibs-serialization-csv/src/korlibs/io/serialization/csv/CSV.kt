@file:Suppress("PackageDirectoryMismatch")

package korlibs.io.serialization.csv

import korlibs.util.*

class CSV(val lines: List<List<String>>, val names: List<String>? = null) : Collection<CSV.Record> {
    val namesToIndex: Map<String, Int> = names?.withIndex()?.associate { it.value to it.index } ?: emptyMap()
    val linesWithNames = if (names != null) listOf(names, *lines.toTypedArray()) else lines
    val records: List<Record> = lines.map { Record(it) }

    override val size: Int get() = records.size
    operator fun get(index: Int) = records[index]

    override fun iterator(): Iterator<Record> = records.iterator()
    override fun contains(element: Record): Boolean = records.contains(element)
    override fun containsAll(elements: Collection<Record>): Boolean = records.containsAll(elements)
    override fun isEmpty(): Boolean = records.isEmpty()

    inner class Record(val cells: List<String>) {
        operator fun get(index: Int): String = getOrNull(index) ?: error("Can't find element at $index")
        operator fun get(name: String): String = getOrNull(name) ?: error("Can't find element '$name'")

        fun getOrNull(index: Int): String? = cells.getOrNull(index)
        fun getOrNull(name: String): String? = namesToIndex[name]?.let { cells[it] }

        fun toMap(): Map<String, String> = if (names != null) cells.zip(names).associate { it.first to it.second } else cells.mapIndexed { index, s -> index to s }.associate { "${it.first}" to it.second }
        override fun toString(): String = if (names != null) "${toMap()}" else "$cells"
    }

    fun toString(separator: Char): String = linesWithNames.joinToString("\n") { serializeLine(it, separator) }
    override fun toString(): String = toString(DEFAULT_SEPARATOR)

    companion object {
        const val DEFAULT_SEPARATOR = ','

        internal fun serializeElement(value: String, separator: Char): String {
            if (!value.contains('"') && !value.contains('\n') && !value.contains(separator)) return value
            val out = StringBuilder(value.length)
            for (n in 0 until value.length) {
                out.append(value[n])
            }
            return out.toString()
        }

        fun serializeLine(values: List<String>, separator: Char = DEFAULT_SEPARATOR): String {
            return values.joinToString("$separator") { serializeElement(it, separator) }
        }

        fun parseLine(line: String, separator: Char = DEFAULT_SEPARATOR): List<String> = parseLine(SimpleStrReader(line), separator)

        fun parseLine(line: SimpleStrReader, separator: Char = DEFAULT_SEPARATOR): List<String> {
            val out = arrayListOf<String>()
            val str = StringBuilder()
            while (line.hasMore) {
                val c = line.readChar()
                when (c) {
                    // Quoted string
                    '"' -> {
                        loop@while (line.hasMore) {
                            val c2 = line.readChar()
                            when (c2) {
                                '"' -> {
                                    if (line.peekChar() == '"') {
                                        line.readChar()
                                        str.append('"')
                                    } else {
                                        break@loop
                                    }
                                }
                                else -> str.append(c2)
                            }
                        }
                    }
                    // Line break
                    '\n' -> {
                        break
                    }
                    // Empty string
                    separator -> {
                        out.add(str.toString())
                        str.clear()
                    }
                    // Normal string
                    else -> {
                        str.append(c)
                    }
                }
            }
            out.add(str.toString())
            str.clear()
            return out
        }

        fun parse(s: SimpleStrReader, separator: Char = DEFAULT_SEPARATOR, headerNames: Boolean = true): CSV {
            val lines = arrayListOf<List<String>>()
            while (s.hasMore) {
                lines.add(parseLine(s, separator))
            }
            return if (headerNames) CSV(lines.drop(1), lines[0]) else CSV(lines, null)
        }

        fun parse(str: String, separator: Char = DEFAULT_SEPARATOR, headerNames: Boolean = true): CSV = parse(SimpleStrReader(str), separator, headerNames)
    }
}
