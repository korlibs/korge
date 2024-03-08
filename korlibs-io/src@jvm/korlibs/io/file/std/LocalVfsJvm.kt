package korlibs.io.file.std

import com.sun.nio.file.*
import korlibs.io.dynamic.*
import korlibs.io.file.*
import korlibs.io.lang.*
import korlibs.io.lang.IOException
import korlibs.io.stream.*
import korlibs.io.util.*
import java.io.*
import java.net.*
import java.nio.file.*

private val absoluteCwd by lazy { File(".").absolutePath }
val tmpdir: String by lazy { System.getProperty("java.io.tmpdir") }

actual val standardVfs: StandardVfs = object : StandardVfs() {
    override val resourcesVfs: VfsFile by lazy { ResourcesVfsProviderJvm()().root.jail() }
    override val rootLocalVfs: VfsFile by lazy { localVfs(absoluteCwd) }
}

actual val applicationVfs: VfsFile by lazy { localVfs(absoluteCwd) }
actual val applicationDataVfs: VfsFile by lazy { localVfs(absoluteCwd) }
actual val cacheVfs: VfsFile by lazy { MemoryVfs() }
actual val externalStorageVfs: VfsFile by lazy { localVfs(absoluteCwd) }
actual val userHomeVfs: VfsFile by lazy { localVfs(System.getProperty("user.home")) }
actual val tempVfs: VfsFile by lazy { localVfs(tmpdir) }

actual fun localVfs(path: String, async: Boolean): VfsFile = LocalVfsJvm()[path]

// Extensions
operator fun LocalVfs.Companion.get(base: File) = localVfs(base)

fun localVfs(base: File): VfsFile = localVfs(base.absolutePath)
fun jailedLocalVfs(base: File): VfsFile = localVfs(base.absolutePath).jail()
suspend fun File.open(mode: VfsOpenMode) = localVfs(this).open(mode)
fun File.toVfs() = localVfs(this)
fun File.toJailedVfs() = jailedLocalVfs(this.parentFile)[this.name]
fun UrlVfs(url: URL): VfsFile = UrlVfs(url.toString())
operator fun File.get(path: String) = File(this, path)

fun ClassLoader.tryGetURLs(): List<URL> = try {
    when (this) {
        is URLClassLoader -> this.urLs.toList()
        else -> this.dyn["ucp"]["path"] as List<URL>
    }
} catch (e: Throwable) {
	//System.err.println("Error trying to get URLs from classloader $this")
	//e.printStackTrace()
	listOf()
}

private class ResourcesVfsProviderJvm {
	operator fun invoke(): Vfs = invoke(ClassLoader.getSystemClassLoader())
	operator fun invoke(classLoader: ClassLoader): Vfs = JvmClassLoaderResourcesVfs(classLoader)
}

