package com.soywiz.korio.file.std

import com.soywiz.klock.*
import com.soywiz.korio.async.*
import com.soywiz.korio.dynamic.*
import com.soywiz.korio.file.*
import com.soywiz.korio.internal.*
import com.soywiz.korio.lang.*
import com.soywiz.korio.lang.Closeable
import com.soywiz.korio.stream.*
import com.soywiz.korio.util.*
import com.sun.nio.file.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.*
import kotlinx.coroutines.flow.*
import java.io.*
import java.io.FileNotFoundException
import java.io.IOException
import java.net.*
import java.nio.channels.CompletionHandler
import java.nio.file.*
import java.nio.file.Path
import java.util.concurrent.*
import kotlin.coroutines.*
import kotlin.math.*

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
	when {
		this is URLClassLoader -> this.urLs.toList()
		else -> KDynamic { this@tryGetURLs["ucp"]["path"] } as List<URL>
	}
} catch (e: Throwable) {
	//System.err.println("Error trying to get URLs from classloader $this")
	//e.printStackTrace()
	listOf()
}

private class ResourcesVfsProviderJvm {
	operator fun invoke(): Vfs = invoke(ClassLoader.getSystemClassLoader())
	operator fun invoke(classLoader: ClassLoader): Vfs = MergedVfsDecorator(classLoader)
}


class MergedVfsDecorator(val classLoader: ClassLoader, val merged: MergedVfs = MergedVfs()) : Vfs.Decorator(merged.root) {
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
                localCurrentDirVfs["resources"],
                localCurrentDirVfs["jvmResources"],
                localCurrentDirVfs["src/commonTest/resources"],
                localCurrentDirVfs["src/jvmTest/resources"]
            )) {
                if (folder.exists() && folder.isDirectory()) {
                    merged += folder.jail()
                }
            }

            for (srcDir in srcDirs.map { it.toVfs() }) {
                for (folder in listOf(
                    srcDir["commonMain/resources"],
                    srcDir["jvmMain/resources"],
                    srcDir["commonTest/resources"],
                    srcDir["jvmTest/resources"],
                    // Korge
                    srcDir["../build/genMainResources"],
                    srcDir["../build/genTestResources"]
                )) {
                    if (folder.exists() && folder.isDirectory()) {
                        merged += folder.jail()
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
                else -> merged += vfs.jail()
            }
        }
        //println(merged.options)

        //println("ResourcesVfsProviderJvm:classLoader:$classLoader")

        merged += object : Vfs() {
            private fun normalize(path: String): String = path.trim('/')

            private fun getResourceAsStream(npath: String) = classLoader.getResourceAsStream(npath)
                ?: classLoader.getResourceAsStream("/$npath")
                ?: invalidOp("Can't find '$npath' in ResourcesVfsProviderJvm")

            override suspend fun open(path: String, mode: VfsOpenMode): AsyncStream {
                val npath = normalize(path)
                //println("ResourcesVfsProviderJvm:open: $path")
                return MemorySyncStream(getResourceAsStream(npath).readBytes()).toAsync()
            }

            override suspend fun stat(path: String): VfsStat {
                val npath = normalize(path)
                //println("ResourcesVfsProviderJvm:stat: $npath")
                return try {
                    val s = getResourceAsStream(npath)
                    val size = s.available()
                    s.read()
                    createExistsStat(npath, isDirectory = false, size = size.toLong())
                } catch (e: Throwable) {
                    //e.printStackTrace()
                    createNonExistsStat(npath)
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
