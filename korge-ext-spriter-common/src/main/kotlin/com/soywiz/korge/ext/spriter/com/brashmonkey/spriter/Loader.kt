package com.soywiz.korge.ext.spriter.com.brashmonkey.spriter

import com.soywiz.korio.vfs.VfsFile

/**
 * A loader is responsible for loading all resources.
 * Since this library is meant to be as generic as possible, it cannot be assumed how to load a resource. Because of this this class has to be abstract.
 * This class takes care of loading all resources a [Data] instance contains.
 * To load all resources an instance relies on [.loadResource] which has to implemented with the backend specific methods.

 * @author Trixt0r
 * *
 * *
 * @param <R> The backend specific resource. In general such a resource is called "sprite", "texture" or "image".
</R> */
abstract class Loader<R>
/**
 * Creates a loader with the given Spriter data.
 * @param data the generated Spriter data
 */
(
	/**
	 * The current set data containing [Folder]s and [File]s.
	 */
	protected var data: Data?) {

	/**
	 * Contains all loaded resources if not [.isDisposed].
	 */
	protected val resources: HashMap<FileReference, R>

	/**
	 * The root path to the previous loaded Spriter SCML file.
	 */
	protected lateinit var root: VfsFile

	/**
	 * Returns whether this loader has been disposed or not.
	 * @return true if this loader is disposed
	 */
	var isDisposed: Boolean = false
		private set

	init {
		this.resources = HashMap<FileReference, R>(100)
	}

	/**
	 * Loads a resource.
	 * The path to the file can be resolved with [.root] and [.data].
	 * I recommend using [Data.getFile]. Then the path to the resource is [File.name] relative to [.root].
	 * @param ref the reference to load
	 * *
	 * @return the loaded resource
	 */
	protected abstract fun loadResource(ref: FileReference): R

	/**
	 * Called when all resources from [.data] have been loaded.
	 */
	protected fun finishLoading() {
	}

	/**
	 * Called before all resources get loaded.
	 */
	protected fun beginLoading() {
	}

	/**
	 * Loads all resources indicated by [.data].
	 * @param root the root folder of the previously loaded Spriter SCML file
	 */
	fun loadRoot(root: VfsFile) {
		this.root = root
		this.beginLoading()
		for (folder in data!!.folders) {
			for (file in folder.files) {
				//if(new java.io.File(root+"/"+file.name).exists()){
				val ref = FileReference(folder.id, file.id)
				this.resources.put(ref, this.loadResource(ref))
				//}
			}
		}
		this.isDisposed = false
		this.finishLoading()
	}

	/**
	 * Loads all resources indicated by [.data].
	 * @param file the previously loaded Spriter SCML file
	 */
	fun load(file: VfsFile) {
		this.loadRoot(file.parent)
	}

	/**
	 * Returns a resource the given reference is pointing to.
	 * @param ref the reference pointing to a resource
	 * *
	 * @return the resource or `null` if the resource is not loaded yet.
	 */
	operator fun get(ref: FileReference): R {
		return this.resources[ref]!!
	}

	/**
	 * Removes all loaded resources from the internal reference-resource map.
	 * Override this method and dispose all your resources. After that call [.dispose] of the super class.
	 */
	fun dispose() {
		resources.clear()
		data = null
		//root = ""
		isDisposed = true
	}

}
