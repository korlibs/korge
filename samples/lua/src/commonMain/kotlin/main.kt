import com.soywiz.korge.*
import com.soywiz.korge.view.*
import org.luaj.vm2.*
import org.luaj.vm2.compiler.*
import org.luaj.vm2.lib.*

suspend fun main() = Korge {
//suspend fun main() {
	val globals = createLuaGlobals()
	val result = globals.load("""
		function max(a, b)
			if (a > b) then
				return a
			else
				return b
			end
		end
		a = 10
		res = 1 + 2 + a + max(20, 30)
		print(res)
		b = {}
		b[1] = 10
		print(b)
		return res
	""").call()
	text("Result from LUA: ${result.toint()}")
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
