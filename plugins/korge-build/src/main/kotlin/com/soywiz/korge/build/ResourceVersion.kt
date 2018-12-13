package com.soywiz.korge.build

import com.soywiz.korio.crypto.*
import com.soywiz.korio.file.*
import com.soywiz.korio.serialization.*
import com.soywiz.korio.serialization.json.*

data class ResourceVersion(val name: String, val loaderVersion: Int, val sha1: String, val configSha1: String = "") {
	suspend fun writeMeta(metaFile: VfsFile) {
		metaFile.writeString(Json.encode(this))
	}

	companion object {
		init {
			Mapper.registerType(ResourceVersion::class) {
				ResourceVersion(it["name"].gen(), it["loaderVersion"].gen(), it["sha1"].gen(), it["configSha1"].gen())
			}
			Mapper.registerUntype(ResourceVersion::class) {
				mapOf(
					"name" to it.name,
					"loaderVersion" to it.loaderVersion,
					"sha1" to it.sha1,
					"configSha1" to it.configSha1
				)
			}
		}

		suspend fun fromFile(file: VfsFile, loaderVersion: Int): ResourceVersion {
			val configFile = file.appendExtension("config")
			val hash = file.readBytes().hash(AsyncHash.SHA1).hex
			val configHash =
				if (configFile.exists()) configFile.readBytes().hash(AsyncHash.SHA1).hex else ""
			return ResourceVersion(file.basename, loaderVersion, hash, configHash)
		}

		suspend fun readMeta(metaFile: VfsFile): ResourceVersion = Json.decodeToType(metaFile.readString())
	}
}
