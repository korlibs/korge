/*******************************************************************************
 * Copyright (c) 2015 Luaj.org. All rights reserved.
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
import org.luaj.vm2.lib.jse.*
import org.luaj.vm2.server.*
import java.io.*
import kotlin.test.*

// Tests using class loading orders that have caused problems for some use cases.
class LoadOrderTest {

    @Test
    fun testLoadGlobalsFirst() {
        val g = JsePlatform.standardGlobals()
        assertNotNull(g)
    }

    @Test
    fun testLoadStringFirst() {
        val BAR = LuaString.valueOf("bar")
        assertNotNull(BAR)
    }

    class TestLauncherLoadStringFirst : Launcher {

        override fun launch(script: String, arg: Array<Any>?): Array<Any?>? {
            //return arrayOf(FOO)
            return arrayOf(LuaString.valueOf("foo"))
        }

        override fun launch(script: LuaBinInput, arg: Array<Any>?): Array<Any?>? {
            return null
        }

        override fun launch(script: LuaReader, arg: Array<Any>?): Array<Any?>? {
            return null
        }

        companion object {
            // Static initializer that causes LuaString->LuaValue->LuaString
            private val FOO by lazy { LuaString.valueOf("foo") }
        }
    }

    @Test
    fun testClassLoadsStringFirst() {
        val launcher = LuajClassLoader
            .NewLauncher(TestLauncherLoadStringFirst::class.java)
        val results = launcher.launch("foo", null)
        assertNotNull(results)
    }

}
