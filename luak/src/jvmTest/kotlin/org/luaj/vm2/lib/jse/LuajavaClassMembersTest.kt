package org.luaj.vm2.lib.jse

import org.luaj.vm2.LuaError
import org.luaj.vm2.LuaValue
import kotlin.test.*

class LuajavaClassMembersTest {
    open class A
    open class B : A {
        @JvmField var m_byte_field: Byte = 0
        @JvmField var m_int_field: Int = 0
        @JvmField var m_double_field: Double = 0.toDouble()
        @JvmField var m_string_field: String? = null

        fun getString(): String = "abc"

        constructor() {}
        constructor(i: Int) {
            m_int_field = i
        }

        fun setString(x: String): String = "setString(String) $x"
        open fun getint(): Int = 100000
        fun uniq(): String = "uniq()"
        fun uniqs(s: String): String = "uniqs(string:$s)"
        fun uniqi(i: Int): String = "uniqi(int:$i)"
        fun uniqsi(s: String, i: Int): String = "uniqsi(string:$s,int:$i)"
        fun uniqis(i: Int, s: String): String = "uniqis(int:$i,string:$s)"
        fun pick(): String = "pick()"
        open fun pick(s: String): String = "pick(string:$s)"
        open fun pick(i: Int): String = "pick(int:$i)"
        fun pick(s: String, i: Int): String = "pick(string:$s,int:$i)"
        fun pick(i: Int, s: String): String = "pick(int:$i,string:$s)"

        companion object {

            @JvmStatic
            fun staticpick(): String = "static-pick()"

            @JvmStatic
            fun staticpick(s: String): String = "static-pick(string:$s)"

            @JvmStatic
            fun staticpick(i: Int): String = "static-pick(int:$i)"

            @JvmStatic
            fun staticpick(s: String, i: Int): String = "static-pick(string:$s,int:$i)"

            @JvmStatic
            fun staticpick(i: Int, s: String): String = "static-pick(int:$i,string:$s)"
        }
    }

    class C : B {
        constructor() {}
        constructor(s: String) {
            m_string_field = s
        }

        constructor(i: Int) {
            m_int_field = i
        }

        constructor(s: String, i: Int) {
            m_string_field = s
            m_int_field = i
        }

        override fun getint(): Int = 200000
        override fun pick(s: String): String = "class-c-pick(string:$s)"
        override fun pick(i: Int): String = "class-c-pick(int:$i)"

        object D {
            @JvmStatic
            fun name(): String = "name-of-D"
        }
    }

    @Test
    fun testSetByteField() {
        val b = B()
        val i = JavaInstance(b)
        i["m_byte_field"] = ONE
        assertEquals(1, b.m_byte_field.toInt())
        assertEquals(ONE, i["m_byte_field"])
        i["m_byte_field"] = PI
        assertEquals(3, b.m_byte_field.toInt())
        assertEquals(THREE, i["m_byte_field"])
        i["m_byte_field"] = ABC
        assertEquals(0, b.m_byte_field.toInt())
        assertEquals(ZERO, i["m_byte_field"])
    }

    @Test
    fun testSetDoubleField() {
        val b = B()
        val i = JavaInstance(b)
        i["m_double_field"] = ONE
        assertEquals(1.0, b.m_double_field)
        assertEquals(ONE, i["m_double_field"])
        i["m_double_field"] = PI
        assertEquals(Math.PI, b.m_double_field)
        assertEquals(PI, i["m_double_field"])
        i["m_double_field"] = ABC
        assertEquals(0.0, b.m_double_field)
        assertEquals(ZERO, i["m_double_field"])
    }

    @Test
    fun testNoFactory() {
        val c = JavaClass.forClass(A::class.java)
        try {
            c.call()
            fail("did not throw lua error as expected")
        } catch (e: LuaError) {
        }

    }

    @Test
    fun testUniqueFactoryCoercible() {
        val c = JavaClass.forClass(B::class.java)
        assertEquals(JavaClass::class.java, c.javaClass)
        val constr = c["new"]
        assertEquals(JavaConstructor.Overload::class.java, constr::class.java)
        val v = constr.call(NUMS)
        val b = v.touserdata()
        assertEquals(B::class.java, b!!::class.java)
        assertEquals(123, (b as B).m_int_field)
        val b0 = constr.call().touserdata()
        assertEquals(B::class.java, b0!!::class.java)
        assertEquals(0, (b0 as B).m_int_field)
    }

