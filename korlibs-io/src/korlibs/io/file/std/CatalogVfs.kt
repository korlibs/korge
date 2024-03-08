package korlibs.io.file.std

import korlibs.datastructure.FastStringMap
import korlibs.datastructure.getOrPut
import korlibs.time.DateTime
import korlibs.logger.Logger
import korlibs.io.dynamic.dyn
import korlibs.io.file.PathInfo
import korlibs.io.file.Vfs
import korlibs.io.file.VfsCachedStatContext
import korlibs.io.file.VfsFile
import korlibs.io.file.VfsOpenMode
import korlibs.io.file.VfsStat
import korlibs.io.file.baseName
import korlibs.io.file.normalize
import korlibs.io.file.parent
import korlibs.io.lang.FileNotFoundException
import korlibs.io.serialization.json.Json
import korlibs.io.stream.AsyncStream
import korlibs.io.stream.AsyncStreamBase
import korlibs.io.stream.buffered
import korlibs.io.stream.toAsyncStream
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.withContext

fun VfsFile.withCatalog(): VfsFile = CatalogVfs(this).root
fun VfsFile.withCatalogJail(): VfsFile = CatalogVfs(this.jail()).root

open class CatalogVfs(val parent: VfsFile) : Vfs.Proxy() {
    private val logger = Logger("CatalogVfs")

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
        val info = cachedListSimpleStatsOrNull(PathInfo(normalizedPath).parent.fullPath)
            ?: return cacheSingle.getOrPut(path) { this@CatalogVfs.parent[path].stat() }
        return info[baseName]
            ?: createNonExistsStat(normalizedPath, cache = true)
    }

    private val cacheSingle = LinkedHashMap<String, VfsStat>()

    override suspend fun listFlow(path: String): Flow<VfsFile> = cachedListSimpleStats(path).map { it.value.enrichedFile }.asFlow()

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
        } catch (e: Throwable) {
            if (e !is FileNotFoundException) logger.error { e }
            return null
        }
        val data = Json.parse(catalogJsonString).dyn

        when (data.value) {
            is List<*> -> {
                return data.list.map {
                    val localName = PathInfo(it["name"].str).baseName
                    createExistsStat(
                        path = "$path/$localName",
                        isDirectory = it["isDirectory"].bool,
                        size = it["size"].long,
                        createTime = DateTime.fromUnixMillis(it["createTime"].long),
                        modifiedTime = DateTime.fromUnixMillis(it["modifiedTime"].long),
                        cache = true
                    )
                }.associateBy { it.baseName }
            }

            else -> {
                val out = LinkedHashMap<String, VfsStat>()
                for (key in data.keys) {
                    val fileName = key.str
                    val info = data[key]
                    val size = info[0].toLongOrNull() ?: 0L
                    val creationTime = info[1].toLongOrNull() ?: 0L
                    val modifiedTime = info[2].toLongOrNull() ?: creationTime
                    val isDirectory = fileName.endsWith("/")
                    val baseName = fileName.trimEnd('/')
                    out[baseName] = createExistsStat(
                        path = "$path/$baseName",
                        isDirectory = isDirectory,
                        size = size,
                        createTime = DateTime.fromUnixMillis(creationTime),
                        modifiedTime = DateTime.fromUnixMillis(modifiedTime),
                        cache = true
                    )
                }
                return out
            }
        }
    }
}
