package korlibs.io.lang

import korlibs.io.*
import korlibs.io.net.*
import korlibs.io.util.*
import kotlinx.browser.*

internal actual object EnvironmentInternal {
	actual operator fun get(key: String): String? = jsRuntime.env(key)
	actual fun getAll(): Map<String, String> = jsRuntime.envs()
}
