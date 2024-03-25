package korlibs.ffi

import korlibs.js.*
import korlibs.js.Deno

internal actual val FFIPlatformSyncIO: FFISyncIO = when {
    Deno.isDeno -> object : FFISyncIO {
        override fun exists(path: String): Boolean =
            try { Deno.statSync(path); true } catch (e: dynamic) { false }
        override fun isDirectory(path: String): Boolean = try { Deno.statSync(path).isDirectory } catch (e: dynamic) { false }
        override fun readBytes(path: String): ByteArray = Deno.readFileSync(path)
        override fun listFiles(path: String): List<String> = Deno.readDirSync(path).toArray().toList().map { it.name }
    }
    else -> TODO()
}
