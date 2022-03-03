package com.soywiz.korgw

import com.soywiz.korio.lang.Charsets
import com.soywiz.korio.lang.toString
import com.soywiz.korio.stream.MemorySyncStream
import com.soywiz.korio.stream.toByteArray
import kotlinx.cinterop.*
import platform.posix.fread
import platform.posix.pclose
import platform.posix.popen

private fun escapeshellarg(str: String) = "'" + str.replace("'", "\\'") + "'"

// @TODO: Move this to Korio exec/execToString
class NativeZenityDialogs : ZenityDialogs() {
    override suspend fun exec(vararg args: String): String = memScoped {
        val command = "/bin/sh -c '" + args.joinToString(" ") { escapeshellarg(it) }.replace("'", "\"'\"") + "' 2>&1"
        println("COMMAND: $command")
        val fp = popen(command, "r")
            ?: error("Couldn't exec ${args.toList()}")

        val out = MemorySyncStream()
        val TMPSIZE = 1024
        val temp = allocArray<ByteVar>(TMPSIZE)

        do {
            val res = fread(temp, 1.convert(), TMPSIZE.convert(), fp)
            for (n in 0 until res.toInt()) {
                out.write(temp[n].toInt() and 0xFF)
            }
        } while (res.toInt() > 0)
        pclose(fp)
        return out.toByteArray().toString(Charsets.UTF8)
    }
}
