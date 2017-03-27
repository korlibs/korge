package com.soywiz.korfl

import com.soywiz.korge.resources.Path
import com.soywiz.korge.view.Views
import com.soywiz.korio.inject.AsyncFactory
import com.soywiz.korio.inject.AsyncFactoryClass
import com.soywiz.korio.vfs.ResourcesVfs

@AsyncFactoryClass(SwfLibraryFactory::class)
class SwfLibrary(
	val an: AnLibrary
) {

}

class SwfLibraryFactory(
	val path: Path,
	val views: Views
) : AsyncFactory<SwfLibrary> {
	suspend override fun create(): SwfLibrary = SwfLibrary(ResourcesVfs[path.path].readSWF(views))
}
