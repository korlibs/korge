package korlibs.korge.gradle.processor

import korlibs.korge.gradle.texpacker.*
import korlibs.korge.gradle.util.*
import java.io.*
import kotlin.system.*

open class KorgeTexturePacker : KorgeResourceProcessor {
    override fun processFolder(context: KorgeResourceProcessorContext) {
        context.resourceFolders
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
                            val settings = parseAtlasSettingsFile(file)
                            val sources = settings.files.map { File(folder, it) }.toTypedArray()
                            context.skipFiles(file, *sources)
                            generate(
                                context.logger,
                                atlasJsonFile,
                                sources,
                                enableRotation = settings.enableRotation,
                                enableTrimming = settings.enableTrimming,
                                padding = settings.padding,
                            )
                        }
                    }
                }
            }
        }
    }

    private fun generate(
        logger: org.slf4j.Logger,
        outputFile: File,
        imageFolders: Array<File>,
        enableRotation: Boolean = true,
        enableTrimming: Boolean = true,
        padding: Int = 2,
    ) {
        val involvedFiles = NewTexturePacker.getAllFiles(*imageFolders)
        //val maxLastModifiedTime = involvedFiles.maxOfOrNull { it.file.lastModified() } ?: System.currentTimeMillis()
        val involvedString = involvedFiles.map { it.relative.name + ":" + it.file.length() + ":" + it.file.lastModified() }.sorted().joinToString("\n")
        val involvedFile = File(outputFile.parentFile, "." + outputFile.name + ".info")

        //if (!outputFile.exists() || involvedFile.takeIfExists()?.readText() != involvedString) {
        if (involvedFile.takeIfExists()?.readText() != involvedString) {
            val time = measureTimeMillis {
                val results = NewTexturePacker.packImages(
                    *imageFolders,
                    enableRotation = enableRotation,
                    enableTrimming = enableTrimming,
                    padding = padding
                )
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

    private fun parseAtlasSettingsFile(atlasFile: File): AtlasGenerationSettings {
        var enableRotation = true
        var enableTrimming = true
        var padding = 2
        val files = mutableListOf<String>()

        atlasFile.forEachLine { line ->
            val trimmedLine = line.trim()
            if (trimmedLine.isBlank()) return@forEachLine

            val enableRotationSetting = tryExtractSetting(trimmedLine, "#enable-rotation") { it.toBoolean() }
            if (enableRotationSetting != null) {
                enableRotation = enableRotationSetting
                return@forEachLine
            }

            val enableTrimmingSetting = tryExtractSetting(trimmedLine, "#enable-trimming") { it.toBoolean() }
            if (enableTrimmingSetting != null) {
                enableTrimming = enableTrimmingSetting
                return@forEachLine
            }

            val paddingSetting = tryExtractSetting(trimmedLine, "#padding") { it.toInt() }
            if (paddingSetting != null) {
                padding = paddingSetting
                return@forEachLine
            }

            files.add(trimmedLine)
        }

        return AtlasGenerationSettings(
            files = files,
            enableRotation = enableRotation,
            enableTrimming = enableTrimming,
            padding = padding,
        )
    }

    private inline fun <T> tryExtractSetting(line: String, key: String, valueParser: (String) -> T): T? {
        return if (line.startsWith(key)) {
            try {
                val value = line.substringAfter("=").trim()
                valueParser(value)
            } catch (e: Exception) {
                null
            }
        } else {
            null
        }
    }
}

private data class AtlasGenerationSettings(
    val files: List<String>,
    val enableRotation: Boolean,
    val enableTrimming: Boolean,
    val padding: Int,
)
