package org.luaj.vm2.require

import org.luaj.vm2.LuaValue
import org.luaj.vm2.lib.TwoArgFunction

/**
 * This should succeed as a library that can be loaded dynamically via "require()"
 */
class RequireSampleSuccess : TwoArgFunction() {

    override fun call(modname: LuaValue, env: LuaValue): LuaValue {
        env.checkglobals()
        return LuaValue.valueOf("require-sample-success-" + modname.tojstring())
    }
}
