package com.soywiz.korio.file.std

import com.soywiz.kds.FastStringMap
import com.soywiz.kds.getOrPut
import com.soywiz.klock.DateTime
import com.soywiz.korio.dynamic.dyn
import com.soywiz.korio.file.PathInfo
import com.soywiz.korio.file.Vfs
import com.soywiz.korio.file.VfsCachedStatContext
import com.soywiz.korio.file.VfsFile
import com.soywiz.korio.file.VfsOpenMode
import com.soywiz.korio.file.VfsStat
import com.soywiz.korio.file.baseName
import com.soywiz.korio.file.normalize
import com.soywiz.korio.file.parent
import com.soywiz.korio.lang.FileNotFoundException
import com.soywiz.korio.serialization.json.Json
import com.soywiz.korio.stream.AsyncStream
import com.soywiz.korio.stream.AsyncStreamBase
import com.soywiz.korio.stream.buffered
import com.soywiz.korio.stream.toAsyncStream
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.withContext

fun VfsFile.withCatalog(): VfsFile = CatalogVfs(this).root
fun VfsFile.withCatalogJail(): VfsFile = CatalogVfs(this.jail()).root

open class CatalogVfs(val parent: VfsFile) : Vfs.Proxy() {
    override suspend fun open(path: String, mode: VfsOpenMode): AsyncStream {
        val fstat = statOrNull(path)
        val base = withContext(VfsCachedStatContext(fstat)) {
            super.open(path, mode).base
        }
        return object : AsyncStreamBase() {
            override suspend fun read(position: Long, buffer: ByteArray, offset: Int, len: Int): Int =
                base.read(position, buffer, offset, len)
            override suspend fun write(position: Long, buffer: ByteArray, offset: Int, len: Int) =
                base.write(position, buffer, offset, len)
            override suspend fun setLength(value: Long) = base.setLength(value)
            override suspend fun getLength(): Long = if (fstat?.exists == true) fstat.size else base.getLength()
            override suspend fun close() = base.close()
        }.toAsyncStream().buffered()
    }

    override suspend fun access(path: String): VfsFile = parent[path]

    override suspend fun stat(path: String): VfsStat {
        return statOrNull(path) ?: createNonExistsStat(PathInfo(path).normalize(), cache = true)
    }

    suspend fun statOrNull(path: String): VfsStat? {
        val normalizedPath = PathInfo(path).normalize()
        if (normalizedPath == "/" || normalizedPath == "") {
            return createExistsStat("/", isDirectory = true, size = 0L, cache = true)
        }
        val baseName = PathInfo(normalizedPath).baseName
        val info = cachedListSimpleStatsOrNull(PathInfo(normalizedPath).parent.fullPath) ?: return null
        return info[baseName] ?: createNonExistsStat(normalizedPath, cache = true)
    }

    override suspend fun listFlow(path: String): Flow<VfsFile> = listSimple(path).asFlow()

    override suspend fun listSimple(path: String): List<VfsFile> =
        cachedListSimpleStats(path).map { it.value.enrichedFile }

    private val catalogCache = FastStringMap<Map<String, VfsStat>?>()

    suspend fun cachedListSimpleStats(path: String): Map<String, VfsStat> {
        return cachedListSimpleStatsOrNull(path) ?: emptyMap()
    }

    suspend fun cachedListSimpleStatsOrNull(path: String): Map<String, VfsStat>? {
        val key = PathInfo(path).normalize()
        return catalogCache.getOrPut(key) { listSimpleStatsOrNull(key) }
    }

    suspend fun listSimpleStatsOrNull(path: String): Map<String, VfsStat>? {
        val catalogJsonString = try {
            parent[path]["\$catalog.json"].readString()
        } catch (e: FileNotFoundException) {
            return null
        }
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
