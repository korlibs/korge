package org.luaj.vm2.lib.jse

import org.luaj.vm2.*
import kotlin.test.*

class LuaJavaCoercionTest {

    private var globals: LuaValue = JsePlatform.standardGlobals()

    @Test
    fun testJavaIntToLuaInt() {
        val i = Integer.valueOf(777)
        val v = CoerceJavaToLua.coerce(i)
        assertEquals(LuaInteger::class.java, v::class.java)
        assertEquals(777, v.toint())
    }

    @Test
    fun testLuaIntToJavaInt() {
        val i = LuaInteger.valueOf(777)
        var o = CoerceLuaToJava.coerce(i, Int::class.javaObjectType)!!
        assertEquals(Int::class.javaObjectType, o::class.java)
        assertEquals(777, (o as Number).toInt())
        o = CoerceLuaToJava.coerce(i, Int::class.java)!!
        assertEquals(Int::class.javaObjectType, o::class.java)
        assertEquals(777, o)
    }

    @Test
    fun testJavaStringToLuaString() {
        val s = ("777")
        val v = CoerceJavaToLua.coerce(s)
        assertEquals(LuaString::class.java, v::class.java)
        assertEquals("777", v.toString())
    }

    @Test
    fun testLuaStringToJavaString() {
        val s = LuaValue.valueOf("777")
        val o = CoerceLuaToJava.coerce(s, String::class.java)!!
        assertEquals(String::class.java, o::class.java)
        assertEquals("777", o)
    }

    @Test
    fun testJavaClassToLuaUserdata() {
        val va = CoerceJavaToLua.coerce(ClassA::class.java)
        val va1 = CoerceJavaToLua.coerce(ClassA::class.java)
        val vb = CoerceJavaToLua.coerce(ClassB::class.java)
        assertSame(va, va1)
        assertNotSame(va, vb)
        val vi = CoerceJavaToLua.coerce(ClassA())
        assertNotSame(va, vi)
        assertTrue(vi.isuserdata())
        assertTrue(vi.isuserdata(ClassA::class.java))
        assertFalse(vi.isuserdata(ClassB::class.java))
        val vj = CoerceJavaToLua.coerce(ClassB())
        assertNotSame(vb, vj)
        assertTrue(vj.isuserdata())
        assertFalse(vj.isuserdata(ClassA::class.java))
        assertTrue(vj.isuserdata(ClassB::class.java))
    }

    internal class ClassA

    internal class ClassB

    @Test
    fun testJavaIntArrayToLuaTable() {
        val i = intArrayOf(222, 333)
        val v = CoerceJavaToLua.coerce(i)
        assertEquals(JavaArray::class.java, v::class.java)
        assertEquals(LuaInteger.valueOf(222), v.get(ONE))
        assertEquals(LuaInteger.valueOf(333), v.get(TWO))
        assertEquals(TWO, v.get(LENGTH))
        assertEquals(LuaValue.NIL, v.get(THREE))
        assertEquals(LuaValue.NIL, v.get(ZERO))
        v.set(ONE, LuaInteger.valueOf(444))
        v.set(TWO, LuaInteger.valueOf(555))
        assertEquals(444, i[0])
        assertEquals(555, i[1])
        assertEquals(LuaInteger.valueOf(444), v.get(ONE))
        assertEquals(LuaInteger.valueOf(555), v.get(TWO))
        try {
            v[ZERO] = LuaInteger.valueOf(777)
            fail("array bound exception not thrown")
        } catch (lee: LuaError) {
            // expected
        }

        try {
            v[THREE] = LuaInteger.valueOf(777)
            fail("array bound exception not thrown")
        } catch (lee: LuaError) {
            // expected
        }

    }

    @Test
    fun testLuaTableToJavaIntArray() {
        val t = LuaTable()
        t.set(1, LuaInteger.valueOf(222))
        t.set(2, LuaInteger.valueOf(333))
        var i: IntArray? = null
        val o = CoerceLuaToJava.coerce(t, IntArray::class.java)!!
        assertEquals(IntArray::class.java, o::class.java)
        i = o as IntArray
        assertEquals(2, i.size)
        assertEquals(222, i[0])
        assertEquals(333, i[1])
    }

