package com.soywiz.korge.build

import com.soywiz.korio.file.*
import com.soywiz.korio.file.std.*
import com.soywiz.korio.lang.runIgnoringExceptions

val defaultResourceProcessors = ResourceProcessors()

class ResourceProcessors {
	val processors = ArrayList<ResourceProcessor>()

	fun register(vararg processors: ResourceProcessor) = this.apply {
		this@ResourceProcessors.processors += processors
	}
}

abstract class ResourceProcessor(vararg extensions: String) {
	abstract val version: Int
	abstract val outputExtension: String

	val extensionLCs = extensions.toList().map(String::toLowerCase)
	protected abstract suspend fun processInternal(inputFile: VfsFile, outputFile: VfsFile): Unit

	suspend fun requireRegeneration(file: VfsFile, outputFolder: VfsFile): Boolean {
		return _checkAndProcess(file, outputFolder, doProcess = false)
	}

	suspend fun process(file: VfsFile, outputFolder: VfsFile): Boolean {
		return _checkAndProcess(file, outputFolder, doProcess = true)
	}

	private suspend fun _checkAndProcess(file: VfsFile, outputFolder: VfsFile, doProcess: Boolean): Boolean {
		val inputFile = file
		val outputFile = outputFolder[file.fullName].withExtension(outputExtension)
		val metaFile = outputFile.appendExtension("meta")
		val metaInfo = ResourceVersion.fromFile(inputFile, version)
		val newMetaInfo = if (metaFile.exists()) ResourceVersion.readMeta(metaFile) else null

		if (metaInfo == newMetaInfo && outputFile.exists()) {
			return false
		}
		if (doProcess) {
			try {
				processInternal(inputFile, outputFile)
			} catch (e: Throwable) {
				e.printStackTrace()
			} finally {
				metaInfo.writeMeta(metaFile)
			}
		}
		return true
	}

	data class Task(val fileInput: VfsFile, val folderOutput: VfsFile, val processor: ResourceProcessor)

	open class ProgressReport(val tasks: List<Task>) {
		var file: String = ""
		var currentFile: Int = 0
		val totalFiles: Int get() = tasks.size
		val fraction: Double get() = if (totalFiles > 0) currentFile.toDouble() / totalFiles.toDouble() else 0.0
	}

	companion object {
		val processorsByExtension: Map<String, ResourceProcessor> by lazy {
			try {
				defaultResourceProcessors.processors.flatMap { processor -> processor.extensionLCs.map { it to processor } }
					.toMap()
			} catch (e: Throwable) {
				e.printStackTrace()
				mapOf<String, ResourceProcessor>()
			}
		}

		suspend fun process(
			inputFiles: List<VfsFile>,
			outputVfs: VfsFile,
			extraOutVfs: VfsFile? = null,
			progressHandler: (ProgressReport) -> Unit = {}
		) {
			val outputVfsJail = outputVfs.jail()
			val extraOutVfsJail = extraOutVfs?.jail()

			val tasks = ArrayList<Task>()
			val progress = ProgressReport(tasks)

			for (inputFile in inputFiles) {
				val inputVfs = inputFile.jail()
				for (file in inputVfs.listRecursive()) {
					//println(file)
					val processor =
						processorsByExtension[file.compoundExtensionLC] ?: processorsByExtension[file.extensionLC]
						?: continue
					val fileInput = file
					val folderOutput = outputVfsJail[file.path].parent

					println("  - Processing: $processor: ${fileInput.absolutePath}")

					runIgnoringExceptions { folderOutput.ensureParents() }
					runIgnoringExceptions { folderOutput.mkdir() }
					if (processor.requireRegeneration(fileInput, folderOutput)) {
						tasks += Task(fileInput, folderOutput, processor)
					}
				}
			}

			for ((index, task) in tasks.withIndex()) {
				try {
					progress.currentFile = index
					progress.file = task.fileInput.path.trim('/')
					progressHandler(progress)

					val processor = task.processor
					val fileInput = task.fileInput
					val folderOutput = task.folderOutput

					println("Processing: $fileInput with $processor with output to $folderOutput")
					try {
						val log = LogVfs(folderOutput)
						processor.process(fileInput, log.root)
						println(" - Log: ${log.log}")
						println(" - Modified: ${log.modifiedFiles}")
						for (path in log.modifiedFiles) {
							println(" - Modified: $path (copying to $extraOutVfsJail)")
							if (extraOutVfsJail != null) {
								val src = folderOutput[path]
								val dst = extraOutVfsJail[path]
								src.copyTo(dst)
								println(" - Copying from: $src to $dst")
							}
						}
						//logger.info(file.toString())
					} catch (e: Throwable) {
						e.printStackTrace()
					}
				} catch (e: Throwable) {
					e.printStackTrace()
				}
			}

			progress.currentFile = progress.totalFiles
			progressHandler(progress)
		}
	}
}
