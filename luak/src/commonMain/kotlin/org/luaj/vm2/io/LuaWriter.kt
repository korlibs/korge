package org.luaj.vm2.io

abstract class LuaWriter : LuaBinOutput() {
    abstract fun print(v: String)
    open fun print(v: Any?) = print(v?.toString() ?: "null")
    open fun println(v: String) = print(v).also { print("\n") }
    open fun println(v: Any?) = print(v?.toString() ?: "null").also { print("\n") }
    open fun println() = print("\n")
}

open class LuaWriterBinOutput(val out: LuaBinOutput, val charset: String? = null) : LuaWriter() {
    override fun write(value: Int) = out.write(value)
    override fun write(b: ByteArray, i: Int, size: Int) = out.write(b, i, size)
    override fun writeByte(v: Int) = out.writeByte(v)
    override fun writeInt(v: Int) = out.writeInt(v)
    override fun writeLong(v: Long) = out.writeLong(v)

    @OptIn(ExperimentalStdlibApi::class)
    override fun print(v: String) = out.write(v.encodeToByteArray())
    override fun flush() = out.flush()
    override fun close() = out.close()
}

fun LuaBinOutput.toWriter(charset: String? = null) = LuaWriterBinOutput(this, charset)
