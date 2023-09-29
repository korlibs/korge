package com.soywiz.kproject.util

import java.io.*
import java.util.zip.*

/**
 * List files ordered by filename for reproducibility.
 * Never returns null.
 */
private fun File.listFilesOrdered(filter: ((File) -> Boolean)? = null): List<File> =
    listFiles()
        ?.let { if (filter != null) it.filter(filter) else it.asList() }
        ?.sortedBy { it.name }
        ?: emptyList()

fun zipTo(zipFile: File, baseDir: File) {
    zipTo(zipFile, baseDir, baseDir.walkReproducibly())
}


internal
fun File.walkReproducibly(): Sequence<File> = sequence {

    require(isDirectory)

    yield(this@walkReproducibly)

    var directories: List<File> = listOf(this@walkReproducibly)
    while (directories.isNotEmpty()) {
        val subDirectories = mutableListOf<File>()
        directories.forEach { dir ->
            dir.listFilesOrdered().partition { it.isDirectory }.let { (childDirectories, childFiles) ->
                yieldAll(childFiles)
                childDirectories.let {
                    yieldAll(it)
                    subDirectories.addAll(it)
                }
            }
        }
        directories = subDirectories
    }
}


private
fun zipTo(zipFile: File, baseDir: File, files: Sequence<File>) {
    zipTo(zipFile, fileEntriesRelativeTo(baseDir, files))
}


private
fun fileEntriesRelativeTo(baseDir: File, files: Sequence<File>): Sequence<Pair<String, ByteArray>> =
    files.filter { it.isFile }.map { file ->
        val path = file.normalisedPathRelativeTo(baseDir)
        val bytes = file.readBytes()
        path to bytes
    }


internal
fun File.normalisedPathRelativeTo(baseDir: File) =
    relativeTo(baseDir).path.replace('\\', '/')


fun zipTo(zipFile: File, entries: Sequence<Pair<String, ByteArray>>) {
    zipTo(zipFile.outputStream(), entries)
}

private
fun zipTo(outputStream: OutputStream, entries: Sequence<Pair<String, ByteArray>>) {
    ZipOutputStream(outputStream).use { zos ->
        entries.forEach { entry ->
            val (path, bytes) = entry
            zos.putNextEntry(
                ZipEntry(path).apply {
                    time = 0L
                    size = bytes.size.toLong()
                }
            )
            zos.write(bytes)
            zos.closeEntry()
        }
    }
}


fun unzipTo(outputDirectory: File, zipFile: File) {
    ZipFile(zipFile).use { zip ->
        for (entry in zip.entries()) {
            unzipEntryTo(outputDirectory, zip, entry)
        }
    }
}


private
fun unzipEntryTo(outputDirectory: File, zip: ZipFile, entry: ZipEntry) {
    val output = outputDirectory.resolve(File(entry.name).normalize().toString().trimStart('/'))
    if (entry.isDirectory) {
        output.mkdirs()
    } else {
        output.parentFile.mkdirs()
        zip.getInputStream(entry).use { it.copyTo(output) }
    }
}


private
fun InputStream.copyTo(file: File): Long =
    file.outputStream().use { copyTo(it) }
