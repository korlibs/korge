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

import org.luaj.vm2.LuaValue
import org.luaj.vm2.internal.*
import java.lang.reflect.*
import kotlin.math.*

/**
 * LuaValue that represents a Java class.
 *
 *
 * Will respond to get() and set() by returning field values, or java methods.
 *
 *
 * This class is not used directly.
 * It is returned by calls to [CoerceJavaToLua.coerce]
 * when a Class is supplied.
 * @see CoerceJavaToLua
 *
 * @see CoerceLuaToJava
 */
class JavaClass internal constructor(c: Class<*>) : JavaInstance(c), CoerceJavaToLua.Coercion {

    @kotlin.jvm.JvmField internal var fields: Map<LuaValue, Field>? = null
    @kotlin.jvm.JvmField internal var methods: Map<LuaValue, LuaValue>? = null
    @kotlin.jvm.JvmField internal var innerclasses: Map<LuaValue, Class<*>>? = null

    fun getConstructor(): LuaValue? = getMethod(NEW)

    init {
        this.jclass = this
    }

    override fun coerce(javaValue: Any): LuaValue {
        return this
    }

    internal fun getField(key: LuaValue): Field? {
        if (fields == null) {
            val m = HashMap<LuaValue, Field>()
            val f = (m_instance as Class<*>).fields
            for (i in f.indices) {
                val fi = f[i]
                if (Modifier.isPublic(fi.modifiers)) {
                    m[LuaValue.valueOf(fi.name)] = fi
                    try {
                        if (!fi.isAccessible) fi.isAccessible = true
                    } catch (s: SecurityException) {
                    }

                }
            }
            fields = m
        }
        return fields?.get(key)
    }

    internal fun getMethod(key: LuaValue): LuaValue? {
        if (methods == null) {
            val namedlists = HashMap<String, MutableList<JavaMethod>>()
            val m = (m_instance as Class<*>).methods
            for (i in m.indices) {
                val mi = m[i]
                if (Modifier.isPublic(mi.modifiers)) {
                    val name = mi.name
                    var list = namedlists[name]
                    if (list == null) {
                        list = ArrayList()
                        namedlists[name] = list
                    }
                    list.add(JavaMethod.forMethod(mi))
                }
            }
            val map = HashMap<LuaValue, LuaValue>()
            val c = (m_instance as Class<*>).constructors
            val list = ArrayList<JavaConstructor>()
            for (i in c.indices) if (Modifier.isPublic(c[i].modifiers)) {
                list.add(JavaConstructor.forConstructor(c[i]))
            }
            when (list.size) {
                0 -> Unit
                1 -> map[NEW] = list[0]
                else -> map[NEW] = JavaConstructor.forConstructors(list.toTypedArray())
            }

            for ((name, methods) in namedlists.entries) {
                map[LuaValue.valueOf(name)] = if (methods.size == 1) methods[0] else JavaMethod.forMethods(methods.toTypedArray())
            }
            methods = map
        }
        return methods?.get(key)
    }

    internal fun getInnerClass(key: LuaValue): Class<*>? {
        if (innerclasses == null) {
            val m = HashMap<LuaValue, Class<*>>()
            val c = (m_instance as Class<*>).classes
            for (i in c.indices) {
                val ci = c[i]
                val name = ci.name
                val stub = name.substring(max(name.lastIndexOf('$'), name.lastIndexOf('.')) + 1)
                m[LuaValue.valueOf(stub)] = ci
            }
            innerclasses = m
        }
        return innerclasses?.get(key)
    }

    companion object {

        @kotlin.jvm.JvmField internal val classes: MutableMap<Class<*>, JavaClass> = HashMap()

        @kotlin.jvm.JvmField internal val NEW: LuaValue = LuaValue.valueOf("new")

         fun forClass(c: Class<*>): JavaClass {
            return classes[c] ?: return JavaClass(c).also { classes[c] = it }
        }
    }
}
