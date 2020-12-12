package com.soywiz.korge.intellij

import com.intellij.openapi.command.undo.*
import com.intellij.openapi.vfs.*
import com.soywiz.korge.intellij.util.*
import com.soywiz.korio.async.*
import com.soywiz.korio.file.*
import com.soywiz.korio.file.std.*
import com.soywiz.korio.stream.*
import com.soywiz.korio.util.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.*
import java.io.*

fun VirtualFile.toVfs(): VfsFile {
	val file = this
	val url = file.canonicalPath ?: ""
	//println("VirtualFile.toVfs: path=${file.path}, canonicalPath=${file.url}")
	val root = this.root
	//println("VirtualFile.toVfs.root: path=${root.path}, canonicalPath=${root.canonicalPath}")
	//return File(URI(url)).toVfs()

	return File(url).toVfs()
}

fun VirtualFile.toTextualVfs(): VfsFile {
    val virtualFile = this
    val file = virtualFile.toVfs()
    val ref: DocumentReference = runWriteAction { DocumentReferenceManager.getInstance().create(virtualFile) }

    val documentFile = (object : Vfs() {
        override suspend fun open(path: String, mode: VfsOpenMode): AsyncStream {
            if (mode.write) error("Unsupported")
            return (ref.document?.text ?: "").toByteArray(Charsets.UTF_8).openAsync()
        }

        override suspend fun put(path: String, content: AsyncInputStream, attributes: List<Attribute>): Long {
            val byteArray = content.readAll()
            runWriteActionNoWait {
                try {
                    ref.document?.setText(byteArray.toString(Charsets.UTF_8))
                } catch (e: Exception) {
                    System.err.println("VirtualFile.toTextualVfs: ${e.message}")
                    //e.printStackTrace()
                }
            }
            return byteArray.size.toLong()
        }
    })[virtualFile.canonicalPath ?: virtualFile.name]

    return runBlocking {
        MountableVfs {
            mount("/", file.root)
            if (ref.document != null) {
                mount(file.path, documentFile)
            }
        }[file.path]
    }
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

	override suspend fun listSimple(path: String): List<VfsFile> {
		return accessSure(path).children.map { VfsFile(this@IdeaAdaptorVfs, "$path/${it.name}") }
	}
}
