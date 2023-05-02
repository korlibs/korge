package korlibs.korge.gradle

import korlibs.korge.gradle.texpacker.*
import korlibs.korge.gradle.util.*
import java.io.*
import kotlin.system.*

object KorgeTexturePacker {
    fun processFolder(logger: org.slf4j.Logger, generatedFolder: File, resourceFolders: List<File>) {
        for (folder in resourceFolders) {
            val files = folder.listFiles()?.toList() ?: emptyList()
            for (file in files) {
                if (file.name.endsWith(".atlas")) {
                    val atlasJsonFile = File(generatedFolder, file.nameWithoutExtension + ".atlas.json")
                    when {
                        file.isDirectory -> {
                            generate(logger, atlasJsonFile, arrayOf(file))
                        }
                        file.isFile -> {
                            val sources = file.readLines().filter { it.isNotBlank() }.map { File(folder, it) }.toTypedArray()
                            generate(logger, atlasJsonFile, sources)
                        }
                    }
                }
            }
        }
    }

    fun generate(logger: org.slf4j.Logger, outputFile: File, imageFolders: Array<File>) {
        val involvedFiles = NewTexturePacker.getAllFiles(*imageFolders)
        //val maxLastModifiedTime = involvedFiles.maxOfOrNull { it.file.lastModified() } ?: System.currentTimeMillis()
        val involvedString = involvedFiles.map { it.relative.name + ":" + it.file.length() + ":" + it.file.lastModified() }.sorted().joinToString("\n")
        val involvedFile = File(outputFile.parentFile, "." + outputFile.name + ".info")

        //if (!outputFile.exists() || involvedFile.takeIfExists()?.readText() != involvedString) {
        if (involvedFile.takeIfExists()?.readText() != involvedString) {
            val time = measureTimeMillis {
                val result = NewTexturePacker.packImages(*imageFolders, enableRotation = true, enableTrimming = true)
                val imageOut = result.write(outputFile)
                involvedFile.writeText(involvedString)
            }
            //outputFile.setLastModified(maxLastModifiedTime)
            //imageOut.setLastModified(maxLastModifiedTime)
            logger.info("KorgeTexturePacker.GENERATED in ${time}ms: $involvedFile")
        } else {
            logger.info("KorgeTexturePacker.CACHED: $involvedFile")
        }
    }
}
