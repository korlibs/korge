package org.luaj.vm2

import org.luaj.test.*
import org.luaj.vm2.parser.*
import kotlin.test.*

class ParserTest {
    @Test
    fun test() {
        LuaParser(AllLua).Block()
    }
}