    @Test
    fun testIntArrayScoringTables() {
        val a = 5
        val la = LuaInteger.valueOf(a)
        val tb = LuaTable()
        tb.set(1, la)
        val tc = LuaTable()
        tc.set(1, tb)

        val saa = CoerceLuaToJava.getCoercion(Int::class.javaObjectType).score(la)
        val sab = CoerceLuaToJava.getCoercion(IntArray::class.java).score(la)
        val sac = CoerceLuaToJava.getCoercion(Array<IntArray>::class.java).score(la)
        assertTrue(saa < sab)
        assertTrue(saa < sac)
        val sba = CoerceLuaToJava.getCoercion(Int::class.javaObjectType).score(tb)
        val sbb = CoerceLuaToJava.getCoercion(IntArray::class.java).score(tb)
        val sbc = CoerceLuaToJava.getCoercion(Array<IntArray>::class.java).score(tb)
        assertTrue(sbb < sba)
        assertTrue(sbb < sbc)
        val sca = CoerceLuaToJava.getCoercion(Int::class.javaObjectType).score(tc)
        val scb = CoerceLuaToJava.getCoercion(IntArray::class.java).score(tc)
        val scc = CoerceLuaToJava.getCoercion(Array<IntArray>::class.java).score(tc)
        assertTrue(scc < sca)
        assertTrue(scc < scb)
    }

    @Test
    fun testIntArrayScoringUserdata() {
        val a = 5
        val b = intArrayOf(44, 66)
        val c = arrayOf(intArrayOf(11, 22), intArrayOf(33, 44))
        val va = CoerceJavaToLua.coerce(a)
        val vb = CoerceJavaToLua.coerce(b)
        val vc = CoerceJavaToLua.coerce(c)

        val vaa = CoerceLuaToJava.getCoercion(Int::class.javaObjectType).score(va)
        val vab = CoerceLuaToJava.getCoercion(IntArray::class.java).score(va)
        val vac = CoerceLuaToJava.getCoercion(Array<IntArray>::class.java).score(va)
        assertTrue(vaa < vab)
        assertTrue(vaa < vac)
        val vba = CoerceLuaToJava.getCoercion(Int::class.javaObjectType).score(vb)
        val vbb = CoerceLuaToJava.getCoercion(IntArray::class.java).score(vb)
        val vbc = CoerceLuaToJava.getCoercion(Array<IntArray>::class.java).score(vb)
        assertTrue(vbb < vba)
        assertTrue(vbb < vbc)
        val vca = CoerceLuaToJava.getCoercion(Int::class.javaObjectType).score(vc)
        val vcb = CoerceLuaToJava.getCoercion(IntArray::class.java).score(vc)
        val vcc = CoerceLuaToJava.getCoercion(Array<IntArray>::class.java).score(vc)
        assertTrue(vcc < vca)
        assertTrue(vcc < vcb)
    }

    class SampleClass {
        fun sample(): String = "void-args"
        fun sample(a: Int): String = "int-args $a"
        fun sample(a: IntArray): String = "int-array-args " + a[0] + "," + a[1]
        fun sample(a: Array<IntArray>): String =
            "int-array-array-args " + a[0][0] + "," + a[0][1] + "," + a[1][0] + "," + a[1][1]
    }

    @Test
    fun testMatchVoidArgs() {
        val v = CoerceJavaToLua.coerce(SampleClass())
        val result = v.method("sample")
        assertEquals("void-args", result.toString())
    }

    @Test
    fun testMatchIntArgs() {
        val v = CoerceJavaToLua.coerce(SampleClass())
        val arg = CoerceJavaToLua.coerce(123)
        val result = v.method("sample", arg)
        assertEquals("int-args 123", result.toString())
    }

    @Test
    fun testMatchIntArrayArgs() {
        val v = CoerceJavaToLua.coerce(SampleClass())
        val arg = CoerceJavaToLua.coerce(intArrayOf(345, 678))
        val result = v.method("sample", arg)
        assertEquals("int-array-args 345,678", result.toString())
    }

    @Test
    fun testMatchIntArrayArrayArgs() {
        val v = CoerceJavaToLua.coerce(SampleClass())
        val arg = CoerceJavaToLua.coerce(arrayOf(intArrayOf(22, 33), intArrayOf(44, 55)))
        val result = v.method("sample", arg)
        assertEquals("int-array-array-args 22,33,44,55", result.toString())
    }

    class SomeException(message: String) : RuntimeException(message)

    object SomeClass {
        @JvmStatic
        fun someMethod() {
            throw SomeException("this is some message")
        }
    }

    @Test
    fun testExceptionMessage() {
        val script = "local c = luajava.bindClass( \"" + SomeClass::class.java.name + "\" )\n" +
                "return pcall( c.someMethod, c )"
        val vresult = globals!!.get("load").call(LuaValue.valueOf(script)).invoke(LuaValue.NONE)
        val status = vresult.arg1()
        val message = vresult.arg(2)
        assertEquals(LuaValue.BFALSE, status)
        val index = message.toString().indexOf("this is some message")
        assertTrue(index >= 0, "bad message: $message")
    }

