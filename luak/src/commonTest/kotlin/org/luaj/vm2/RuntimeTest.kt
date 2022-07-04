package org.luaj.vm2

import org.luaj.vm2.compiler.*
import org.luaj.vm2.lib.*
import kotlin.test.*

class RuntimeTest {
    @Test
    fun test() {
        val globals = createLuaGlobals()
        assertEquals(3, globals.load("return 1 + 2").call().toint())
    }

    fun createLuaGlobals(): Globals = Globals().apply {
        load(BaseLib())
        load(PackageLib())
        load(Bit32Lib())
        load(TableLib())
        load(StringLib()) // Mutates metatable
        load(CoroutineLib())
        LoadState.install(this)
        LuaC.install(this)
    }

}
