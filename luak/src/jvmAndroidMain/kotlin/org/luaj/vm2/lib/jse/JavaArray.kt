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

import org.luaj.vm2.LuaTable
import org.luaj.vm2.LuaUserdata
import org.luaj.vm2.LuaValue
import org.luaj.vm2.lib.OneArgFunction

/**
 * LuaValue that represents a Java instance of array type.
 *
 *
 * Can get elements by their integer key index, as well as the length.
 *
 *
 * This class is not used directly.
 * It is returned by calls to [CoerceJavaToLua.coerce]
 * when an array is supplied.
 * @see CoerceJavaToLua
 *
 * @see CoerceLuaToJava
 */
internal class JavaArray(instance: Any) : LuaUserdata(instance) {

    private class LenFunction : OneArgFunction() {
        override fun call(u: LuaValue): LuaValue {
            return LuaValue.valueOf(java.lang.reflect.Array.getLength((u as LuaUserdata).m_instance))
        }
    }

    init {
        setmetatable(array_metatable)
    }

    override fun get(key: LuaValue): LuaValue {
        return when {
            key == LENGTH -> LuaValue.valueOf(java.lang.reflect.Array.getLength(m_instance))
            key.isint() -> {
                val i = key.toint() - 1
                if (i >= 0 && i < java.lang.reflect.Array.getLength(m_instance))
                    CoerceJavaToLua.coerce(java.lang.reflect.Array.get(m_instance, key.toint() - 1))
                else
                    LuaValue.NIL
            }
            else -> super.get(key)
        }
    }

    override fun set(key: LuaValue, value: LuaValue) {
        if (key.isint()) {
            val i = key.toint() - 1
            if (i >= 0 && i < java.lang.reflect.Array.getLength(m_instance))
                java.lang.reflect.Array.set(m_instance, i, CoerceLuaToJava.coerce(value, m_instance.javaClass.componentType))
            else if (m_metatable == null || !LuaValue.settable(this, key, value))
                LuaValue.error("array index out of bounds")
        } else
            super.set(key, value)
    }

    companion object {
        @kotlin.jvm.JvmField
        val LENGTH: LuaValue = LuaValue.valueOf("length")

        @kotlin.jvm.JvmField
        val array_metatable: LuaTable = LuaTable().apply {
            rawset(LuaValue.LEN, LenFunction())
        }
    }
}