    @Test
    fun testLuaErrorCause() {
        val script = "luajava.bindClass( \"" + SomeClass::class.java.name + "\"):someMethod()"
        val chunk = globals!!.get("load").call(LuaValue.valueOf(script))
        try {
            chunk.invoke(LuaValue.NONE)
            fail("call should not have succeeded")
        } catch (lee: LuaError) {
            val c = lee.getLuaCause()!!
            assertEquals(SomeException::class.java, c::class.java)
        }

    }

    interface VarArgsInterface {
        fun varargsMethod(a: String, vararg v: String): String
        fun arrayargsMethod(a: String, v: Array<String>?): String
    }

    @Test
    fun testVarArgsProxy() {
        val script = "return luajava.createProxy( \"" + VarArgsInterface::class.java.name + "\", \n" +
                "{\n" +
                "	varargsMethod = function(a,...)\n" +
                "		return table.concat({a,...},'-')\n" +
                "	end,\n" +
                "	arrayargsMethod = function(a,array)\n" +
                "		return tostring(a)..(array and \n" +
                "			('-'..tostring(array.length)\n" +
                "			..'-'..tostring(array[1])\n" +
                "			..'-'..tostring(array[2])\n" +
                "			) or '-nil')\n" +
                "	end,\n" +
                "} )\n"
        val chunk = globals!!.get("load").call(LuaValue.valueOf(script))
        if (!chunk.arg1().toboolean())
            fail(chunk.arg(2).toString())
        val result = chunk.arg1().call()
        val u = result.touserdata()
        val v = u as VarArgsInterface
        assertEquals("foo", v.varargsMethod("foo"))
        assertEquals("foo-bar", v.varargsMethod("foo", "bar"))
        assertEquals("foo-bar-etc", v.varargsMethod("foo", "bar", "etc"))
        assertEquals("foo-0-nil-nil", v.arrayargsMethod("foo", arrayOf()))
        assertEquals("foo-1-bar-nil", v.arrayargsMethod("foo", arrayOf("bar")))
        assertEquals("foo-2-bar-etc", v.arrayargsMethod("foo", arrayOf("bar", "etc")))
        assertEquals("foo-3-bar-etc", v.arrayargsMethod("foo", arrayOf("bar", "etc", "etc")))
        assertEquals("foo-nil", v.arrayargsMethod("foo", null))
    }

    @Test
    fun testBigNum() {
        val script = "bigNumA = luajava.newInstance('java.math.BigDecimal','12345678901234567890');\n" +
                "bigNumB = luajava.newInstance('java.math.BigDecimal','12345678901234567890');\n" +
                "bigNumC = bigNumA:multiply(bigNumB);\n" +
                //"print(bigNumA:toString())\n" +
                //"print(bigNumB:toString())\n" +
                //"print(bigNumC:toString())\n" +
                "return bigNumA:toString(), bigNumB:toString(), bigNumC:toString()"
        val chunk = globals!!.get("load").call(LuaValue.valueOf(script))
        if (!chunk.arg1().toboolean())
            fail(chunk.arg(2).toString())
        val results = chunk.arg1().invoke()
        val nresults = results.narg()
        val sa = results.tojstring(1)
        val sb = results.tojstring(2)
        val sc = results.tojstring(3)
        assertEquals(3, nresults)
        assertEquals("12345678901234567890", sa)
        assertEquals("12345678901234567890", sb)
        assertEquals("152415787532388367501905199875019052100", sc)
    }

    interface IA
    interface IB : IA
    interface IC : IB

    open class A : IA
    open class B : A(), IB {

        val `object`: Any get() = Any()
        val string: String get() = "abc"
        val a: A get() = A()
        val b: B get() = B()
        val c: C get() = C()

