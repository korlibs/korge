package org.luaj.vm2

import java.io.*

fun LuaValue.isuserdata(c: Class<*>): Boolean = isuserdata(c.kotlin)
fun LuaValue.optuserdata(c: Class<*>, defval: Any?): Any? = optuserdata(c.kotlin, defval)
fun LuaValue.checkuserdata(c: Class<*>): Any? = checkuserdata(c.kotlin)
fun LuaString.toInputStream(): InputStream = ByteArrayInputStream(this.m_bytes, this.m_offset, this.m_length)
