package korlibs.io.file.std

import korlibs.platform.Platform
import korlibs.io.file.Vfs
import korlibs.io.file.VfsFile
import korlibs.io.file.withOnce
import korlibs.io.lang.Environment
import korlibs.io.lang.expand

abstract class LocalVfs : Vfs() {
	companion object {
		operator fun get(base: String) = localVfs(base)
	}

    override suspend fun getAttributes(path: String): List<Attribute> {
        val stat = stat(path)
        if (!stat.exists) return emptyList()
        return listOf(UnixPermissions(stat.mode))
    }

    override fun toString(): String = "LocalVfs"
}

var resourcesVfsDebug = false

open class StandardVfs {
    /**
     * Typically a dot folder in the home directory ~/.[name]
     *
     * For example userSharedFolder("korimFontCache") will map to `~/.korimFontCache` whenever possible
     */
    open fun userSharedCacheFile(name: String): VfsFile = when {
        Platform.os.isMobile -> cacheVfs[".$name"]
        else -> localVfs(Environment.expand("~/.${name.removePrefix(".")}"))
    }

    open fun userSharedCacheDir(name: String): VfsFile = userSharedCacheFile(name).withOnce { it.mkdirs() }

    /** Contains files from `src/...Main/resources` and generated files by the build system */
    open val resourcesVfs: VfsFile get() = TODO()
    open val rootLocalVfs: VfsFile get() = TODO()
}

expect val standardVfs: StandardVfs

val resourcesVfs: VfsFile get() = standardVfs.resourcesVfs

/** @TODO */
val rootLocalVfs: VfsFile get() = standardVfs.rootLocalVfs
/** @TODO */
expect val applicationVfs: VfsFile
/** @TODO */
expect val applicationDataVfs: VfsFile

/** A Memory Virtual File System for cache */
expect val cacheVfs: VfsFile
/** @TODO */
expect val externalStorageVfs: VfsFile
/** User home folder, usually `~`, `/Users/something`, `/home/something` or equivalent */
expect val userHomeVfs: VfsFile
/** Temp folder, usually `/tmp` or equivalent */
expect val tempVfs: VfsFile
/** Alias for [applicationVfs] */
val localCurrentDirVfs: VfsFile get() = applicationVfs

/** Gets a [VfsFile] in the Operating System filesystem in [path]. It supports accessing parent folders. */
expect fun localVfs(path: String, async: Boolean = true): VfsFile
/** Gets a [VfsFile] in the Operating System filesystem in [base]. Jailed. Doesn't support accessing parent folders. */
fun jailedLocalVfs(base: String): VfsFile = localVfs(base).jail()