    @Test
    fun testUniqueFactoryUncoercible() {
        val f = JavaClass.forClass(B::class.java)
        val constr = f["new"]
        assertEquals(JavaConstructor.Overload::class.java, constr::class.java)
        try {
            val v = constr.call(LuaValue.userdataOf(Any()))
            val b = v.touserdata()
            // fail( "did not throw lua error as expected" );
            assertEquals(0, (b as B).m_int_field)
        } catch (e: LuaError) {
        }

    }

    @Test
    fun testOverloadedFactoryCoercible() {
        val f = JavaClass.forClass(C::class.java)
        val constr = f["new"]
        assertEquals(JavaConstructor.Overload::class.java, constr::class.java)
        val c = constr.call().touserdata()
        val ci = constr.call(LuaValue.valueOf(123)).touserdata()
        val cs = constr.call(LuaValue.valueOf("abc")).touserdata()
        val csi = constr.call(LuaValue.valueOf("def"), LuaValue.valueOf(456)).touserdata()
        assertEquals(C::class.java, c!!::class.java)
        assertEquals(C::class.java, ci!!::class.java)
        assertEquals(C::class.java, cs!!::class.java)
        assertEquals(C::class.java, csi!!::class.java)
        assertEquals(null, (c as C).m_string_field)
        assertEquals(0, c.m_int_field)
        assertEquals("abc", (cs as C).m_string_field)
        assertEquals(0, cs.m_int_field)
        assertEquals(null, (ci as C).m_string_field)
        assertEquals(123, ci.m_int_field)
        assertEquals("def", (csi as C).m_string_field)
        assertEquals(456, csi.m_int_field)
    }

    @Test
    fun testOverloadedFactoryUncoercible() {
        val f = JavaClass.forClass(C::class.java)
        try {
            val c = f.call(LuaValue.userdataOf(Any()))
            // fail( "did not throw lua error as expected" );
            assertEquals(0, (c as C).m_int_field)
            assertEquals(null, (c as C).m_string_field)
        } catch (e: LuaError) {
        }

    }

    @Test
    fun testNoAttribute() {
        val f = JavaClass.forClass(A::class.java)
        val v = f["bogus"]
        assertEquals(v, LuaValue.NIL)
        try {
            f["bogus"] = ONE
            fail("did not throw lua error as expected")
        } catch (e: LuaError) {
        }

    }

    @Test
    fun testFieldAttributeCoercible() {
        var i = JavaInstance(B())
        i["m_int_field"] = ONE
        assertEquals(1, i["m_int_field"].toint())
        i["m_int_field"] = THREE
        assertEquals(3, i["m_int_field"].toint())
        i = JavaInstance(C())
        i["m_int_field"] = ONE
        assertEquals(1, i["m_int_field"].toint())
        i["m_int_field"] = THREE
        assertEquals(3, i["m_int_field"].toint())
    }

    @Test
    fun testUniqueMethodAttributeCoercible() {
        val b = B()
        val ib = JavaInstance(b)
        val b_getString = ib["getString"]
        val b_getint = ib["getint"]
        assertEquals(JavaMethod::class.java, b_getString::class.java)
        assertEquals(JavaMethod::class.java, b_getint::class.java)
        assertEquals("abc", b_getString.call(SOMEB).tojstring())
        assertEquals(100000, b_getint.call(SOMEB).toint())
        assertEquals("abc", b_getString.call(SOMEC).tojstring())
        assertEquals(200000, b_getint.call(SOMEC).toint())
    }

    @Test
    fun testUniqueMethodAttributeArgsCoercible() {
        val b = B()
        val ib = JavaInstance(b)
        val uniq = ib["uniq"]
        val uniqs = ib["uniqs"]
        val uniqi = ib["uniqi"]
        val uniqsi = ib["uniqsi"]
        val uniqis = ib["uniqis"]
        assertEquals(JavaMethod::class.java, uniq::class.java)
        assertEquals(JavaMethod::class.java, uniqs::class.java)
        assertEquals(JavaMethod::class.java, uniqi::class.java)
        assertEquals(JavaMethod::class.java, uniqsi::class.java)
        assertEquals(JavaMethod::class.java, uniqis::class.java)
        assertEquals("uniq()", uniq.call(SOMEB).tojstring())
        assertEquals("uniqs(string:abc)", uniqs.call(SOMEB, ABC).tojstring())
        assertEquals("uniqi(int:1)", uniqi.call(SOMEB, ONE).tojstring())
        assertEquals("uniqsi(string:abc,int:1)", uniqsi.call(SOMEB, ABC, ONE).tojstring())
        assertEquals("uniqis(int:1,string:abc)", uniqis.call(SOMEB, ONE, ABC).tojstring())
        assertEquals(
            "uniqis(int:1,string:abc)",
            uniqis.invoke(LuaValue.varargsOf(arrayOf(SOMEB, ONE, ABC, ONE))).arg1().tojstring()
        )
    }

