package korlibs.io.vfs

import korlibs.io.async.*
import korlibs.io.async.async
import korlibs.io.file.*
import korlibs.io.file.std.*
import korlibs.time.*
import kotlinx.coroutines.*
import kotlin.test.*

class VfsProxyTest {
    val log = arrayListOf<String>()
    @Test
    fun test() = suspendTest {
        val vfs = MyVfs(MemoryVfsMix("hello" to "world"))
        val a = async { log += "a:" + vfs["hello"].readString() }
        val b = async { log += "b:" + vfs["hello"].readString() }
        a.await()
        b.await()
        assertEquals("initializing,initialized,a:world,b:world", log.joinToString(","))
    }
    inner class MyVfs(val parent: VfsFile) : Vfs.Proxy() {
        var _init = false

        override suspend fun init() {
            log += "initializing"
            delay(10.milliseconds)
            _init = true
            log += "initialized"
        }

        override suspend fun access(path: String): VfsFile {
            return parent[path]
        }
    }
}
