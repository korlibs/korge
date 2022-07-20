package samples

import com.soywiz.korge.scene.Scene
import com.soywiz.korge.ui.uiVerticalStack
import com.soywiz.korge.view.SContainer
import com.soywiz.korge.view.text
import com.soywiz.korge.view.textOld
import com.soywiz.korge.view.xy
import org.luaj.vm2.Globals
import org.luaj.vm2.LoadState
import org.luaj.vm2.LuaValue
import org.luaj.vm2.Varargs
import org.luaj.vm2.compiler.LuaC
import org.luaj.vm2.lib.BaseLib
import org.luaj.vm2.lib.Bit32Lib
import org.luaj.vm2.lib.CoroutineLib
import org.luaj.vm2.lib.PackageLib
import org.luaj.vm2.lib.StringLib
import org.luaj.vm2.lib.TableLib
import org.luaj.vm2.lib.VarArgFunction

class MainLua : Scene() {
    override suspend fun SContainer.sceneMain() {
        val globals = createLuaGlobals()

        val textStack = uiVerticalStack(padding = 8.0, adjustSize = false).xy(10, 10)

        fun luaprintln(str: String) {
            println("LUA_PRINTLN: $str")
            textStack.text(str)
            //kotlin.io.println()
        }

        // Overwrite print function
        globals["print"] = object : VarArgFunction() {
            override fun invoke(args: Varargs): Varargs {
                val tostring = globals["tostring"]
                val out = (1 .. args.narg())
                    .map { tostring.call(args.arg(it)).strvalue()!!.tojstring() }
                luaprintln(out.joinToString("\t"))
                return LuaValue.NONE
            }
        }
        val result = globals.load(
            //language=lua
            """
            function max(a, b)
                if (a > b) then
                    return a
                else
                    return b
                end
            end
            a = 10
            res = 1 + 2 + a + max(20, 30)
            print(res - 1)
            b = {}
            b[1] = 10
            print(b)
            for i=4,1,-1 do print(i) end
            return res
        """).call()

        luaprintln(result.toString())
    }

    fun createLuaGlobals(): Globals = Globals().apply {
        load(BaseLib())
        load(PackageLib())
        load(Bit32Lib())
        load(TableLib())
        load(StringLib())
        load(CoroutineLib())
        LoadState.install(this)
        LuaC.install(this)
    }
}
