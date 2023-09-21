package korlibs.js

data class JSStackTrace(val message: String, val entries: List<Entry>) {
    data class Entry(val method: String, val file: String, val line: Int, val column: Int)

    companion object {
        val stackLine = Regex("^\\s+at\\s*(.*?)?\\s+\\(?(.*?):(\\d+):(\\d+)\\)?\$")

        operator fun invoke(): JSStackTrace = current()

        fun parse(stack: String): JSStackTrace {
            val entries = arrayListOf<Entry>()
            var message = arrayListOf<String>()
            for (line in stack.lines()) {
                val stack = stackLine.matchEntire(line)
                if (stack != null) {
                    val (all, method, file, line, column) = stack!!.groupValues
                    entries += Entry(method, file, line.toInt(), column.toInt())
                } else {
                    if (entries.isEmpty()) line.trim().takeIf { it.isNotBlank() }?.let { message += it }
                }

                //println("STACK: ${stack!!.groupValues}")
            }
            return JSStackTrace(message.joinToString("\n"), entries)
        }
        fun from(e: Exception): JSStackTrace = parse(e.stackTraceToString())
        fun current(): JSStackTrace = from(Exception(""))
    }
}
