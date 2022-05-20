package com.soywiz.korge.gradle.util

import java.io.*
import java.nio.file.Files

object LDLibraries {
    private val libFolders = LinkedHashSet<File>()
    private val loadConfFiles = LinkedHashSet<File>()

    val ldFolders: List<File> get() = libFolders.toList()

    // /etc/ld.so.conf
    // include /etc/ld.so.conf.d/*.conf

    fun addPath(path: String) {
        val file = File(path)
        if (file.isDirectory) {
            libFolders.add(file)
        }
    }

    init {
        try {
            // Fixed paths as described https://renenyffenegger.ch/notes/Linux/fhs/etc/ld_so_conf
            addPath("/lib")
            addPath("/usr/lib")
            // Load config file
            loadConfFile(File("/etc/ld.so.conf"))
        } catch (e: Throwable) {
            e.printStackTrace()
        }
    }

    fun hasLibrary(name: String) = libFolders.any { File(it, name).exists() }

    private fun loadConfFile(file: File) {
        if (file in loadConfFiles) return
        loadConfFiles.add(file)
        for (line in file.readLines()) {
            val tline = line.trim().substringBefore('#').takeIf { it.isNotEmpty() } ?: continue

            if (tline.startsWith("include ")) {
                val glob = tline.removePrefix("include ")
                val globFolder = File(glob).parentFile
                val globPattern = File(glob).name
                if (globFolder.isDirectory) {
                    for (folder in
                    Files.newDirectoryStream(globFolder.toPath(), globPattern).toList().map { it.toFile() }
                    ) {
                        loadConfFile(folder)
                    }
                }
            } else {
                addPath(tline)
            }
        }
    }
}
