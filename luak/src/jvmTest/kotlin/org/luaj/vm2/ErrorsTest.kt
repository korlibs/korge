/*******************************************************************************
 * Copyright (c) 2009 Luaj.org. All rights reserved.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package org.luaj.vm2

import org.luaj.vm2.io.*
import java.io.*
import kotlin.test.*

/**
 * Test argument type check errors
 *
 * Results are compared for exact match with
 * the installed C-based lua environment.
 */
class ErrorsTest : ScriptDrivenTest(ScriptDrivenTest.PlatformType.JSE, dir) {

    fun testBaseLibArgs() {
        globals.STDIN = object : LuaBinInput() { override fun read(): Int = -1 }
        runTest("baselibargs")
    }

    @Test fun testCoroutineLibArgs() { runTest("coroutinelibargs") }

    //@Test fun testDebugLibArgs() { runTest("debuglibargs") }
    //@Test fun testIoLibArgs() { runTest("iolibargs") }
    @Test fun testMathLibArgs() { runTest("mathlibargs") }
    //@Test fun testModuleLibArgs() { runTest("modulelibargs") }
    @Test fun testOperators() { runTest("operators") }
    //@Test fun testStringLibArgs() { runTest("stringlibargs") }
    @Test fun testTableLibArgs() { runTest("tablelibargs") }

    companion object {

        private val dir = "errors/"
    }

}
