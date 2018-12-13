package com.soywiz.korge.intellij

import com.intellij.openapi.vfs.VirtualFile
import com.soywiz.korio.async.AsyncSequence
import com.soywiz.korio.async.asyncGenerate
import com.soywiz.korio.lang.Closeable
import com.soywiz.korio.stream.AsyncInputStream
import com.soywiz.korio.stream.AsyncStream
import com.soywiz.korio.file.*
import java.io.File
import java.io.FileNotFoundException
import kotlin.reflect.KClass

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

	suspend override fun delete(path: String): Boolean {
		return super.delete(path)
	}

	suspend override fun exec(path: String, cmdAndArgs: List<String>, handler: VfsProcessHandler): Int {
		return super.exec(path, cmdAndArgs, handler)
	}

	suspend override fun exec(
		path: String,
		cmdAndArgs: List<String>,
		env: Map<String, String>,
		handler: VfsProcessHandler
	): Int {
		return super.exec(path, cmdAndArgs, env, handler)
	}

	override fun getAbsolutePath(path: String): String = accessSure(path).toString()

	suspend override fun list(path: String): AsyncSequence<VfsFile> = asyncGenerate {
		val children = accessSure(path).children
		for (item in children.map { VfsFile(this@IdeaAdaptorVfs, "$path/${it.name}") }) {
			yield(item)
		}
	}

	suspend override fun mkdir(path: String, attributes: List<Attribute>): Boolean {
		return super.mkdir(path, attributes)
	}

	suspend override fun open(path: String, mode: VfsOpenMode): AsyncStream {
		return super.open(path, mode)
	}

	suspend override fun put(path: String, content: AsyncInputStream, attributes: List<Attribute>): Long {
		return super.put(path, content, attributes)
	}

	suspend override fun readRange(path: String, range: LongRange): ByteArray {
		return super.readRange(path, range)
	}

	suspend override fun <T : Any> readSpecial(path: String, clazz: KClass<T>): T {
		return super.readSpecial(path, clazz)
	}

	suspend override fun rename(src: String, dst: String): Boolean {
		return super.rename(src, dst)
	}

	suspend override fun setAttributes(path: String, attributes: List<Attribute>) {
		super.setAttributes(path, attributes)
	}

	suspend override fun setSize(path: String, size: Long) {
		super.setSize(path, size)
	}

	suspend override fun stat(path: String): VfsStat {
		return super.stat(path)
	}

	suspend override fun touch(path: String, time: Long, atime: Long) {
		super.touch(path, time, atime)
	}

	suspend override fun watch(path: String, handler: (VfsFileEvent) -> Unit): Closeable {
		return super.watch(path, handler)
	}
}
