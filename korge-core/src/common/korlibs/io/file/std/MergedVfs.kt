package korlibs.io.file.std

import korlibs.datastructure.iterators.fastForEach
import korlibs.logger.Logger
import korlibs.io.file.Vfs
import korlibs.io.file.VfsFile
import korlibs.io.file.VfsStat
import korlibs.io.file.baseName
import korlibs.io.lang.runIgnoringExceptions
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

open class MergedVfs(vfsList: List<VfsFile> = listOf(), val name: String = "unknown") : Vfs.Proxy() {
    private val logger = Logger("MergedVfs")

    constructor(vararg vfsList: VfsFile) : this(vfsList.toList())

	private val vfsList = ArrayList(vfsList)

	operator fun plusAssign(other: VfsFile) {
		vfsList += other
	}

	operator fun minusAssign(other: VfsFile) {
		vfsList -= other
	}

    override suspend fun access(path: String): VfsFile {
        initOnce()
        return when (vfsList.size) {
            0 -> {
                val msg = "MergedVfs.access: VfsList is empty $vfsList : path=$path, name=$name"
                logger.error { msg }
                //return EmptyVfs[path]
                error(msg)
            }
            1 -> vfsList.first()[path]
            else -> vfsList.map { it[path] }.firstOrNull { it.exists() } ?: vfsList.first()[path]
        }
    }

	override suspend fun stat(path: String): VfsStat {
        initOnce()
		vfsList.fastForEach { vfs ->
			val result = vfs[path].stat()
			if (result.exists) return result.copy(file = file(path))
		}
		return createNonExistsStat(path)
	}

    override suspend fun listFlow(path: String): Flow<VfsFile> {
        initOnce()
        return flow {
            val emitted = LinkedHashSet<String>()
            vfsList.fastForEach { vfs ->
                val items = runIgnoringExceptions { vfs[path].list() } ?: return@fastForEach

                try {
                    items.collect { v ->
                        if (v.baseName !in emitted) {
                            emitted += v.baseName
                            emit(file("$path/${v.baseName}"))
                        }
                    }
                } catch (e: Throwable) {
                }
            }
        }
    }

	override fun toString(): String = "MergedVfs($vfsList)"
}
