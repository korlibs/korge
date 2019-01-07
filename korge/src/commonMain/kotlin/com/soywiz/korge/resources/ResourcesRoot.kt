package com.soywiz.korge.resources

import com.soywiz.korinject.*
import com.soywiz.korio.file.*
import com.soywiz.korio.file.std.*
import com.soywiz.korio.util.*

//@Singleton
class ResourcesRoot : AsyncDependency {
	private lateinit var root: VfsFile
	private lateinit var mountable: Mountable

	fun mount(path: String, file: VfsFile) {
		mountable.mount(path, file)
	}

	operator fun get(path: String) = root[path]
	operator fun get(path: VPath) = root[path.path]

	override suspend fun init() {
		root = MountableVfs() {
			mountable = this
		}
		mount("/", resourcesVfs)
	}

	suspend fun redirected(redirector: VfsFile.(String) -> String) {
		this.root = this.root.redirected { this.redirector(it) }
	}

	suspend fun mapExtensions(vararg maps: Pair<String, String>) {
		val mapsLC = maps.map { it.first.toLowerCase() to it.second }.toMap()
		redirected {
			val pi = PathInfo(it)
			val map = mapsLC[pi.extensionLC]
			if (map != null) {
				pi.fullPathWithExtension(map)
			} else {
				pi.fullPath
			}
		}
	}

	suspend fun mapExtensionsJustInJS(vararg maps: Pair<String, String>) {
		if (OS.isJs) {
			mapExtensions(*maps)
		}
	}

	override fun toString(): String = "ResourcesRoot[$root]"
}
