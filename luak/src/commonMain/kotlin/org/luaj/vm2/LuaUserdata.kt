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

import org.luaj.vm2.internal.*
import kotlin.reflect.*

open class LuaUserdata : LuaValue {

    @kotlin.jvm.JvmField
    var m_instance: Any
    @kotlin.jvm.JvmField
    var m_metatable: LuaValue? = null

    constructor(obj: Any) {
        m_instance = obj
    }

    constructor(obj: Any, metatable: LuaValue?) {
        m_instance = obj
        m_metatable = metatable
    }

    override fun tojstring(): String {
        return m_instance.toString()
    }

    override fun type(): Int {
        return LuaValue.TUSERDATA
    }

    override fun typename(): String {
        return "userdata"
    }

    override fun hashCode(): Int {
        return m_instance.hashCode()
    }

    fun userdata(): Any {
        return m_instance
    }

    override fun isuserdata(): Boolean {
        return true
    }

    override fun isuserdata(c: KClass<*>): Boolean {
        return c.isInstancePortable(m_instance)
    }

    override fun touserdata(): Any {
        return m_instance
    }

    override fun touserdata(c: KClass<*>): Any? {
        return if (c.isInstancePortable(m_instance)) m_instance else null
    }

    override fun optuserdata(defval: Any?): Any? {
        return m_instance
    }

    override fun optuserdata(c: KClass<*>, defval: Any?): Any? {
        if (!c.isInstancePortable(m_instance))
            typerror(c.portableName)
        return m_instance
    }

    override fun getmetatable(): LuaValue? {
        return m_metatable
    }

    override fun setmetatable(metatable: LuaValue?): LuaValue {
        this.m_metatable = metatable
        return this
    }

    override fun checkuserdata(): Any? {
        return m_instance
    }

    override fun checkuserdata(c: KClass<*>): Any? {

        return if (c.isInstancePortable(m_instance)) m_instance else typerror(c.portableName)
    }

    override fun get(key: LuaValue): LuaValue {
        return if (m_metatable != null) LuaValue.gettable(this, key) else LuaValue.NIL
    }

    override fun set(key: LuaValue, value: LuaValue) {
        if (m_metatable == null || !LuaValue.settable(this, key, value))
            LuaValue.error("cannot set $key for userdata")
    }

    override fun equals(`val`: Any?): Boolean {
        if (this === `val`)
            return true
        if (`val` !is LuaUserdata)
            return false
        val u = `val` as LuaUserdata?
        return m_instance == u!!.m_instance
    }

    // equality w/ metatable processing
    override fun eq(`val`: LuaValue): LuaValue {
        return if (eq_b(`val`)) LuaValue.BTRUE else LuaValue.BFALSE
    }

    override fun eq_b(`val`: LuaValue): Boolean {
        if (`val`.raweq(this)) return true
        if (m_metatable == null || !`val`.isuserdata()) return false
        val valmt = `val`.getmetatable()
        return valmt != null && LuaValue.eqmtcall(this, m_metatable!!, `val`, valmt)
    }

    // equality w/o metatable processing
    override fun raweq(`val`: LuaValue): Boolean {
        return `val`.raweq(this)
    }

    override fun raweq(`val`: LuaUserdata): Boolean {
        return this === `val` || m_metatable === `val`.m_metatable && m_instance == `val`.m_instance
    }

    // __eq metatag processing
    fun eqmt(`val`: LuaValue): Boolean {
        return if (m_metatable != null && `val`.isuserdata()) LuaValue.eqmtcall(
            this,
            m_metatable!!,
            `val`,
            `val`.getmetatable()!!
        ) else false
    }
}