        fun set(x: Any): String = "set(Object) "
        fun set(x: String): String = "set(String) $x"
        fun set(x: A): String = "set(A) "
        fun set(x: B): String = "set(B) "
        fun set(x: C): String = "set(C) "
        fun set(x: Byte): String = "set(byte) $x"
        fun set(x: Char): String = "set(char) " + x.toInt()
        fun set(x: Short): String = "set(short) $x"
        fun set(x: Int): String = "set(int) $x"
        fun set(x: Long): String = "set(long) $x"
        fun set(x: Float): String = "set(float) $x"
        fun set(x: Double): String = "set(double) $x"
        fun setr(x: Double): String = "setr(double) $x"
        fun setr(x: Float): String = "setr(float) $x"
        fun setr(x: Long): String = "setr(long) $x"
        fun setr(x: Int): String = "setr(int) $x"
        fun setr(x: Short): String = "setr(short) $x"
        fun setr(x: Char): String = "setr(char) " + x.toInt()
        fun setr(x: Byte): String = "setr(byte) $x"
        fun setr(x: C): String = "setr(C) "
        fun setr(x: B): String = "setr(B) "
        fun setr(x: A): String = "setr(A) "
        fun setr(x: String): String = "setr(String) $x"
        fun setr(x: Any): String = "setr(Object) "
        fun getbytearray(): ByteArray = byteArrayOf(1, 2, 3)
        fun getbyte(): Byte = 1
        fun getchar(): Char = 65000.toChar()
        fun getshort(): Short = -32000
        fun getint(): Int = 100000
        fun getlong(): Long = 50000000000L
        fun getfloat(): Float = 6.5f
        fun getdouble(): Double = Math.PI
    }

    open class C : B(), IC
    class D : C(), IA

    @Test
    fun testOverloadedJavaMethodObject() {
        doOverloadedMethodTest("Object", "")
    }

    @Test
    fun testOverloadedJavaMethodString() {
        doOverloadedMethodTest("String", "abc")
    }

    @Test
    fun testOverloadedJavaMethodA() {
        doOverloadedMethodTest("A", "")
    }

    @Test
    fun testOverloadedJavaMethodB() {
        doOverloadedMethodTest("B", "")
    }

    @Test
    fun testOverloadedJavaMethodC() {
        doOverloadedMethodTest("C", "")
    }

    @Test
    fun testOverloadedJavaMethodByte() {
        doOverloadedMethodTest("byte", "1")
    }

    @Test
    fun testOverloadedJavaMethodChar() {
        doOverloadedMethodTest("char", "65000")
    }

    @Test
    fun testOverloadedJavaMethodShort() {
        doOverloadedMethodTest("short", "-32000")
    }

    @Test
    fun testOverloadedJavaMethodInt() {
        doOverloadedMethodTest("int", "100000")
    }

    @Test
    fun testOverloadedJavaMethodLong() {
        doOverloadedMethodTest("long", "50000000000")
    }

    @Test
    fun testOverloadedJavaMethodFloat() {
        doOverloadedMethodTest("float", "6.5")
    }

    @Test
    fun testOverloadedJavaMethodDouble() {
        doOverloadedMethodTest("double", "3.141592653589793")
    }

    private fun doOverloadedMethodTest(typename: String, value: String) {
        val script = "local a = luajava.newInstance('" + B::class.java.name + "');\n" +
                "local b = a:set(a:get" + typename + "())\n" +
                "local c = a:setr(a:get" + typename + "())\n" +
                "return b,c"
        val chunk = globals!!.get("load").call(LuaValue.valueOf(script))
        if (!chunk.arg1().toboolean())
            fail(chunk.arg(2).toString())
        val results = chunk.arg1().invoke()
        val nresults = results.narg()
        assertEquals(2, nresults)
        val b = results.arg(1)
        val c = results.arg(2)
        val sb = b.tojstring()
        val sc = c.tojstring()
        assertEquals("set($typename) $value", sb)
        assertEquals("setr($typename) $value", sc)
    }

