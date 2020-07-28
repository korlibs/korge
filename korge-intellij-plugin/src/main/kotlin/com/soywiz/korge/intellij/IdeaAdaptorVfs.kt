package com.soywiz.korge.intellij

import com.intellij.openapi.vfs.*
import com.soywiz.korio.async.*
import com.soywiz.korio.file.*
import com.soywiz.korio.file.std.*
import kotlinx.coroutines.channels.*
import java.io.*

fun VirtualFile.toVfs(): VfsFile {
	val file = this
	val url = file.canonicalPath ?: ""
	println("VirtualFile.toVfs: path=${file.path}, canonicalPath=${file.url}")
	val root = this.root
	println("VirtualFile.toVfs.root: path=${root.path}, canonicalPath=${root.canonicalPath}")
	//return File(URI(url)).toVfs()

	return File(url).toVfs()
}

val VirtualFile.root: VirtualFile get() = this.parent?.root ?: this

// @TODO: Do this properly
class IdeaAdaptorVfs(val file: VirtualFile) : Vfs() {
	override val absolutePath: String = file.toString()

	private fun access(path: String): VirtualFile? {
		var current: VirtualFile? = file
		for (component in path.split("/")) {
			when (component) {
				".", "" -> {
					current = current
				}
				".." -> {
					current = current?.parent
				}
				else -> {
					current = current?.findChild(component)
				}
			}
		}
		return current
	}

	private fun accessSure(path: String): VirtualFile =
		access(path) ?: throw FileNotFoundException("$absolutePath/$path")

	override suspend fun delete(path: String): Boolean {
		return super.delete(path)
	}

	override suspend fun exec(
		path: String,
		cmdAndArgs: List<String>,
		env: Map<String, String>,
		handler: VfsProcessHandler
	): Int {
		return super.exec(path, cmdAndArgs, env, handler)
	}

	override fun getAbsolutePath(path: String): String = accessSure(path).toString()

	override suspend fun list(path: String): ReceiveChannel<VfsFile> {
		return accessSure(path).children.map { VfsFile(this@IdeaAdaptorVfs, "$path/${it.name}") }.toChannel()
	}
}
