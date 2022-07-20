package org.luaj.vm2

import org.luaj.vm2.lib.jse.*
import org.luaj.vm2.require.*
import kotlin.test.*

class RequireClassTest {

    private var globals: LuaTable = JsePlatform.standardGlobals()
    private var require: LuaValue = globals!!.get("require")

    @Test
    fun testLoadClass() {
        val result = globals!!.load(org.luaj.vm2.require.RequireSampleSuccess())
        assertEquals("require-sample-success-", result.tojstring())
    }

    @Test
    fun testRequireClassSuccess() {
        var result = require!!.call(LuaValue.valueOf("org.luaj.vm2.require.RequireSampleSuccess"))
        assertEquals("require-sample-success-org.luaj.vm2.require.RequireSampleSuccess", result.tojstring())
        result = require!!.call(LuaValue.valueOf("org.luaj.vm2.require.RequireSampleSuccess"))
        assertEquals("require-sample-success-org.luaj.vm2.require.RequireSampleSuccess", result.tojstring())
    }

    @Test
    fun testRequireClassLoadLuaError() {
        try {
            val result = require!!.call(LuaValue.valueOf(RequireSampleLoadLuaError::class.java.name))
            fail("incorrectly loaded class that threw lua error")
        } catch (le: LuaError) {
            assertEquals(
                "sample-load-lua-error",
                le.message
            )
        }

        try {
            val result = require!!.call(LuaValue.valueOf(RequireSampleLoadLuaError::class.java.name))
            fail("incorrectly loaded class that threw lua error")
        } catch (le: LuaError) {
            assertEquals(
                "loop or previous error loading module '" + RequireSampleLoadLuaError::class.java.name + "'",
                le.message
            )
        }
    }

    @Test
    fun testRequireClassLoadRuntimeException() {
        try {
            val result = require!!.call(LuaValue.valueOf(RequireSampleLoadRuntimeExcep::class.java.name))
            fail("incorrectly loaded class that threw runtime exception")
        } catch (le: RuntimeException) {
            assertEquals(
                "sample-load-runtime-exception",
                le.message
            )
        }

        try {
            val result = require!!.call(LuaValue.valueOf(RequireSampleLoadRuntimeExcep::class.java.name))
            fail("incorrectly loaded class that threw runtime exception")
        } catch (le: LuaError) {
            assertEquals(
                "loop or previous error loading module '" + RequireSampleLoadRuntimeExcep::class.java.name + "'",
                le.message
            )
        }
    }

    @Test
    fun testRequireClassClassCastException() {
        try {
            val result = require!!.call(LuaValue.valueOf(RequireSampleClassCastExcep::class.java.name))
            fail("incorrectly loaded class that threw class cast exception")
        } catch (le: LuaError) {
            val msg = le.message
            if (msg!!.indexOf("not found") < 0)
                fail("expected 'not found' message but got $msg")
        }

        try {
            val result = require!!.call(LuaValue.valueOf(RequireSampleClassCastExcep::class.java.name))
            fail("incorrectly loaded class that threw class cast exception")
        } catch (le: LuaError) {
            val msg = le.message
            if (msg!!.indexOf("not found") < 0)
                fail("expected 'not found' message but got $msg")
        }

    }
}