    @Test
    fun testOverloadedMethodAttributeCoercible() {
        val b = B()
        val ib = JavaInstance(b)
        val p = ib["pick"]
        assertEquals("pick()", p.call(SOMEB).tojstring())
        assertEquals("pick(string:abc)", p.call(SOMEB, ABC).tojstring())
        assertEquals("pick(int:1)", p.call(SOMEB, ONE).tojstring())
        assertEquals("pick(string:abc,int:1)", p.call(SOMEB, ABC, ONE).tojstring())
        assertEquals("pick(int:1,string:abc)", p.call(SOMEB, ONE, ABC).tojstring())
        assertEquals(
            "pick(int:1,string:abc)",
            p.invoke(LuaValue.varargsOf(arrayOf(SOMEB, ONE, ABC, ONE))).arg1().tojstring()
        )
    }

    @Test
    fun testUnboundOverloadedMethodAttributeCoercible() {
        val b = B()
        val ib = JavaInstance(b)
        val p = ib["pick"]
        assertEquals(JavaMethod.Overload::class.java, p::class.java)
        assertEquals("pick()", p.call(SOMEC).tojstring())
        assertEquals("class-c-pick(string:abc)", p.call(SOMEC, ABC).tojstring())
        assertEquals("class-c-pick(int:1)", p.call(SOMEC, ONE).tojstring())
        assertEquals("pick(string:abc,int:1)", p.call(SOMEC, ABC, ONE).tojstring())
        assertEquals("pick(int:1,string:abc)", p.call(SOMEC, ONE, ABC).tojstring())
        assertEquals(
            "pick(int:1,string:abc)",
            p.invoke(LuaValue.varargsOf(arrayOf(SOMEC, ONE, ABC, ONE))).arg1().tojstring()
        )
    }

    @Test
    fun testOverloadedStaticMethodAttributeCoercible() {
        val b = B()
        val ib = JavaInstance(b)
        val p = ib["staticpick"]
        assertEquals("static-pick()", p.call(SOMEB).tojstring())
        assertEquals("static-pick(string:abc)", p.call(SOMEB, ABC).tojstring())
        assertEquals("static-pick(int:1)", p.call(SOMEB, ONE).tojstring())
        assertEquals("static-pick(string:abc,int:1)", p.call(SOMEB, ABC, ONE).tojstring())
        assertEquals("static-pick(int:1,string:abc)", p.call(SOMEB, ONE, ABC).tojstring())
        assertEquals(
            "static-pick(int:1,string:abc)",
            p.invoke(LuaValue.varargsOf(arrayOf(SOMEB, ONE, ABC, ONE))).arg1().tojstring()
        )
    }

    @Test
    fun testGetInnerClass() {
        val c = C()
        val ic = JavaInstance(c)
        val d = ic["D"]
        assertFalse(d.isnil())
        assertSame(d, JavaClass.forClass(C.D::class.java))
        val e = ic["E"]
        assertTrue(e.isnil())
    }

    companion object {

        internal var ZERO: LuaValue = LuaValue.ZERO
        internal var ONE: LuaValue = LuaValue.ONE
        internal var PI: LuaValue = LuaValue.valueOf(Math.PI)
        internal var THREE: LuaValue = LuaValue.valueOf(3)
        internal var NUMS: LuaValue = LuaValue.valueOf(123)
        internal var ABC: LuaValue = LuaValue.valueOf("abc")
        internal var SOMEA = CoerceJavaToLua.coerce(A())
        internal var SOMEB = CoerceJavaToLua.coerce(B())
        internal var SOMEC = CoerceJavaToLua.coerce(C())
    }
}
