package org.luaj.vm2

import org.luaj.vm2.LuaTable.Slot

internal class NonTableMetatable(private val value: LuaValue) : Metatable {
    override fun useWeakKeys(): Boolean = false
    override fun useWeakValues(): Boolean = false
    override fun toLuaValue(): LuaValue = value
    override fun entry(key: LuaValue, value: LuaValue): Slot = LuaTable.defaultEntry(key, value)
    override fun wrap(value: LuaValue): LuaValue = value
    override fun arrayget(array: Array<LuaValue?>, index: Int): LuaValue? = array[index]
}
