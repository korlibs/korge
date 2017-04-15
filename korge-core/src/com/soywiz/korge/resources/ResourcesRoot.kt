package com.soywiz.korge.resources

import com.soywiz.korio.inject.AsyncDependency
import com.soywiz.korio.inject.Singleton
import com.soywiz.korio.vfs.Mountable
import com.soywiz.korio.vfs.MountableVfs
import com.soywiz.korio.vfs.ResourcesVfs
import com.soywiz.korio.vfs.VfsFile

@Singleton
class ResourcesRoot : AsyncDependency {
	lateinit private var root: VfsFile
	lateinit private var mountable: Mountable

	fun mount(path: String, file: VfsFile) {
		mountable.mount(path, file)
	}

	operator fun get(path: String) = root[path]
	operator fun get(path: Path) = root[path.path]

	suspend override fun init() {
		root = MountableVfs() {
			mountable = this
		}
		mount("/", ResourcesVfs)
	}
}
