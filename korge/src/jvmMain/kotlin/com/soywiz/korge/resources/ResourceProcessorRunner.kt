package com.soywiz.korge.resources

import com.soywiz.korge.plugin.*
import com.soywiz.korio.file.std.*
import kotlinx.coroutines.*
import java.io.*
import java.util.*
import java.util.logging.*
import kotlin.collections.LinkedHashMap

@Suppress("unused")
object ResourceProcessorRunner {
	val logger = Logger.getLogger("ResourceProcessorRunner")

    @JvmStatic
    fun printPlugins(classLoader: ClassLoader) {
        println("KORGE ResourceProcessors:")
        try {
            for (processor in ServiceLoader.load(ResourceProcessor::class.java, classLoader).toList().filterNotNull()) {
                println(" - ${processor::class.qualifiedName}")
            }
        } catch (e: Throwable) {
            println(" - ERROR: ${e.message}")
        }
        println("KORGE Plugins:")
        try {
            for (plugin in ServiceLoader.load(KorgePluginExtension::class.java, classLoader).toList().filterNotNull()) {
                println(" - ${plugin::class.qualifiedName}")
            }
        } catch (e: Throwable) {
            println(" - ERROR: ${e.message}")
        }
    }

	@JvmStatic
	fun run(classLoader: ClassLoader, foldersMain: List<String>, outputMain: String, kind: String) {
		logger.info("ResourceProcessorRunner:")
		logger.info("Free memory: ${Runtime.getRuntime().freeMemory()}")
		logger.info("Resource Processors:")
		val processors = ResourceProcessorContainer(ServiceLoader.load(ResourceProcessor::class.java, classLoader).toList())
		for (processor in processors.processors) {
			@Suppress("SENSELESS_COMPARISON")
			if (processor != null) {
				logger.info(" - ${processor::class.qualifiedName}")
			}
		}
		logger.info("Plugins:")
		val plugins = ServiceLoader.load(KorgePluginExtension::class.java, classLoader).toList()
		for (plugin in plugins) {
			logger.info(" - $plugin")
		}
		runBlocking {
			handle(kind, foldersMain, outputMain, processors)
			//handle("Test", foldersTest, outputTest, processors)
		}
	}

	class ResourceProcessorContainer(val processors: List<ResourceProcessor>) {
		val processorsByExt: Map<String, ResourceProcessor> = LinkedHashMap<String, ResourceProcessor>().also {
			for (proc in processors) {
				for (ext in proc.extensionLCs) {
					it[ext] = proc
				}
			}
		}
	}

	suspend fun handle(kind: String, folders: List<String>, output: String, processors: ResourceProcessorContainer) {
		logger.info("folders$kind[$output]:")
		for (folder in folders.map { File(it) }) {
			logger.info("- $folder")
			if (folder.exists()) {
				for (file in folder.walkTopDown()) {
					val ext = file.extension.toLowerCase()
					val processor = processors.processorsByExt[ext]
					logger.info("   - $file")
					if (processor != null) {
						val relativeFile = file.absoluteFile.relativeTo(folder)
						val outputFile = File(File(output), relativeFile.path)
						outputFile.parentFile.mkdirs()
						//println("       - $processor - $outputRelativeFolder - $outputFile")
						//outputFile.parentFile.mkdirs()
						processor.process(file.absoluteFile.toVfs(), outputFile.toVfs())
					}
				}
			}
		}
	}
}
