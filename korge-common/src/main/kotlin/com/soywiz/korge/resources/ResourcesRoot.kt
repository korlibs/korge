package com.soywiz.korge.resources

import com.soywiz.korio.inject.AsyncDependency
import com.soywiz.korio.inject.Singleton
import com.soywiz.korio.util.OS
import com.soywiz.korio.vfs.*

@Singleton
class ResourcesRoot : AsyncDependency {
	lateinit private var root: VfsFile
	lateinit private var mountable: Mountable

	fun mount(path: String, file: VfsFile) {
		mountable.mount(path, file)
	}

	operator fun get(path: String) = root[path]
	operator fun get(path: Path) = root[path.path]
	operator fun get(path: VPath) = root[path.path]

	suspend override fun init() {
		root = MountableVfs() {
			mountable = this
		}
		mount("/", ResourcesVfs)
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
				pi.pathWithExtension(map)
			} else {
				pi.fullpath
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
