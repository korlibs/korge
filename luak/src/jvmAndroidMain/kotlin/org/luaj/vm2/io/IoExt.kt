package org.luaj.vm2.io

import java.io.*

fun Reader.toLua(): LuaReader {
    val self = this
    return object : LuaReader() {
        override fun read(): Int = self.read()
        override fun read(cbuf: CharArray, off: Int, len: Int): Int = self.read(cbuf, off, len)
        override fun close() = self.close()
    }
}

fun InputStream.toLua(): LuaBinInput {
    val self = this
    return object : LuaBinInput() {
        override fun read(out: ByteArray, off: Int, size: Int): Int = self.read(out, off, size)
        override fun skip(n: Long): Long = self.skip(n)
        override fun available(): Int = self.available()
        override fun markSupported(): Boolean = self.markSupported()
        override fun mark(value: Int) = self.mark(value)
        override fun reset() = self.reset()
        override fun close() = self.close()
        override fun read(): Int = self.read()
    }
}

fun OutputStream.toLua(): LuaBinOutput {
    val self = this
    return object : LuaBinOutput() {
        override fun write(value: Int) = self.write(value)
        override fun write(b: ByteArray, i: Int, size: Int) = self.write(b, i, size)
        override fun flush() = self.flush()
        override fun close() = self.close()
    }
}

fun PrintStream.toLua(): LuaWriter {
    val self = this
    return object : LuaWriter() {
        override fun write(value: Int) = self.write(value)
        override fun print(v: String) = self.print(v)
    }
}

fun LuaBinOutput.toOutputStream(): OutputStream {
    val self = this
    return object : OutputStream() {
        override fun write(b: Int) {
            self.write(b)
        }
    }
}
