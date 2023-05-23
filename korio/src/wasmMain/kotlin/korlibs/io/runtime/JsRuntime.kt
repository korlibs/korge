package korlibs.io.runtime

import korlibs.logger.*
import korlibs.io.*
import korlibs.io.file.VfsFile
import korlibs.io.file.std.MemoryVfs
import korlibs.io.file.std.localVfs
import korlibs.io.lang.Environment
import korlibs.io.lang.tempPath
import korlibs.io.net.AsyncClient
import korlibs.io.net.AsyncServer
import korlibs.io.net.http.HttpClient
import korlibs.io.net.http.HttpServer

abstract class JsRuntime {
    companion object {
        val logger = Logger("JsRuntime")
    }

    open val rawOsName: String = "unknown"

    open val rawPlatformName: String = when {
        isDenoJs -> "deno.js"
        isWeb -> "web.js"
        isNodeJs -> "node.js"
        isWorker -> "worker.js"
        isShell -> "shell.js"
        else -> "js"
    }
    open val isBrowser get() = false
    abstract fun existsSync(path: String): Boolean
    open fun currentDir(): String = "."

    open fun langs(): List<String> = listOf(
        Environment["LANG"]
            ?: Environment["LANGUAGE"]
            ?: Environment["LC_ALL"]
            ?: Environment["LC_MESSAGES"]
            ?: "english"
    )

    abstract fun envs(): Map<String, String>
    open fun env(key: String): String? = _envsUC[key.uppercase()]
    private val _envs by lazy { envs() }
    private val _envsUC by lazy { _envs.mapKeys { it.key.uppercase() }.toMap() }

    open fun openVfs(path: String): VfsFile = TODO()
    open fun localStorage(): VfsFile = MemoryVfs()
    open fun tempVfs(): VfsFile = localVfs(Environment.tempPath)
    open suspend fun createClient(secure: Boolean): AsyncClient =
        error("AsyncClient is not supported on this JS runtime")
    open suspend fun createServer(port: Int, host: String, backlog: Int, secure: Boolean): AsyncServer =
        error("AsyncServer is not supported on JS browser")
    open fun createHttpClient(): HttpClient = TODO()
    open fun createHttpServer(): HttpServer = TODO()
}
