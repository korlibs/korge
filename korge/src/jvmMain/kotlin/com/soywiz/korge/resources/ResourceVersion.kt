package com.soywiz.korge.resources

import com.soywiz.korio.dynamic.mapper.Mapper
import com.soywiz.korio.dynamic.serialization.parseTyped
import com.soywiz.korio.dynamic.serialization.stringifyTyped
import com.soywiz.korio.file.VfsFile
import com.soywiz.korio.file.baseName
import com.soywiz.korio.serialization.json.Json
import com.soywiz.krypto.*

data class ResourceVersion(val name: String, val loaderVersion: Int, val sha1: String, val configSha1: String = "") {
	suspend fun writeMeta(metaFile: VfsFile) {
		metaFile.writeString(Json.stringifyTyped(this, Mapper))
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

            val content = when {
                file.isDirectory() -> byteArrayOf()
                else -> file.readBytes()
            }
			val hash = content.sha1().hex
			val configHash = if (configFile.exists()) configFile.readBytes().sha1().hex else ""
			return ResourceVersion(file.baseName, loaderVersion, hash, configHash)
		}

		suspend fun readMeta(metaFile: VfsFile): ResourceVersion = Json.parseTyped(metaFile.readString(), Mapper)
	}
}
