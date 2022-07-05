package org.luaj.vm2.lib.jse

import org.luaj.vm2.*
import kotlin.test.*

class LuajavaAccessibleMembersTest {

    private var globals: Globals = JsePlatform.standardGlobals()

    private fun invokeScript(script: String): String {
        try {
            val c = globals!!.load(script, "script")
            return c.call().tojstring()
        } catch (e: Exception) {
            fail("exception: $e")
            return "failed"
        }

    }

    @Test
    fun testAccessFromPrivateClassImplementedMethod() {
        assertEquals(
            "privateImpl-aaa-interface_method(bar)", invokeScript(
                "b = luajava.newInstance('" + TestClass::class.java.name + "');" +
                    "a = b:create_PrivateImpl('aaa');" +
                    "return a:interface_method('bar');"
            )
        )
    }

    @Test
    fun testAccessFromPrivateClassPublicMethod() {
        assertEquals(
            "privateImpl-aaa-public_method", invokeScript(
                "b = luajava.newInstance('" + TestClass::class.java.name + "');" +
                    "a = b:create_PrivateImpl('aaa');" +
                    "return a:public_method();"
            )
        )
    }

    @Test
    fun testAccessFromPrivateClassGetPublicField() {
        assertEquals(
            "aaa", invokeScript(
                "b = luajava.newInstance('" + TestClass::class.java.name + "');" +
                    "a = b:create_PrivateImpl('aaa');" +
                    "return a.public_field;"
            )
        )
    }

    @Test
    fun testAccessFromPrivateClassSetPublicField() {
        assertEquals(
            "foo", invokeScript(
                "b = luajava.newInstance('" + TestClass::class.java.name + "');" +
                    "a = b:create_PrivateImpl('aaa');" +
                    "a.public_field = 'foo';" +
                    "return a.public_field;"
            )
        )
    }

    @Test
    fun testAccessFromPrivateClassPublicConstructor() {
        assertEquals(
            "privateImpl-constructor", invokeScript(
                "b = luajava.newInstance('" + TestClass::class.java.name + "');" +
                    "c = b:get_PrivateImplClass();" +
                    "return luajava.new(c);"
            )
        )
    }

    @Test
    fun testAccessPublicEnum() {
        assertEquals(
            "class org.luaj.vm2.lib.jse.TestClass\$SomeEnum", invokeScript(
                "b = luajava.newInstance('" + TestClass::class.java.name + "');" +
                    "return b.SomeEnum"
            )
        )
    }
}
