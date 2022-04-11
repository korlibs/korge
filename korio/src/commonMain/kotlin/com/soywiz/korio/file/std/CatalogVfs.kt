package com.soywiz.korio.file.std

import com.soywiz.kds.*
import com.soywiz.klock.*
import com.soywiz.korio.dynamic.*
import com.soywiz.korio.file.*
import com.soywiz.korio.serialization.json.*
import kotlinx.coroutines.flow.*

fun VfsFile.withCatalog(): VfsFile = CatalogVfs(this).root
fun VfsFile.withCatalogJail(): VfsFile = CatalogVfs(this.jail()).root

open class CatalogVfs(val parent: VfsFile) : Vfs.Proxy() {

    override suspend fun access(path: String): VfsFile = parent[path]

    override suspend fun stat(path: String): VfsStat {
        val normalizedPath = PathInfo(path).normalize()
        if (normalizedPath == "/" || normalizedPath == "") {
            return createExistsStat("/", isDirectory = true, size = 0L, cache = true)
        }
        val baseName = PathInfo(normalizedPath).baseName
        val info = cachedListSimpleStats(PathInfo(normalizedPath).parent.fullPath)
        return info[baseName] ?: createNonExistsStat(normalizedPath, cache = true)
    }

    override suspend fun listFlow(path: String): Flow<VfsFile> = listSimple(path).asFlow()

    override suspend fun listSimple(path: String): List<VfsFile> =
        cachedListSimpleStats(path).map { it.value.enrichedFile }

    private val catalogCache = FastStringMap<Map<String, VfsStat>>()

    suspend fun cachedListSimpleStats(path: String): Map<String, VfsStat> {
        val key = PathInfo(path).normalize()
        return catalogCache.getOrPut(key) { listSimpleStats(key) }
    }

    suspend fun listSimpleStats(path: String): Map<String, VfsStat> {
        val catalogJsonString = parent[path]["\$catalog.json"].readString()
        val data = Json.parse(catalogJsonString).dyn

        return data.list.map {
            val localName = PathInfo(it["name"].str).baseName
            createExistsStat(
                path = "$path/$localName",
                isDirectory = it["isDirectory"].bool,
                size = it["size"].long,
                createTime = DateTime.fromUnix(it["createTime"].long),
                modifiedTime = DateTime.fromUnix(it["modifiedTime"].long),
                cache = true
            )
        }.associateBy { it.baseName }
    }
}
