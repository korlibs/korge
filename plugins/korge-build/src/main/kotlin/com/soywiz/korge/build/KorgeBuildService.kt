package com.soywiz.korge.build

import com.soywiz.korge.resources.*
import com.soywiz.korio.file.std.*
import kotlinx.coroutines.*
import java.io.File
import java.util.logging.*

// Used at korge-gradle-plugin
@Suppress("unused")
object KorgeBuildService {
	fun processResourcesFolders(group: ResourceProcessor.Group, srcDirs: List<File>, dstDir: File, logger: (String) -> Unit = { }) {
		runCatching { dstDir.mkdirs() }

		logger("PROCESSORS:")
		for (processor in group.processorsByExtension) {
			logger(" - $processor")
		}

		for (srcDir in srcDirs) {
			for (srcFile in srcDir.walkTopDown()) {
				if (srcFile.isFile) {
					val dstFile = File(dstDir, srcFile.relativeTo(srcDir).path)
					val srcDirectory = srcFile.parentFile
					val dstDirectory = dstFile.parentFile
					val processor = group.tryGetProcessorByName(srcFile.name)
					if (processor != null) {
						logger("$processor: srcFile=$srcFile -> dstDirectory=$dstDirectory...")
						runBlocking {
							try {
								runCatching { dstDirectory.mkdirs() }

								val srcVfsFile = srcDirectory.toVfs().jail()[srcFile.name]
								val dstVfsDir = dstDirectory.toVfs().jail()

								processor.process(srcVfsFile, dstVfsDir)
							} catch (e: Throwable) {
								e.printStackTrace()
							}
						}
					} else {
						logger("$processor: srcFile=$srcFile")
					}
				}
			}
		}
		//ResourceProcessor.process(srcDirs, dstDir)
	}
}
