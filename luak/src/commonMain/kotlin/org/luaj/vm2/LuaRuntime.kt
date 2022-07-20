package org.luaj.vm2

import org.luaj.vm2.internal.*
import kotlin.native.concurrent.*

open class LuaRuntime {
    /** Simple cache of recently created strings that are short.
     * This is simply a list of strings, indexed by their hash codes modulo the cache size
     * that have been recently constructed.  If a string is being constructed frequently
     * from different contexts, it will generally show up as a cache hit and resolve
     * to the same value.   */
    val recent_short_strings = arrayOfNulls<LuaString>(LuaString.RECENT_STRINGS_CACHE_SIZE)

    companion object {
        private val _defaultRuntime get() = LuaRuntime_defaultRuntime
        // Temporal hack to support statics on JVM, but allow to pass initial tests on native
        //val default: LuaRuntime? get() = if (JSystem.supportStatic) _defaultRuntime else null
        val default: LuaRuntime? get() = _defaultRuntime
    }
}

@ThreadLocal
private val LuaRuntime_defaultRuntime by lazy { LuaRuntime() }
