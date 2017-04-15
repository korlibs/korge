package com.brashmonkey.spriter

/**
 * Represents a folder in a Spriter SCML file.
 * A folder has at least an [.id], [.name] and [.files] may be empty.
 * An instance of this class holds an array of [File] instances.
 * Specific [File] instances can be accessed via the corresponding methods, i.e getFile().
 * @author Trixt0r
 */
class Folder(val id: Int, val name: String, files: Int) {
	companion object {
		val DUMMY = Folder(0, "", 0)
	}

	val files: Array<File> = Array<File>(files) { File.DUMMY }
	private var filePointer = 0

	/**
	 * Adds a [File] instance to this folder.
	 * @param file the file to add
	 */
	fun addFile(file: File) {
		this.files[filePointer++] = file
	}

	/**
	 * Returns a [File] instance with the given index.
	 * @param index the index of the file
	 * *
	 * @return the file with the given name
	 */
	fun getFile(index: Int): File {
		return files[index]
	}

	/**
	 * Returns a [File] instance with the given name.
	 * @param name the name of the file
	 * *
	 * @return the file with the given name or null if no file with the given name exists
	 */
	fun getFile(name: String): File? {
		val index = getFileIndex(name)
		if (index >= 0)
			return getFile(index)
		else
			return null
	}

	/**
	 * Returns a file index with the given name.
	 * @param name the name of the file
	 * *
	 * @return the file index with the given name or -1 if no file with the given name exists
	 */
	internal fun getFileIndex(name: String): Int {
		for (file in this.files)
			if (file.name == name) return file.id
		return -1
	}

	override fun toString(): String {
		var toReturn = javaClass.simpleName + "|[id: " + id + ", name: " + name
		for (file in files)
			toReturn += "\n" + file
		toReturn += "]"
		return toReturn
	}
}
