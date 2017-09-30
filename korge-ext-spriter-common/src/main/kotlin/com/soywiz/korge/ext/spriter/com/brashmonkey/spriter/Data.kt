package com.soywiz.korge.ext.spriter.com.brashmonkey.spriter


/**
 * Represents all the data which necessary to animate a Spriter generated SCML file.
 * An instance of this class holds [Folder]s and [Entity] instances.
 * Specific [Folder] and [Entity] instances can be accessed via the corresponding methods, i.e. getEntity()
 * and getFolder().
 * @author Trixt0r
 */
class Data(val scmlVersion: String, val generator: String, val generatorVersion: String, val pixelMode: PixelMode, folders: Int, entities: Int, val atlases: List<String>) {

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

	fun addFolder(folder: Folder) {
		this.folders[folderPointer++] = folder
	}

	fun addEntity(entity: Entity) {
		this.entities[entityPointer++] = entity
	}

	fun getFolder(name: String): Folder? {
		val index = getFolderIndex(name)
		if (index >= 0)
			return getFolder(index)
		else
			return null
	}

	fun getFolderIndex(name: String): Int = this.folders.firstOrNull { it.name == name }?.id ?: -1

	fun getFolder(index: Int): Folder = this.folders[index]

	fun getEntity(index: Int): Entity = this.entities[index]

	fun getEntity(name: String): Entity? {
		val index = getEntityIndex(name)
		if (index >= 0) return getEntity(index) else return null
	}

	fun getEntityIndex(name: String): Int = this.entities.firstOrNull { it.name == name }?.id ?: -1
	fun getFile(folder: Folder, file: Int): File = folder.getFile(file)
	fun getFile(folder: Int, file: Int): File = getFile(this.getFolder(folder), file)
	fun getFile(ref: FileReference): File = this.getFile(ref.folder, ref.file)

	/**
	 * @return The string representation of this spriter data
	 */
	override fun toString(): String {
		var toReturn = this::class.simpleName +
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
