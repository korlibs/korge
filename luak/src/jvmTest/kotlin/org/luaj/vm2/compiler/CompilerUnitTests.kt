package org.luaj.vm2.compiler

import kotlin.test.Test


open class CompilerUnitTests : AbstractUnitTests("test/lua", "luaj3.0-tests.zip", "lua5.2.1-tests") {
    @Test
    fun testAll() {
        doTest("all.lua")
    }

    @Test
    fun testApi() {
        doTest("api.lua")
    }

    @Test
    fun testAttrib() {
        doTest("attrib.lua")
    }

    @Test
    fun testBig() {
        doTest("big.lua")
    }

    @Test
    fun testBitwise() {
        doTest("bitwise.lua")
    }

    @Test
    fun testCalls() {
        doTest("calls.lua")
    }

    @Test
    fun testChecktable() {
        doTest("checktable.lua")
    }

    @Test
    fun testClosure() {
        doTest("closure.lua")
    }

    @Test
    fun testCode() {
        doTest("code.lua")
    }

    @Test
    fun testConstruct() {
        doTest("constructs.lua")
    }

    @Test
    fun testCoroutine() {
        doTest("coroutine.lua")
    }

    @Test
    fun testDb() {
        doTest("db.lua")
    }

    @Test
    fun testErrors() {
        doTest("errors.lua")
    }

    @Test
    fun testEvents() {
        doTest("events.lua")
    }

    @Test
    fun testFiles() {
        doTest("files.lua")
    }

    @Test
    fun testGc() {
        doTest("gc.lua")
    }

    @Test
    fun testGoto() {
        doTest("goto.lua")
    }

    @Test
    fun testLiterals() {
        doTest("literals.lua")
    }

    @Test
    fun testLocals() {
        doTest("locals.lua")
    }

    @Test
    fun testMain() {
        doTest("main.lua")
    }

    @Test
    fun testMath() {
        doTest("math.lua")
    }

    @Test
    fun testNextvar() {
        doTest("nextvar.lua")
    }

    @Test
    fun testPm() {
        doTest("pm.lua")
    }

    @Test
    fun testSort() {
        doTest("sort.lua")
    }

    @Test
    fun testStrings() {
        doTest("strings.lua")
    }

    @Test
    fun testVararg() {
        doTest("vararg.lua")
    }

    @Test
    fun testVerybig() {
        doTest("verybig.lua")
    }
}
