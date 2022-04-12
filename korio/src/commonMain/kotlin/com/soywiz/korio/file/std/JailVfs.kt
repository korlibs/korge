package com.soywiz.korio.file.std

import com.soywiz.korio.file.*

class JailVfs private constructor(val jailRoot: VfsFile, dummy: Unit) : Vfs.Proxy() {
    companion object {
        operator fun invoke(jailRoot: VfsFile): VfsFile = JailVfs(jailRoot, Unit).root
    }

    val baseJail = jailRoot.pathInfo.normalize()

    override suspend fun access(path: String): VfsFile = jailRoot[path.pathInfo.normalize().trim('/')]

    override suspend fun VfsFile.transform(): VfsFile {
        val outPath = this.path.pathInfo.normalize()
        if (!outPath.startsWith(baseJail)) throw UnsupportedOperationException("Jail not base root : ${this.path} | $baseJail")
        return file(outPath.substring(baseJail.length))
    }

    override val absolutePath: String get() = jailRoot.absolutePath

    override fun toString(): String = "JailVfs($jailRoot)"
}
