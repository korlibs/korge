package com.soywiz.kproject.model

import com.soywiz.kproject.util.*
import java.io.*
import java.util.zip.*

fun unzipTo(outputDirectory: FileRef, zipFile: File) {
    ZipFile(zipFile).use { zip ->
        for (entry in zip.entries()) {
            if (!entry.isDirectory) {
                outputDirectory[entry.name.pathInfo.fullPath.trim('/')]
                    .writeBytes(zip.getInputStream(entry).use { it.readBytes() })
            }
        }
    }
}
