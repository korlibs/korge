package com.soywiz.korge.reloadagent

/*
import com.sun.nio.file.SensitivityWatchEventModifier
import java.io.File
import java.nio.file.FileSystems
import java.nio.file.Path
import java.nio.file.StandardWatchEventKinds
import java.nio.file.WatchEvent
import java.nio.file.WatchService
import kotlin.io.path.absolutePathString
import kotlin.io.path.isDirectory
import kotlin.io.path.listDirectoryEntries

interface CommonWatcher {
    fun watch(vararg paths: String): Sequence<List<String>>
}

class FSWatchCommonWatcher : CommonWatcher {
    override fun watch(vararg paths: String): Sequence<List<String>> = sequence {
        val lock = Any()
        val bufferedLines = arrayListOf<String>()
        var running = true
        Thread {
            //val p = ProcessBuilder("/bin/sh", "-c", "/opt/homebrew/bin/fswatch", *paths).inheritIO().start()
            val p = ProcessBuilder("/opt/homebrew/bin/fswatch", *paths).start()
            Runtime.getRuntime().addShutdownHook(Thread { p.destroy() });

            try {
                val reader = p.inputStream.reader().buffered()
                for (line in reader.lineSequence()) {
                    synchronized(lock) {
                        bufferedLines += line
                    }
                }
            } finally {
                p.waitFor()
                running = false
            }
        }.start()
        while (running) {
            Thread.sleep(1L)
            val size1 = synchronized(lock) { bufferedLines.size }
            if (size1 > 0) {
                Thread.sleep(300L)
                val size2 = synchronized(lock) { bufferedLines.size }
                if (size1 == size2) {
                    yield(synchronized(lock) {
                        bufferedLines.toList().also { bufferedLines.clear() }
                    })
                }
            }
        }
        //Runtime.getRuntime().addShutdownHook(Thread {
        //    p.destroy()
        //    Thread.sleep(500L)
        //    p.destroyForcibly()
        //})
        //val exit = p.waitFor()
    }
}

class JVMWatchCommonWatcher : CommonWatcher {
    override fun watch(vararg paths: String): Sequence<List<String>> = sequence {
        val watcher: WatchService = FileSystems.getDefault().newWatchService()
        for (folderToWatch in paths) {
            for (path in File(folderToWatch).toPath().getRecursivePaths()) {
                path.register(
                    watcher,
                    arrayOf(StandardWatchEventKinds.ENTRY_CREATE, StandardWatchEventKinds.ENTRY_DELETE, StandardWatchEventKinds.ENTRY_MODIFY),
                    // @TODO: This is still slow
                    SensitivityWatchEventModifier.HIGH
                )
            }
        }

        while (true) {
            val key = watcher.take()
            val files = mutableSetOf<String>()

            try {
                for (event in key.pollEvents()) {
                    // @TODO: If we create a new folder, we should register a new key
                    val kind: WatchEvent.Kind<*> = event.kind()
                    if (kind == StandardWatchEventKinds.OVERFLOW) {
                        continue
                    }
                    val ev: WatchEvent<Path> = event as WatchEvent<Path>
                    val dir = key.watchable() as Path
                    val fileName: Path = ev.context()
                    val fullPath: Path = dir.resolve(fileName)
                    files += fullPath.absolutePathString().replace("\\", "/")
                }
            } finally {
                key.reset()
            }

            yield(files.toList())
        }
    }
}

private fun Path.getRecursivePaths(out: ArrayList<Path> = arrayListOf()): List<Path> {
    out.add(this)
    for (child in this.listDirectoryEntries()) {
        if (child.isDirectory()) {
            child.getRecursivePaths(out)
        }
    }
    return out
}
*/