class JvmClassLoaderResourcesVfs(val classLoader: ClassLoader) : MergedVfs(name = "MergedVfsDecorator") {
    override suspend fun init() {
        val currentDir = localCurrentDirVfs.absolutePath
        val urls = classLoader.tryGetURLs()
        //val urlsApp = urls.filter { File(it.toURI()).absolutePath.startsWith(currentDir) }
        val classPaths = urls.filter { it.toString().startsWith("file:") }.map { File(it.toURI()).absolutePath }
        val classPathsApp = classPaths.filter { it.startsWith(currentDir) }

        if (resourcesVfsDebug) {
            println("currentDirectory: ${localCurrentDirVfs.absolutePath}")
            if (classLoader is URLClassLoader) {
                println("classLoader is URLClassLoader")
            } else {
                println("classLoader !is URLClassLoader but $classLoader")
            }
            for (path in classPaths) {
                println("classLoader: $path")
            }
            for (path in classPathsApp) {
                println("classPathsApp: $path")
            }
        }

        val srcDirs = arrayListOf<File>()

        for (path in classPathsApp) {
            val relativePath = File(path).relativeTo(File(currentDir))
            if (resourcesVfsDebug) println("classPathsApp.relative: $relativePath")
            val srcs = findSrcs(File(currentDir), File(path))
            if (resourcesVfsDebug) println("classPathsApp.relative: $srcs")
            srcDirs += srcs
        }

        //println("localCurrentDirVfs: $localCurrentDirVfs, ${localCurrentDirVfs.absolutePath}")

        // @TODO: IntelliJ doesn't properly set resources folder for MPP just yet (on gradle works just fine),
        // @TODO: so at least we try to load resources from sources until this is fixed.
        run {
            for (folder in listOf(
                localCurrentDirVfs["src/commonMain/resources"],
                localCurrentDirVfs["src/jvmMain/resources"],
                localCurrentDirVfs["srcresources"],
                localCurrentDirVfs["resources"],
                localCurrentDirVfs["jvmResources"],
                localCurrentDirVfs["src/commonTest/resources"],
                localCurrentDirVfs["src/jvmTest/resources"],
                localCurrentDirVfs["testresources"],
            )) {
                if (folder.exists() && folder.isDirectory()) {
                    this += folder.jail()
                }
            }

            for (srcDir in srcDirs.map { it.toVfs() }) {
                for (folder in listOf(
                    srcDir["srcresources"],
                    srcDir["commonMain/resources"],
                    srcDir["jvmMain/resources"],
                    srcDir["commonTest/resources"],
                    srcDir["jvmTest/resources"],
                    srcDir["testresources"],
                    // Korge
                    srcDir["../build/genMainResources"],
                    srcDir["../build/genTestResources"]
                )) {
                    if (folder.exists() && folder.isDirectory()) {
                        this += folder.jail()
                    }
                }
            }
        }

        for (url in urls) {
            //println("ResourcesVfsProviderJvm.url: $url")
            val urlStr = url.toString()
            val vfs = when {
                urlStr.startsWith("http") -> UrlVfs(url)
                else -> localVfs(File(url.toURI()))
            }

            //println(vfs)

            when {
                vfs.extension in setOf("jar", "zip") -> {
                    //merged.vfsList += vfs.openAsZip()
                }
                else -> this += vfs.jail()
            }
        }
        //println(merged.options)

        //println("ResourcesVfsProviderJvm:classLoader:$classLoader")

        this += object : Vfs() {
            private fun normalize(path: String): String = path.trim('/')

            private fun getResource(npath: String): URL = classLoader.getResource(npath)
                ?: classLoader.getResource("/$npath")
                ?: invalidOp("Can't find '$npath' in ResourcesVfsProviderJvm")

            override suspend fun open(path: String, mode: VfsOpenMode): AsyncStream {
                val npath = normalize(path)
                val url = getResource(npath).caseSensitiveOrNull() ?: throw IOException("$npath doesn't match case")
                url.toFileOrNull()?.let {
                    return BaseLocalVfsJvm.open(this@JvmClassLoaderResourcesVfs, it, mode,  npath)
                }
                return MemorySyncStream(jvmExecuteIo { url.openStream().use { it.readBytes() } }).toAsync()
            }

            override suspend fun stat(path: String): VfsStat {
                val npath = normalize(path)
                try {
                    val url = getResource(npath).caseSensitiveOrNull() ?: throw IOException("$npath doesn't match case")
                    url.toFileOrNull()?.let {
                        return BaseLocalVfsJvm.stat(this@JvmClassLoaderResourcesVfs, it, npath)
                    }
                    return jvmExecuteIo { url.openStream()?.use { s ->
                        val size = s.available()
                        s.read()
                        createExistsStat(npath, isDirectory = false, size = size.toLong())
                    } } ?: error("Not found")
                } catch (e: Throwable) {
                    //e.printStackTrace()
                    return createNonExistsStat(npath, exception = e)
                }
            }

            override fun toString(): String = "ResourcesVfsProviderJvm"
        }.root

        //println("ResourcesVfsProviderJvm: $merged")
    }

    fun findSrcs(base: File, classPath: File): List<File> {
        val relative = classPath.relativeTo(base).path.replace('\\', '/')
        val out = arrayListOf<File>()
        var current = base
        for (part in relative.split('/')) {
            current = File(current, part)
            val srcDir = File(current, "src")
            if (srcDir.isDirectory) {
                out += srcDir
            }
        }
        return out
    }

    override fun toString(): String = "ResourcesVfs"
}

internal class LocalVfsJvm : AsynchronousFileChannelVfs() {
    override fun watchModifiers(path: String): Array<WatchEvent.Modifier> {
        return arrayOf(SensitivityWatchEventModifier.HIGH)
    }
}

/*
internal class LocalVfsJvm : BaseLocalVfsJvm() {
    override fun watchModifiers(path: String): Array<WatchEvent.Modifier> {
        return arrayOf(SensitivityWatchEventModifier.HIGH)
    }
}
*/
