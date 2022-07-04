package org.luaj.luajc

import org.luaj.vm2.LuaValue
import org.luaj.vm2.Varargs
import org.luaj.vm2.lib.TwoArgFunction
import org.luaj.vm2.lib.VarArgFunction

// Must have this in the main chunk so it can be loaded and instantiated on all platforms.
class SampleMainChunk : VarArgFunction() {

    internal var rw_ENV: Array<LuaValue> = arrayOf()  // The environment when it is read-write
    //	LuaValue ro_ENV;  // The environment when it is read-only in all sub-functions

    internal var rw_openup1: Array<LuaValue>? =
        null  // upvalue that we create and modify in "slot" 1, passed to sub-function in initer.
    internal var rw_openup2: Array<LuaValue>? =
        null  // array is instantiated on first set or before supply to closure, after that value is get, set.
    internal var rw_openup3: Array<LuaValue>? =
        null  // closing these nulls them out, sub-functions still retain references to array & can use
    internal var ro_openup4: LuaValue? =
        null  // open upvalue that is read-only once it is supplied to an inner function.
    internal var ro_openup5: LuaValue? = null  // closing this also nulls it out.

    fun initupvalue1(v: Array<LuaValue>) {
        this.rw_ENV = v
    }

    override fun invoke(args: Varargs): Varargs {
        rw_ENV[0].get(`$print`).call(`$foo`)

        rw_ENV[0].set(`$print`, InnerFunction(rw_openup3, rw_openup1, ro_openup5))

        return LuaValue.NIL
    }

    internal class InnerFunction(
        val rw_upvalue1: Array<LuaValue>?  // from enclosing function, corresponds to upvaldesc not instack.
        , val rw_upvalue2: Array<LuaValue>?  // from enclosing function, corresponds to upvaldesc not instack.
        , val ro_upvalue3: LuaValue?  // from enclosing function, but read-only everywhere.
    ) : TwoArgFunction() {

        var rw_openup1: Array<LuaValue>? =
            null  // closing these nulls them out, sub-functions still retain references to array & can use
        var ro_openup2: LuaValue? = null  // open upvalue that is read-only once it is supplied to an inner function.

        override fun call(arg1: LuaValue, arg2: LuaValue): LuaValue {
            return LuaValue.NIL
        }

        companion object {
            val `$print`: LuaValue = LuaValue.valueOf("print") // A constant, named for what it is.
            val `$foo`: LuaValue = LuaValue.valueOf("foo")
        }

    }

    companion object {

        internal val `$print`: LuaValue = LuaValue.valueOf("print")
        internal val `$foo`: LuaValue = LuaValue.valueOf("foo")
    }

}
