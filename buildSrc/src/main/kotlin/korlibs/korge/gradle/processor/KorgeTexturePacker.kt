package korlibs.korge.gradle.processor

import korlibs.korge.gradle.texpacker.*
import korlibs.korge.gradle.util.*
import java.io.*
import kotlin.system.*

open class KorgeTexturePacker : KorgeResourceProcessor {
    override fun processFolder(context: KorgeResourceProcessorContext) {
        for (folder in context.resourceFolders) {
            val files = folder.listFiles()?.toList() ?: emptyList()
            for (file in files) {
                if (file.name.endsWith(".atlas")) {
                    val atlasJsonFile = File(context.generatedFolder, file.nameWithoutExtension + ".atlas.json")
                    when {
                        file.isDirectory -> {
                            context.skipFiles(file)
                            generate(context.logger, atlasJsonFile, arrayOf(file))
                        }
                        file.isFile -> {
                            val sources = file.readLines().filter { it.isNotBlank() }.map { File(folder, it) }.toTypedArray()
                            context.skipFiles(file, *sources)
                            generate(context.logger, atlasJsonFile, sources)
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
                val results = NewTexturePacker.packImages(*imageFolders, enableRotation = true, enableTrimming = true)
                for (result in results) {
                    val imageOut = result.write(outputFile)
                }
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
