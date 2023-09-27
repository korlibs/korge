package korlibs.io.file.registry

import korlibs.math.toIntClamp
import korlibs.io.file.Vfs
import korlibs.io.file.VfsFile
import korlibs.io.file.VfsOpenMode
import korlibs.io.file.VfsStat
import korlibs.io.lang.UTF8
import korlibs.io.lang.toByteArray
import korlibs.io.lang.toString
import korlibs.io.stream.AsyncInputStream
import korlibs.io.stream.AsyncStream
import korlibs.io.stream.openAsync
import korlibs.io.stream.readAll
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlin.reflect.KClass

object WindowsRegistryVfs : Vfs() {
    fun normalizePath(path: String) = path.trim('/').replace('\\', '/')

    override suspend fun listFlow(path: String): Flow<VfsFile> {
        val path = normalizePath(path)
        val subKeys = runCatching { WindowsRegistry.listSubKeys(path) }.getOrElse { listOf() }
            .map { createExistsStat(normalizePath("$path/$it"), isDirectory = true, size = 0L) }
        val subKeyValues = runCatching { WindowsRegistry.listValues(path) }.getOrElse { emptyMap() }
            .map { (key, value) -> createExistsStat(normalizePath("$path/$key"), isDirectory = false, size = getValueSize(value)) }
        return (subKeys + subKeyValues).map { it.enrichedFile }.asFlow()
    }

    private fun readAll(path: String): ByteArray {
        val value = WindowsRegistry.getValue(path)
        return if (value is ByteArray) value else "$value".toByteArray(UTF8)
    }

    override suspend fun readRange(path: String, range: LongRange): ByteArray {
        val all = readAll(path)
        val fromIndex = range.start.toIntClamp(0, all.size)
        val toIndex = range.endInclusive.toIntClamp(0, all.size - 1)
        val chunk = all.copyOfRange(fromIndex, toIndex + 1)
        return chunk
    }

    override suspend fun put(path: String, content: AsyncInputStream, attributes: List<Attribute>): Long {
        val keyKind = attributes.filterIsInstance<FileKind>().firstOrNull() ?: FileKind.BINARY
        val valueContent = content.readAll()
        val value: Any = when (keyKind) {
            FileKind.BINARY -> valueContent
            FileKind.STRING -> valueContent.toString(UTF8)
            FileKind.LONG -> valueContent.toString(UTF8).toLong()
            FileKind.INT -> valueContent.toString(UTF8).toInt()
            else -> valueContent
        }
        WindowsRegistry.setValue(path, value)
        return valueContent.size.toLong()
    }

    override suspend fun delete(path: String): Boolean {
        if (WindowsRegistry.hasKey(path)) {
            WindowsRegistry.deleteKey(path)
            return true
        }
        if (WindowsRegistry.hasValue(path)) {
            WindowsRegistry.deleteValue(path)
            return true
        }
        return false
    }

    override suspend fun open(path: String, mode: VfsOpenMode): AsyncStream {
        if (mode.write) error("Unsupported WindowsRegistryVfs.open with write, use WindowsRegistryVfs.put/write methods instead")
        return readAll(path).openAsync()
    }

    override val supportedAttributeTypes: List<KClass<out Attribute>> get() = super.supportedAttributeTypes + listOf(
        FileKind::class)

    fun getValueSize(value: Any?): Long {
        return when (value) {
            null -> 0L
            is ByteArray -> value.size.toLong()
            else -> value.toString().toByteArray(UTF8).size.toLong()
        }
    }

    override suspend fun stat(path: String): VfsStat {
        if (WindowsRegistry.hasKey(path)) {
            return createExistsStat(path, isDirectory = true, size = 0L)
        }
        val value = WindowsRegistry.getValue(path)
        if (value != null) {
            return createExistsStat(path, isDirectory = false, size = getValueSize(value)).copy(kind = getKind(value))
        }
        return createNonExistsStat(path)
    }

    override suspend fun mkdir(path: String, attributes: List<Attribute>): Boolean = WindowsRegistry.createKey(path)
}
