package com.soywiz.korge.ext.spriter.com.brashmonkey.spriter


/**
 * Represents all the data which necessary to animate a Spriter generated SCML file.
 * An instance of this class holds [Folder]s and [Entity] instances.
 * Specific [Folder] and [Entity] instances can be accessed via the corresponding methods, i.e. getEntity()
 * and getFolder().
 * @author Trixt0r
 */
class Data constructor(val scmlVersion: String, val generator: String, val generatorVersion: String, val pixelMode: PixelMode, folders: Int, entities: Int) {

	/**
	 * Represents the rendering mode stored in the spriter data root.
	 */
	enum class PixelMode {
		NONE, PIXEL_ART;


		companion object {

			/**
			 * @param mode
			 * *
			 * @return The pixel mode for the given int value. Default is [NONE].
			 */
			operator fun get(mode: Int): PixelMode {
				when (mode) {
					1 -> return PIXEL_ART
					else -> return NONE
				}
			}
		}
	}

	val folders: Array<Folder> = Array(folders) { Folder.DUMMY }
	val entities: Array<Entity> = Array(entities) { Entity.DUMMY }
	private var folderPointer = 0
	private var entityPointer = 0

	/**
	 * Adds a folder to this data.
	 * @param folder the folder to add
	 */
	fun addFolder(folder: Folder) {
		this.folders[folderPointer++] = folder
	}

	/**
	 * Adds an entity to this data.
	 * @param entity the entity to add
	 */
	fun addEntity(entity: Entity) {
		this.entities[entityPointer++] = entity
	}

	/**
	 * Returns a [Folder] instance with the given name.
	 * @param name the name of the folder
	 * *
	 * @return the folder with the given name or null if no folder with the given name exists
	 */
	fun getFolder(name: String): Folder? {
		val index = getFolderIndex(name)
		if (index >= 0)
			return getFolder(index)
		else
			return null
	}

	/**
	 * Returns a folder index with the given name.
	 * @param name name of the folder
	 * *
	 * @return the folder index of the Folder with the given name or -1 if no folder with the given name exists
	 */
	fun getFolderIndex(name: String): Int {
		for (folder in this.folders)
			if (folder.name == name) return folder.id
		return -1
	}

	/**
	 * Returns a [Folder] instance at the given index.
	 * @param index the index of the folder
	 * *
	 * @return the [Folder] instance at the given index
	 */
	fun getFolder(index: Int): Folder {
		return this.folders[index]
	}

	/**
	 * Returns an [Entity] instance with the given index.
	 * @param index index of the entity to return.
	 * *
	 * @return the entity with the given index
	 * *
	 * @throws [IndexOutOfBoundsException] if the index is out of range
	 */
	fun getEntity(index: Int): Entity {
		return this.entities[index]
	}

	/**
	 * Returns an [Entity] instance with the given name.
	 * @param name the name of the entity
	 * *
	 * @return the entity with the given name or null if no entity with the given name exists
	 */
	fun getEntity(name: String): Entity? {
		val index = getEntityIndex(name)
		if (index >= 0)
			return getEntity(index)
		else
			return null
	}

	/**
	 * Returns an entity index with the given name.
	 * @param name name of the entity
	 * *
	 * @return the entity index of the entity with the given name or -1 if no entity with the given name exists
	 */
	fun getEntityIndex(name: String): Int {
		for (entity in this.entities)
			if (entity.name == name) return entity.id
		return -1
	}

	/**
	 * Returns a [File] instance in the given [Folder] instance at the given file index.
	 * @param folder [Folder] instance to search in.
	 * *
	 * @param file index of the file
	 * *
	 * @return the [File] instance in the given folder at the given file index
	 */
	fun getFile(folder: Folder, file: Int): File {
		return folder.getFile(file)
	}

	/**
	 * Returns a [File] instance in the given folder at the given file index.
	 * @param folder index of the folder
	 * *
	 * @param file index of the file
	 * *
	 * @return the [File] instance in the given folder at the given file index
	 * *
	 * @throws [IndexOutOfBoundsException] if the folder or file index are out of range
	 */
	fun getFile(folder: Int, file: Int): File {
		return getFile(this.getFolder(folder), file)
	}

	/**
	 * Returns a [File] instance for the given [FileReference] instance.
	 * @param ref reference to the file
	 * *
	 * @return the [File] instance for the given reference
	 */
	fun getFile(ref: FileReference): File {
		return this.getFile(ref.folder, ref.file)
	}

	/**
	 * @return The string representation of this spriter data
	 */
	override fun toString(): String {
		var toReturn = javaClass.simpleName +
			"|[Version: " + scmlVersion +
			", Generator: " + generator +
			" (" + generatorVersion + ")]"
		for (folder in folders)
			toReturn += "\n" + folder
		for (entity in entities)
			toReturn += "\n" + entity
		toReturn += "]"
		return toReturn
	}

}
