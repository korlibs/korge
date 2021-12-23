package com.soywiz.korge.filecatalog

import com.soywiz.korge.resources.*
import com.soywiz.korio.file.*
import com.soywiz.korio.serialization.json.*
import java.io.*

class CatalogDirectoryResourceProcessor : ResourceProcessor(forFolders = true) {
    override val version: Int = 1
    override val outputExtension: String = "json"

    override fun getOutputFileName(relativeFile: File): String {
        return "${relativeFile.name}/\$catalog"
    }

    override suspend fun processInternal(inputFile: VfsFile, outputFile: VfsFile) {
        val files = inputFile.listSimple()

        println("CatalogDirectoryResourceProcessor: inputFile=$inputFile, outputFile=$outputFile")
        println("-- $files")

        val jsonList = files.map {
            val stat = it.stat()
            mapOf(
                "name" to stat.baseName,
                "size" to stat.size,
                "modifiedTime" to stat.modifiedTime.unixMillisLong,
                "createTime" to stat.createTime.unixMillisLong,
                "isDirectory" to stat.isDirectory,
            )
        }

        outputFile.writeString(Json.stringify(jsonList))
    }
}
