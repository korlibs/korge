package korlibs.io.file.std

import korlibs.io.file.Vfs
import korlibs.io.file.VfsFile
import korlibs.io.file.VfsOpenMode
import korlibs.io.file.VfsStat
import korlibs.io.lang.FileNotFoundException
import korlibs.io.stream.AsyncStream
import kotlinx.coroutines.flow.Flow

object EmptyVfs : Vfs() {
    override suspend fun open(path: String, mode: VfsOpenMode): AsyncStream {
        throw FileNotFoundException(path)
    }
    override suspend fun stat(path: String): VfsStat {
        return createNonExistsStat(path)
    }

    override suspend fun listFlow(path: String): Flow<VfsFile> {
        throw FileNotFoundException(path)
        //return emptyList()
    }
}
