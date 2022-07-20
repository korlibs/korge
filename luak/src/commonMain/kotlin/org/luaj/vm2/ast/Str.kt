/*******************************************************************************
 * Copyright (c) 2010 Luaj.org. All rights reserved.
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
package org.luaj.vm2.ast

import org.luaj.vm2.LuaString
import org.luaj.vm2.internal.*
import org.luaj.vm2.io.*

object Str {


    fun quoteString(image: String): LuaString {
        val s = image.substring(1, image.length - 1)
        val bytes = unquote(s)
        return LuaString.valueUsing(bytes)
    }


    fun charString(image: String): LuaString {
        val s = image.substring(1, image.length - 1)
        val bytes = unquote(s)
        return LuaString.valueUsing(bytes)
    }


    fun longString(image: String): LuaString {
        val i = image.indexOf('[', image.indexOf('[') + 1) + 1
        val s = image.substring(i, image.length - i)
        val b = iso88591bytes(s)
        return LuaString.valueUsing(b)
    }


    fun iso88591bytes(s: String): ByteArray {
        val baos = ByteArrayLuaBinOutput()
        for (c in s) {
            baos.write(c.toInt()) // @TODO: Might require some adjustments?
        }
        return baos.toByteArray()
    }

    @OptIn(ExperimentalStdlibApi::class)

    fun unquote(s: String): ByteArray {
        val baos = ByteArrayLuaBinOutput()
        val c = s.toCharArray()
        val n = c.size
        var i = 0
        loop@while (i < n) {
            if (c[i] == '\\' && i < n) {
                when (c[++i]) {
                    '0', '1', '2', '3', '4', '5', '6', '7', '8', '9' -> {
                        var d = c[i++] - '0'
                        var j = 0
                        while (i < n && j < 2 && c[i] >= '0' && c[i] <= '9') {
                            d = d * 10 + (c[i] - '0')
                            i++
                            j++
                        }
                        baos.write(d.toByte().toInt())
                        --i
                        i++
                        continue@loop
                    }
                    'a' -> {
                        baos.write(7.toByte().toInt())
                        i++
                        continue@loop
                    }
                    'b' -> {
                        baos.write('\b'.toByte().toInt())
                        i++
                        continue@loop
                    }
                    'f' -> {
                        //baos.write('\f'.toByte().toInt())
                        baos.write('\u000c'.toByte().toInt())
                        i++
                        continue@loop
                    }
                    'n' -> {
                        baos.write('\n'.toByte().toInt())
                        i++
                        continue@loop
                    }
                    'r' -> {
                        baos.write('\r'.toByte().toInt())
                        i++
                        continue@loop
                    }
                    't' -> {
                        baos.write('\t'.toByte().toInt())
                        i++
                        continue@loop
                    }
                    'v' -> {
                        baos.write(11.toByte().toInt())
                        i++
                        continue@loop
                    }
                    '"' -> {
                        baos.write('"'.toByte().toInt())
                        i++
                        continue@loop
                    }
                    '\'' -> {
                        baos.write('\''.toByte().toInt())
                        i++
                        continue@loop
                    }
                    '\\' -> {
                        baos.write('\\'.toByte().toInt())
                        i++
                        continue@loop
                    }
                    else -> baos.write(c[i].toByte().toInt())
                }
            } else {
                baos.write(c[i].toByte().toInt())
            }
            i++
        }
        return baos.toByteArray()
    }
}
