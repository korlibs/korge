package com.soywiz.korge.gradle.targets.linux

import java.io.*
import java.nio.file.Files

object LDLibraries {
    private val libFolders = LinkedHashSet<File>()
    private val loadConfFiles = LinkedHashSet<File>()
    // /etc/ld.so.conf
    // include /etc/ld.so.conf.d/*.conf

    init {
        try {
            loadConfFile(File("/etc/ld.so.conf"))
        } catch (e: Throwable) {
            e.printStackTrace()
        }
    }

    val ldFolders = libFolders.toList()

    fun hasLibrary(name: String) = ldFolders.any { File(it, name).exists() }

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
                libFolders += File(tline)
            }
        }
    }
}
