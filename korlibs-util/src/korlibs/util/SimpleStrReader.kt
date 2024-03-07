package korlibs.util

interface SimpleStrReader {
    val hasMore: Boolean
    fun readChar(): Char
    fun peekChar(): Char

    companion object {
        private class Impl(val str: String, var pos: Int) : SimpleStrReader {
            override val hasMore: Boolean get() = pos < str.length

            override fun readChar(): Char = peekChar().also { pos++ }
            override fun peekChar(): Char = str.getOrElse(pos) { '\u0000' }
        }
        operator fun invoke(str: String, pos: Int = 0): SimpleStrReader = Impl(str, pos)
    }
}
