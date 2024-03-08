@file:Suppress("EXPERIMENTAL_FEATURE_WARNING")

package korlibs.io.file.std

import korlibs.datastructure.iterators.fastForEach
import korlibs.io.file.Vfs
import korlibs.io.file.VfsFile
import korlibs.io.file.normalize
import korlibs.io.file.pathInfo
import korlibs.io.lang.FileNotFoundException

inline fun MountableVfs(closeMounts: Boolean = false, callback: Mountable.() -> Unit): VfsFile =
    MountableVfsSync(closeMounts) { callback() }

inline fun MountableVfsSync(closeMounts: Boolean = false, callback: Mountable.() -> Unit): VfsFile =
    MountableVfs(closeMounts).also { callback(it) }.root

class MountableVfs(val closeMounts: Boolean) : Vfs.Proxy(), Mountable {
    private val mounts = ArrayList<Pair<String, VfsFile>>()

    override suspend fun close() {
        if (closeMounts) {
            mounts.fastForEach { mount ->
                mount.second.vfs.close()
            }
        }
    }

    override fun mount(folder: String, file: VfsFile) = this.apply {
        unmountInternal(folder)
        mounts += folder.pathInfo.normalize() to file
        resort()
    }

    override fun unmount(folder: String): Mountable = this.apply {
        unmountInternal(folder)
        resort()
    }

    private fun unmountInternal(folder: String) {
        mounts.removeAll { it.first == folder.pathInfo.normalize() }
    }

    private fun resort() {
        mounts.sortByDescending { it.first.length }
    }

    override suspend fun access(path: String): VfsFile {
        val rpath = path.pathInfo.normalize()
        mounts.fastForEach { (base, file) ->
            //println("$base/$file")
            if (rpath.startsWith(base)) return file[rpath.substring(base.length)]
        }
        throw FileNotFoundException(path)
    }

    override fun toString(): String = "MountableVfs"
}

interface Mountable {
    fun mount(folder: String, file: VfsFile): Mountable
    fun unmount(folder: String): Mountable
}

fun Mountable.mount(folder: String, vfs: Vfs): Mountable = mount(folder, vfs.root)