    @Test
    fun testClassInheritanceLevels() {
        assertEquals(0, CoerceLuaToJava.inheritanceLevels(Any::class.java, Any::class.java))
        assertEquals(1, CoerceLuaToJava.inheritanceLevels(Any::class.java, String::class.java))
        assertEquals(1, CoerceLuaToJava.inheritanceLevels(Any::class.java, A::class.java))
        assertEquals(2, CoerceLuaToJava.inheritanceLevels(Any::class.java, B::class.java))
        assertEquals(3, CoerceLuaToJava.inheritanceLevels(Any::class.java, C::class.java))

        assertEquals(
            CoerceLuaToJava.SCORE_UNCOERCIBLE,
            CoerceLuaToJava.inheritanceLevels(A::class.java, Any::class.java)
        )
        assertEquals(
            CoerceLuaToJava.SCORE_UNCOERCIBLE,
            CoerceLuaToJava.inheritanceLevels(A::class.java, String::class.java)
        )
        assertEquals(0, CoerceLuaToJava.inheritanceLevels(A::class.java, A::class.java))
        assertEquals(1, CoerceLuaToJava.inheritanceLevels(A::class.java, B::class.java))
        assertEquals(2, CoerceLuaToJava.inheritanceLevels(A::class.java, C::class.java))

        assertEquals(
            CoerceLuaToJava.SCORE_UNCOERCIBLE,
            CoerceLuaToJava.inheritanceLevels(B::class.java, Any::class.java)
        )
        assertEquals(
            CoerceLuaToJava.SCORE_UNCOERCIBLE,
            CoerceLuaToJava.inheritanceLevels(B::class.java, String::class.java)
        )
        assertEquals(
            CoerceLuaToJava.SCORE_UNCOERCIBLE,
            CoerceLuaToJava.inheritanceLevels(B::class.java, A::class.java)
        )
        assertEquals(0, CoerceLuaToJava.inheritanceLevels(B::class.java, B::class.java))
        assertEquals(1, CoerceLuaToJava.inheritanceLevels(B::class.java, C::class.java))

        assertEquals(
            CoerceLuaToJava.SCORE_UNCOERCIBLE,
            CoerceLuaToJava.inheritanceLevels(C::class.java, Any::class.java)
        )
        assertEquals(
            CoerceLuaToJava.SCORE_UNCOERCIBLE,
            CoerceLuaToJava.inheritanceLevels(C::class.java, String::class.java)
        )
        assertEquals(
            CoerceLuaToJava.SCORE_UNCOERCIBLE,
            CoerceLuaToJava.inheritanceLevels(C::class.java, A::class.java)
        )
        assertEquals(
            CoerceLuaToJava.SCORE_UNCOERCIBLE,
            CoerceLuaToJava.inheritanceLevels(C::class.java, B::class.java)
        )
        assertEquals(0, CoerceLuaToJava.inheritanceLevels(C::class.java, C::class.java))
    }

    @Test
    fun testInterfaceInheritanceLevels() {
        assertEquals(1, CoerceLuaToJava.inheritanceLevels(IA::class.java, A::class.java))
        assertEquals(1, CoerceLuaToJava.inheritanceLevels(IB::class.java, B::class.java))
        assertEquals(2, CoerceLuaToJava.inheritanceLevels(IA::class.java, B::class.java))
        assertEquals(1, CoerceLuaToJava.inheritanceLevels(IC::class.java, C::class.java))
        assertEquals(2, CoerceLuaToJava.inheritanceLevels(IB::class.java, C::class.java))
        assertEquals(3, CoerceLuaToJava.inheritanceLevels(IA::class.java, C::class.java))
        assertEquals(1, CoerceLuaToJava.inheritanceLevels(IA::class.java, D::class.java))
        assertEquals(2, CoerceLuaToJava.inheritanceLevels(IC::class.java, D::class.java))
        assertEquals(3, CoerceLuaToJava.inheritanceLevels(IB::class.java, D::class.java))

        assertEquals(
            CoerceLuaToJava.SCORE_UNCOERCIBLE,
            CoerceLuaToJava.inheritanceLevels(IB::class.java, A::class.java)
        )
        assertEquals(
            CoerceLuaToJava.SCORE_UNCOERCIBLE,
            CoerceLuaToJava.inheritanceLevels(IC::class.java, A::class.java)
        )
        assertEquals(
            CoerceLuaToJava.SCORE_UNCOERCIBLE,
            CoerceLuaToJava.inheritanceLevels(IC::class.java, B::class.java)
        )
        assertEquals(
            CoerceLuaToJava.SCORE_UNCOERCIBLE,
            CoerceLuaToJava.inheritanceLevels(IB::class.java, IA::class.java)
        )
        assertEquals(1, CoerceLuaToJava.inheritanceLevels(IA::class.java, IB::class.java))
    }

    @Test
    fun testCoerceJavaToLuaLuaValue() {
        assertSame(LuaValue.NIL, CoerceJavaToLua.coerce(LuaValue.NIL))
        assertSame(LuaValue.ZERO, CoerceJavaToLua.coerce(LuaValue.ZERO))
        assertSame(LuaValue.ONE, CoerceJavaToLua.coerce(LuaValue.ONE))
        assertSame(LuaValue.INDEX, CoerceJavaToLua.coerce(LuaValue.INDEX))
        val table = LuaValue.tableOf()
        assertSame(table, CoerceJavaToLua.coerce(table))
    }

    @Test
    fun testCoerceJavaToLuaByeArray() {
        val bytes = "abcd".toByteArray()
        val value = CoerceJavaToLua.coerce(bytes)
        assertEquals(LuaString::class.java, value::class.java)
        assertEquals(LuaValue.valueOf("abcd"), value)
    }

    companion object {
        private val ZERO = LuaValue.ZERO
        private val ONE = LuaValue.ONE
        private val TWO = LuaValue.valueOf(2)
        private val THREE = LuaValue.valueOf(3)
        private val LENGTH = LuaString.valueOf("length")
    }
}

