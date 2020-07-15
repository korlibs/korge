package com.soywiz.korio.file.std

import com.soywiz.korio.file.*

fun JailVfs(jailRoot: VfsFile): VfsFile = object : Vfs.Proxy() {
	val baseJail = jailRoot.pathInfo.normalize()

	override suspend fun access(path: String): VfsFile = jailRoot[path.pathInfo.normalize().trim('/')]

	override suspend fun VfsFile.transform(): VfsFile {
		val outPath = this.path.pathInfo.normalize()
		if (!outPath.startsWith(baseJail)) throw UnsupportedOperationException("Jail not base root : ${this.path} | $baseJail")
		return file(outPath.substring(baseJail.length))
	}

	override val absolutePath: String get() = jailRoot.absolutePath

	override fun toString(): String = "JailVfs($jailRoot)"
}.root
