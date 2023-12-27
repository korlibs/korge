package korlibs.js

data class JSStackTrace(val message: String, val entries: List<Entry>) {
    data class Entry(val method: String, val file: String, val line: Int, val column: Int = -1)

    companion object {
        operator fun invoke(): JSStackTrace = current()

        fun parse(stack: String, message: String? = null): JSStackTrace {
            val entries = arrayListOf<Entry>()
            var messageLines = arrayListOf<String>()
            var isChrome = false
            for ((index, strLine) in stack.lines().withIndex()) {
                val strLine = strLine.trimEnd()
                if (strLine.isEmpty() && !isChrome) continue
                // Chrome first line
                when {
                    (strLine.startsWith("Error: ") || strLine == "Error") && index == 0 -> {
                        isChrome = true
                        messageLines.add(strLine.substring(7))
                    }
                    strLine.startsWith("    at ") -> {
                        val part = strLine.substring(7).trimEnd(')')
                        val column = part.substringAfterLast(':')
                        val part0 = part.substringBeforeLast(':')
                        val line = part0.substringAfterLast(':')
                        val part1 = part0.substringBeforeLast(':')
                        val (method, file) = if (part1.contains('(')) {
                            part1.split("(").map { it.trim() }
                        } else {
                            listOf("", part1.trim())
                        }
                        entries += Entry(method, file, line.toIntOrNull() ?: -1, column.toIntOrNull() ?: -1)
                    }
                    isChrome -> {
                        messageLines.add(strLine)
                    }
                    else -> {
                        var cline = strLine
                        val numParts = arrayListOf<Int>()
                        for (n in 0 until 2) {
                            cline = Regex("^(.*):(\\d+)$").replace(cline) {
                                numParts.add(0, it.groupValues[2].toIntOrNull() ?: -1)
                                it.groupValues[1]
                            }
                        }
                        val file = cline.substringAfterLast('@')
                        val method = cline.substringBeforeLast('@')
                        val line = numParts.firstOrNull() ?: -1
                        val column = if (numParts.size >= 2) numParts.lastOrNull() ?: -1 else -1
                        entries += Entry(method, file, line, column)
                    }
                }

                //println("STACK: ${stack!!.groupValues}")
            }
            if (!isChrome) {
                messageLines = arrayListOf(message ?: "")
            }
            if (entries.isEmpty()) {
                entries.add(Entry("<unknown>", "<unknown>", -1))
            }
            return JSStackTrace(messageLines.joinToString("\n"), entries)
        }
        fun from(e: Exception): JSStackTrace = parse(e.stackTraceToString(), e.message)
        fun current(): JSStackTrace = from(Exception(""))
    }
}
