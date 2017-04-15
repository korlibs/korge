package com.brashmonkey.spriter

/**
 * Represents a reference to a specific file.
 * A file reference consists of a folder and file index.
 * @author Trixt0r
 */
data class FileReference(var folder: Int, var file: Int) {

	override fun hashCode(): Int {
		return folder * 10000 + file//We can have 10000 files per folder
	}

	override fun equals(ref: Any?): Boolean {
		if (ref is FileReference) {
			return this.file == ref.file && this.folder == ref.folder
		} else
			return false
	}

	fun set(folder: Int, file: Int) {
		this.folder = folder
		this.file = file
	}

	fun set(ref: FileReference) {
		this.set(ref.folder, ref.file)
	}

	fun hasFile(): Boolean {
		return this.file != -1
	}

	fun hasFolder(): Boolean {
		return this.folder != -1
	}

	override fun toString(): String = "[folder: $folder, file: $file]"

}
