package com.brashmonkey.spriter

/**
 * Represents a reference to a specific file.
 * A file reference consists of a folder and file index.
 * @author Trixt0r
 */
data class FileReference(var folder: Int, var file: Int) {
	fun set(folder: Int, file: Int) {
		this.folder = folder
		this.file = file
	}

	fun set(ref: FileReference) = this.set(ref.folder, ref.file)
	fun hasFile(): Boolean = this.file != -1
	fun hasFolder(): Boolean = this.folder != -1

	override fun toString(): String = "[folder: $folder, file: $file]"

}
