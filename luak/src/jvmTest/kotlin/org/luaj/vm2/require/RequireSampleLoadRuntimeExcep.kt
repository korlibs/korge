package org.luaj.vm2.require

import org.luaj.vm2.LuaValue
import org.luaj.vm2.lib.ZeroArgFunction

/**
 * This should fail while trying to load via "require()" because it throws a RuntimeException
 *
 */
class RequireSampleLoadRuntimeExcep : ZeroArgFunction() {
    override fun call(): LuaValue = throw RuntimeException("sample-load-runtime-exception")
}
