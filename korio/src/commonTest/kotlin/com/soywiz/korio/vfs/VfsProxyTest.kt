package com.soywiz.korio.vfs

import com.soywiz.klock.milliseconds
import com.soywiz.korio.async.async
import com.soywiz.korio.async.delay
import com.soywiz.korio.async.suspendTest
import com.soywiz.korio.file.Vfs
import com.soywiz.korio.file.VfsFile
import com.soywiz.korio.file.std.MemoryVfsMix
import kotlin.test.Test
import kotlin.test.assertEquals

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
