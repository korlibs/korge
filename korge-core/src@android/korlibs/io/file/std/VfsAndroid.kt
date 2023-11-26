package korlibs.io.file.std

import android.content.*
import android.os.*
import korlibs.io.android.*
import korlibs.io.async.*
import korlibs.io.file.*
import korlibs.io.net.*
import korlibs.io.stream.*
import korlibs.io.util.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.io.*
import java.net.URL
import kotlin.math.*

private var _vfsInitWithAndroidContextOnce: Boolean = false
var absoluteCwd = File(".").absolutePath
val tmpdir: String by lazy { System.getProperty("java.io.tmpdir") }

fun vfsInitWithAndroidContextOnce(context: Context) {
    if (_vfsInitWithAndroidContextOnce) return
    _vfsInitWithAndroidContextOnce = true
    absoluteCwd = context.applicationInfo.dataDir
}

class AndroidDeferredVfs(private val generate: (Context) -> VfsFile) : Vfs.Proxy() {
    private var _generated: VfsFile? = null
    suspend fun generated(): VfsFile {
        val context = androidContext()
        if (_generated == null) {
            _generated = generate(context)
        }
        return _generated!!
    }

    override suspend fun access(path: String): VfsFile {
        return generated()[path]
    }
}

fun AndroidDeferredFile(generate: (Context) -> File): VfsFile =
    AndroidDeferredVfs { localVfs(generate(it)).jail() }.root

actual val standardVfs: StandardVfs = object : StandardVfs() {
    override val resourcesVfs: VfsFile by lazy { AndroidResourcesVfs().root }
    override val rootLocalVfs: VfsFile by lazy { localVfs(absoluteCwd) }
}

actual val applicationVfs: VfsFile by lazy { localVfs(absoluteCwd) }
actual val applicationDataVfs: VfsFile by lazy { AndroidDeferredFile { it.getDir("korio", Context.MODE_PRIVATE) } }
actual val cacheVfs: VfsFile by lazy { AndroidDeferredFile { it.cacheDir } }
actual val externalStorageVfs: VfsFile by lazy { localVfs(absoluteCwd) }
actual val userHomeVfs: VfsFile by lazy { localVfs(absoluteCwd) }
actual val tempVfs: VfsFile by lazy { AndroidDeferredFile { it.cacheDir } }

actual fun localVfs(path: String, async: Boolean): VfsFile {
    return when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.O -> AsynchronousFileChannelVfs()[path]
        else -> BaseLocalVfsJvm()[path]
    }
}

// Extensions
operator fun LocalVfs.Companion.get(base: File) = localVfs(base)

fun localVfs(base: File): VfsFile = localVfs(base.absolutePath)
fun jailedLocalVfs(base: File): VfsFile = localVfs(base.absolutePath).jail()
suspend fun File.open(mode: VfsOpenMode) = localVfs(this).open(mode)
fun File.toVfs() = localVfs(this)
fun UrlVfs(url: URL): VfsFile = UrlVfs(url.toString())

class AndroidResourcesVfs : Vfs() {
	override suspend fun open(path: String, mode: VfsOpenMode): AsyncStream =
        readRange(path, LONG_ZERO_TO_MAX_RANGE).openAsync(mode.cmode)

    override suspend fun listFlow(path: String): Flow<VfsFile> {
        val context = androidContext()
        return flow<VfsFile> {
            val rpath = path.trim('/')
            val files = context.assets.list(rpath)?.toList() ?: emptyList()
            //println("AndroidResourcesVfs.listSimple: path=$path, rpath=$rpath")
            //println(" -> $files")
            files.map { file(it) }
        }.flowOn(Dispatchers.CIO)
    }

    override suspend fun stat(path: String): VfsStat {
        val context = androidContext()
        return doIo {
            val rpath = path.trim('/')

            try {
                val files = context.assets.list(rpath) ?: emptyArray()
                if (files.isNotEmpty()) {
                    createExistsStat(path, isDirectory = true, size = 0L)
                } else {
                    context.assets.openFd(rpath).use {
                        createExistsStat(path, isDirectory = false, size = it.length)
                    }
                }
            } catch (e: IOException) {
                createNonExistsStat(path)
            }
        }
    }

    override suspend fun readRange(path: String, range: LongRange): ByteArray {
        val context = androidContext()
        return doIo {

            //val path = "/assets/" + path.trim('/')
            val rpath = path.trim('/')

            val fs = context.assets.open(rpath)
            fs.skip(range.start)
            val out = ByteArrayOutputStream()
            val temp = ByteArray(16 * 1024)
            var available = (range.endExclusiveClamped - range.start)
            while (available >= 0) {
                val read = fs.read(temp, 0, min(temp.size.toLong(), available).toInt())
                if (read <= 0) break
                out.write(temp, 0, read)
                available -= read
            }
            out.toByteArray()
        }
    }
}
