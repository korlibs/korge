package korlibs.korge.resources

import korlibs.io.dynamic.*
import korlibs.io.file.*
import korlibs.io.serialization.json.*
import korlibs.crypto.*

data class ResourceVersion(val name: String, val loaderVersion: Int, val sha1: String, val configSha1: String = "") {
	suspend fun writeMeta(metaFile: VfsFile) {
        metaFile.writeString(Json.stringify(mapOf(
            "name" to name,
            "loaderVersion" to loaderVersion,
            "sha1" to sha1,
            "configSha1" to configSha1,
        )))
	}

	companion object {
		suspend fun fromFile(file: VfsFile, loaderVersion: Int): ResourceVersion {
			val configFile = file.appendExtension("config")

            val content = when {
                file.isDirectory() -> byteArrayOf()
                else -> file.readBytes()
            }
			val hash = content.sha1().hex
			val configHash = if (configFile.exists()) configFile.readBytes().sha1().hex else ""
			return ResourceVersion(file.baseName, loaderVersion, hash, configHash)
		}

		suspend fun readMeta(metaFile: VfsFile): ResourceVersion {
            val dyn = metaFile.readString().fromJson().dyn

            return ResourceVersion(dyn["name"].str, dyn["loaderVersion"].int, dyn["sha1"].str, dyn["configSha1"].str)
        }
	}
}