package org.luaj.vm2.io

abstract class LuaReader {
    abstract fun read(): Int
    fun read(cbuf: CharArray) = read(cbuf, 0, cbuf.size)
    open fun read(cbuf: CharArray, off: Int, len: Int): Int {
        for (n in 0 until len) {
            val c = read()
            if (c >= 0) {
                cbuf[off + n] = c.toChar()
            } else {
                return if (n == 0) -1 else n
            }
        }
        return len
    }
    open fun close(): Unit = Unit
}

/** Reader implementation to read chars from a String in JME or JSE.  */
open class StrLuaReader(val s: String) : LuaReader() {
    var i = 0
    val n: Int = s.length

    override fun close() { i = n }
    override fun read(): Int = if (i < n) s[i++].toInt() and 0xFF else -1

    override fun read(cbuf: CharArray, off: Int, len: Int): Int {
        var j = 0
        while (j < len && i < n) cbuf[off + j++] = s[i++]
        return if (j > 0 || len == 0) j else -1
    }
}

fun String.luaReader() = StrLuaReader(this)


// @TODO: Move to Java. Use UTF-8
open class InputStreamLuaReader(val iss: LuaBinInput, val encoding: String? = null) : LuaReader() {
    init {
        if (encoding != null) {
            error("Unsupported encoding $encoding")
        }
    }
    override fun read(): Int = iss.read()
    override fun close() = iss.close()
}

fun LuaBinInput.reader(encoding: String? = null) = InputStreamLuaReader(this, encoding)
