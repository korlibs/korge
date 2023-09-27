package korlibs.io.file.std

import korlibs.io.file.Vfs
import korlibs.io.file.VfsFile
import korlibs.io.file.lightCombine
import korlibs.io.file.pathInfo

fun DynamicRootVfs(base: Vfs, rootGet: () -> String) = DynamicRootVfsVfs(base, rootGet).root

open class DynamicRootVfsVfs(val base: Vfs, val rootGet: () -> String) : Vfs.Proxy() {
    private fun getLocalAbsolutePath(path: String) = rootGet().pathInfo.lightCombine(path.pathInfo).fullPath
    override fun getAbsolutePath(path: String): String = base.getAbsolutePath(getLocalAbsolutePath(path))
    override suspend fun access(path: String): VfsFile = base[getLocalAbsolutePath(path)]
}
