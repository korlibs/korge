package org.luaj.vm2.compiler

import org.luaj.vm2.*
import org.luaj.vm2.io.*
import org.luaj.vm2.lib.jse.*
import java.io.*
import java.net.*
import kotlin.test.*

abstract class AbstractUnitTests(zipdir: String, zipfile: String, private val dir: String) {
    private val jar: String
    private var globals: Globals = JsePlatform.standardGlobals()

    init {
        var zip: URL? = null
        zip = javaClass.getResource(zipfile)
        if (zip == null) {
            val file = File("$zipdir/$zipfile")
            try {
                if (file.exists())
                    zip = file.toURI().toURL()
            } catch (e: MalformedURLException) {
                e.printStackTrace()
            }

        }
        if (zip == null)
            throw RuntimeException("not found: $zipfile")
        this.jar = "jar:" + zip.toExternalForm() + "!/"
    }

    protected fun pathOfFile(file: String): String = "$jar$dir/$file"
    protected fun inputStreamOfPath(path: String): LuaBinInput = URL(path).readBytes().toLuaBinInput()
    protected fun inputStreamOfFile(file: String): LuaBinInput = inputStreamOfPath(pathOfFile(file))

    protected open fun doTest(file: String) {
        try {
            // load source from jar
            val path = pathOfFile(file)
            val lua = bytesFromJar(path)

            // compile in memory
            val p = globals.loadPrototype(lua.toLuaBinInput(), "@$file", "bt")
            val actual = protoToString(p)

            // load expected value from jar
            val luac = bytesFromJar(path.substring(0, path.length - 4) + ".lc")
            val e = loadFromBytes(luac, file)
            val expected = protoToString(e)

            // compare results
            assertEquals(expected, actual)

            // dump into memory
            val baos = ByteArrayLuaBinOutput()
            DumpState.dump(p, baos, false)
            val dumped = baos.toByteArray()

            // re-undump
            val p2 = loadFromBytes(dumped, file)
            val actual2 = protoToString(p2)

            // compare again
            assertEquals(actual, actual2)

        } catch (e: IOException) {
            throw AssertionError(e.toString())
        }

    }

    protected fun bytesFromJar(path: String): ByteArray {
        val `is` = inputStreamOfPath(path)
        val baos = ByteArrayOutputStream()
        val buffer = ByteArray(2048)
        var n: Int
        while (run {
                n = `is`.read(buffer)
                (n) >= 0
            })
            baos.write(buffer, 0, n)
        `is`.close()
        return baos.toByteArray()
    }

    protected fun loadFromBytes(bytes: ByteArray, script: String): Prototype {
        return globals!!.loadPrototype(BytesLuaBinInput(bytes), script, "b")
    }

    protected fun protoToString(p: Prototype): String {
        val baos = ByteArrayLuaBinOutput()
        val ps = LuaWriterBinOutput(baos)
        Print.ps = ps
        Print.printFunction(p, true)
        return baos.toString()
    }

}
