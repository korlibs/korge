package org.luaj.vm2.lib.common

import org.luaj.vm2.*
import org.luaj.vm2.compiler.*
import org.luaj.vm2.lib.*

open class CommonPlatform {
    companion object : CommonPlatform()

    /**
     * Create a standard set of globals for JSE including all the libraries.
     *
     * @return Table of globals initialized with the standard JSE libraries
     * @see .debugGlobals
     * @see JsePlatform
     * //@see org.luaj.vm2.lib.jme.JmePlatform
     */
    open fun standardGlobals(): Globals {
        val globals = Globals()
        globals.load(BaseLib())
        globals.load(PackageLib())
        globals.load(Bit32Lib())
        globals.load(TableLib())
        globals.load(StringLib())
        globals.load(CoroutineLib())
        globals.load(MathLib())
        //globals.load(IoLib())
        globals.load(OsLib())
        LoadState.install(globals)
        LuaC.install(globals)
        return globals
    }

    /** Create standard globals including the [DebugLib] library.
     *
     * @return Table of globals initialized with the standard JSE and debug libraries
     * @see .standardGlobals
     * @see JsePlatform
     * //@see org.luaj.vm2.lib.jme.JmePlatform
     *
     * @see DebugLib
     */
    fun debugGlobals(): Globals {
        val globals = standardGlobals()
        globals.load(DebugLib())
        return globals
    }


    /** Simple wrapper for invoking a lua function with command line arguments.
     * The supplied function is first given a new Globals object,
     * then the program is run with arguments.
     */
    fun luaMain(mainChunk: LuaValue, args: Array<String>) {
        val g = standardGlobals()
        val vargs = Array<LuaValue>(args.size) { LuaValue.valueOf(args[it]) }
        val arg = LuaValue.listOf(vargs)
        arg["n"] = args.size
        g["arg"] = arg
        mainChunk.initupvalue1(g)
        mainChunk.invoke(LuaValue.varargsOf(vargs))
    }
}
