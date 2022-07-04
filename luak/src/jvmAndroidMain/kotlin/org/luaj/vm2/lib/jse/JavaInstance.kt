/*******************************************************************************
 * Copyright (c) 2011 Luaj.org. All rights reserved.
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
package org.luaj.vm2.lib.jse

import org.luaj.vm2.LuaError
import org.luaj.vm2.LuaUserdata
import org.luaj.vm2.LuaValue

/**
 * LuaValue that represents a Java instance.
 *
 *
 * Will respond to get() and set() by returning field values or methods.
 *
 *
 * This class is not used directly.
 * It is returned by calls to [CoerceJavaToLua.coerce]
 * when a subclass of Object is supplied.
 * @see CoerceJavaToLua
 *
 * @see CoerceLuaToJava
 */
open class JavaInstance(instance: Any) : LuaUserdata(instance) {

    @kotlin.jvm.JvmField
    var jclass: JavaClass? = null

    override fun get(key: LuaValue): LuaValue {
        if (jclass == null)
            jclass = JavaClass.forClass(m_instance.javaClass)
        val f = jclass!!.getField(key)
        if (f != null)
            try {
                return CoerceJavaToLua.coerce(f.get(m_instance))
            } catch (e: Exception) {
                throw LuaError(e)
            }

        val m = jclass!!.getMethod(key)
        if (m != null)
            return m
        val c = jclass!!.getInnerClass(key)
        return if (c != null) JavaClass.forClass(c) else super.get(key)
    }

    override fun set(key: LuaValue, value: LuaValue) {
        if (jclass == null)
            jclass = JavaClass.forClass(m_instance.javaClass)
        val f = jclass!!.getField(key)
        if (f != null)
            try {
                f.set(m_instance, CoerceLuaToJava.coerce(value, f.type))
                return
            } catch (e: Exception) {
                throw LuaError(e)
            }

        super.set(key, value)
    }

}
