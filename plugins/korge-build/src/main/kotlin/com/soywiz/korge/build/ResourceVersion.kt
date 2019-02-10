package com.soywiz.korge.build

import com.soywiz.korio.dynamic.mapper.*
import com.soywiz.korio.dynamic.serialization.*
import com.soywiz.korio.file.*
import com.soywiz.korio.serialization.json.*
import com.soywiz.korio.util.encoding.*
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
            val hash = SHA1.digest(file.readBytes()).hex
            val configHash =
                if (configFile.exists()) SHA1.digest(configFile.readBytes()).hex else ""
            return ResourceVersion(file.baseName, loaderVersion, hash, configHash)
        }

        suspend fun readMeta(metaFile: VfsFile): ResourceVersion = Json.parseTyped(metaFile.readString(), Mapper)
    }
}
