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
package org.luaj.vm2.server

import org.luaj.vm2.io.*

/** Interface to launch lua scripts using the [LuajClassLoader].
 * <P>
 * *Note: This class is experimental and subject to change in future versions.*
</P> * <P>
 * This interface is purposely genericized to defer class loading so that
 * luaj classes can come from the class loader.
</P> * <P>
 * The implementation should be acquired using [LuajClassLoader.NewLauncher]
 * or [LuajClassLoader.NewLauncher] which ensure that the classes are
 * loaded to give each Launcher instance a pristine set of Globals, including
 * the shared metatables.
 *
 * @see LuajClassLoader
 *
 * @see LuajClassLoader.NewLauncher
 * @see LuajClassLoader.NewLauncher
 * @see DefaultLauncher
 *
 * @since luaj 3.0.1
</P> */
interface Launcher {

    /** Launch a script contained in a String.
     *
     * @param script The script contents.
     * @param arg Optional arguments supplied to the script.
     * @return return values from the script.
     */
    fun launch(script: String, arg: Array<Any>?): Array<Any?>?

    /** Launch a script from an InputStream.
     *
     * @param script The script as an InputStream.
     * @param arg Optional arguments supplied to the script.
     * @return return values from the script.
     */
    fun launch(script: LuaBinInput, arg: Array<Any>?): Array<Any?>?

    /** Launch a script from a Reader.
     *
     * @param script The script as a Reader.
     * @param arg Optional arguments supplied to the script.
     * @return return values from the script.
     */
    fun launch(script: LuaReader, arg: Array<Any>?): Array<Any?>?
}
