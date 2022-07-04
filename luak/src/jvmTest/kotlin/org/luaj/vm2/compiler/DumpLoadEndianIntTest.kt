package org.luaj.vm2.compiler

import org.luaj.vm2.*
import org.luaj.vm2.io.*
import org.luaj.vm2.lib.jse.*
import java.io.*
import kotlin.test.*


class DumpLoadEndianIntTest {

    private var globals: Globals = JsePlatform.standardGlobals()

    init {
        DumpState.ALLOW_INTEGER_CASTING = false
    }

    @Test
    fun testBigDoubleCompile() {
        doTest(
            false,
            DumpState.NUMBER_FORMAT_FLOATS_OR_DOUBLES,
            false,
            mixedscript,
            withdoubles,
            withdoubles,
            SHOULDPASS
        )
        doTest(
            false,
            DumpState.NUMBER_FORMAT_FLOATS_OR_DOUBLES,
            true,
            mixedscript,
            withdoubles,
            withdoubles,
            SHOULDPASS
        )
    }

    @Test
    fun testLittleDoubleCompile() {
        doTest(
            true,
            DumpState.NUMBER_FORMAT_FLOATS_OR_DOUBLES,
            false,
            mixedscript,
            withdoubles,
            withdoubles,
            SHOULDPASS
        )
        doTest(true, DumpState.NUMBER_FORMAT_FLOATS_OR_DOUBLES, true, mixedscript, withdoubles, withdoubles, SHOULDPASS)
    }

    @Test
    fun testBigIntCompile() {
        DumpState.ALLOW_INTEGER_CASTING = true
        doTest(false, DumpState.NUMBER_FORMAT_INTS_ONLY, false, mixedscript, withdoubles, withints, SHOULDPASS)
        doTest(false, DumpState.NUMBER_FORMAT_INTS_ONLY, true, mixedscript, withdoubles, withints, SHOULDPASS)
        DumpState.ALLOW_INTEGER_CASTING = false
        doTest(false, DumpState.NUMBER_FORMAT_INTS_ONLY, false, mixedscript, withdoubles, withints, SHOULDFAIL)
        doTest(false, DumpState.NUMBER_FORMAT_INTS_ONLY, true, mixedscript, withdoubles, withints, SHOULDFAIL)
        doTest(false, DumpState.NUMBER_FORMAT_INTS_ONLY, false, intscript, withints, withints, SHOULDPASS)
        doTest(false, DumpState.NUMBER_FORMAT_INTS_ONLY, true, intscript, withints, withints, SHOULDPASS)
    }

    @Test
    fun testLittleIntCompile() {
        DumpState.ALLOW_INTEGER_CASTING = true
        doTest(true, DumpState.NUMBER_FORMAT_INTS_ONLY, false, mixedscript, withdoubles, withints, SHOULDPASS)
        doTest(true, DumpState.NUMBER_FORMAT_INTS_ONLY, true, mixedscript, withdoubles, withints, SHOULDPASS)
        DumpState.ALLOW_INTEGER_CASTING = false
        doTest(true, DumpState.NUMBER_FORMAT_INTS_ONLY, false, mixedscript, withdoubles, withints, SHOULDFAIL)
        doTest(true, DumpState.NUMBER_FORMAT_INTS_ONLY, true, mixedscript, withdoubles, withints, SHOULDFAIL)
        doTest(true, DumpState.NUMBER_FORMAT_INTS_ONLY, false, intscript, withints, withints, SHOULDPASS)
        doTest(true, DumpState.NUMBER_FORMAT_INTS_ONLY, true, intscript, withints, withints, SHOULDPASS)
    }

    @Test
    fun testBigNumpatchCompile() {
        doTest(false, DumpState.NUMBER_FORMAT_NUM_PATCH_INT32, false, mixedscript, withdoubles, withdoubles, SHOULDPASS)
        doTest(false, DumpState.NUMBER_FORMAT_NUM_PATCH_INT32, true, mixedscript, withdoubles, withdoubles, SHOULDPASS)
    }

    @Test
    fun testLittleNumpatchCompile() {
        doTest(true, DumpState.NUMBER_FORMAT_NUM_PATCH_INT32, false, mixedscript, withdoubles, withdoubles, SHOULDPASS)
        doTest(true, DumpState.NUMBER_FORMAT_NUM_PATCH_INT32, true, mixedscript, withdoubles, withdoubles, SHOULDPASS)
    }

    fun doTest(
        littleEndian: Boolean, numberFormat: Int, stripDebug: Boolean,
        script: String, expectedPriorDump: String, expectedPostDump: String, shouldPass: Boolean
    ) {
        try {

            // compile into prototype
            val reader = StrLuaReader(script)
            val p = globals!!.compilePrototype(reader, "script")

            // double check script result before dumping
            var f: LuaFunction? = LuaClosure(p, globals)
            var r = f!!.call()
            var actual = r.tojstring()
            assertEquals(expectedPriorDump, actual)

            // dump into bytes
            val baos = ByteArrayLuaBinOutput()
            try {
                DumpState.dump(p, baos, stripDebug, numberFormat, littleEndian)
                if (!shouldPass)
                    fail("dump should not have succeeded")
            } catch (e: Exception) {
                if (shouldPass)
                    fail("dump threw $e")
                else
                    return
            }

            val dumped = baos.toByteArray()

            // load again using compiler
            val `is` = BytesLuaBinInput(dumped)
            f = globals!!.load(`is`, "dumped", "b", globals!!).checkfunction()
            r = f!!.call()
            actual = r.tojstring()
            assertEquals(expectedPostDump, actual)

            // write test chunk
            if (System.getProperty(SAVECHUNKS) != null && script == mixedscript) {
                File("build").mkdirs()
                val filename = ("build/test-"
                    + (if (littleEndian) "little-" else "big-")
                    + (if (numberFormat == DumpState.NUMBER_FORMAT_FLOATS_OR_DOUBLES)
                    "double-"
                else if (numberFormat == DumpState.NUMBER_FORMAT_INTS_ONLY)
                    "int-"
                else if (numberFormat == DumpState.NUMBER_FORMAT_NUM_PATCH_INT32) "numpatch4-" else "???-")
                    + (if (stripDebug) "nodebug-" else "debug-")
                    + "bin.lua")
                val fos = FileOutputStream(filename)
                fos.write(dumped)
                fos.close()
            }

        } catch (e: IOException) {
            fail(e.toString())
        }

    }

    companion object {
        private val SAVECHUNKS = "SAVECHUNKS"

        private val SHOULDPASS = true
        private val SHOULDFAIL = false
        private val mixedscript = "return tostring(1234)..'-#!-'..tostring(23.75)"
        private val intscript = "return tostring(1234)..'-#!-'..tostring(23)"
        private val withdoubles = "1234-#!-23.75"
        private val withints = "1234-#!-23"
    }
}
