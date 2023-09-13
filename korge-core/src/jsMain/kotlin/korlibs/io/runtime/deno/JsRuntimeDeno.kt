package korlibs.io.runtime.deno

import korlibs.io.jsObjectToMap
import korlibs.io.runtime.JsRuntime

private external val Deno: dynamic

object JsRuntimeDeno : JsRuntime() {
    override fun existsSync(path: String): Boolean = try {
        Deno.statSync(path)
        true
    } catch (e: dynamic) {
        false
    }

    override fun currentDir(): String = Deno.cwd()

    override fun env(key: String): String? = Deno.env.get(key)
    override fun envs() = jsObjectToMap(Deno.env.toObject())
}
