package org.luaj.vm2.require

import org.luaj.vm2.LuaValue

/**
 * This should fail while trying to load via "require() because it is not a LibFunction"
 *
 */
class RequireSampleClassCastExcep {
    fun call(): LuaValue = LuaValue.valueOf("require-sample-class-cast-excep")
}
