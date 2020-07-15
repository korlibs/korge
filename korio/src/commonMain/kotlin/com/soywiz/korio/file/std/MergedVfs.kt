package com.soywiz.korio.file.std

import com.soywiz.kds.iterators.*
import com.soywiz.korio.async.*
import com.soywiz.korio.file.*
import com.soywiz.korio.lang.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.*
import kotlinx.coroutines.flow.*
import kotlin.coroutines.*

open class MergedVfs(vfsList: List<VfsFile> = listOf()) : Vfs.Proxy() {
	private val vfsList = ArrayList(vfsList)

	operator fun plusAssign(other: VfsFile) {
		vfsList += other
	}

	operator fun minusAssign(other: VfsFile) {
		vfsList -= other
	}

	override suspend fun access(path: String): VfsFile {
		if (vfsList.size == 1) {
			return vfsList.first()[path]
		} else {
			return vfsList.map { it[path] }.firstOrNull { it.exists() } ?: vfsList.firstOrNull()?.get(path) ?: error("MergedVfs.access: VfsList is empty $vfsList")
		}
	}

	override suspend fun stat(path: String): VfsStat {
		vfsList.fastForEach { vfs ->
			val result = vfs[path].stat()
			if (result.exists) return result.copy(file = file(path))
		}
		return createNonExistsStat(path)
	}

	override suspend fun listFlow(path: String): Flow<VfsFile> = flow {
		val emitted = LinkedHashSet<String>()
		vfsList.fastForEach { vfs ->
			val items = runIgnoringExceptions { vfs[path].list() } ?: return@fastForEach

			try {
				for (v in items) {
					if (v.baseName !in emitted) {
						emitted += v.baseName
						emit(file("$path/${v.baseName}"))
					}
				}
			} catch (e: Throwable) {

			}
		}
	}

	override fun toString(): String = "MergedVfs($vfsList)"
}
