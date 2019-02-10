package com.soywiz.korge.build

import com.soywiz.korge.build.ResourceProcessor.Companion.processorsByExtension
import com.soywiz.korio.file.std.*
import kotlinx.coroutines.*
import java.io.File

// Used at korge-gradle-plugin
@Suppress("unused")
object KorgeBuildService {
	fun processResourcesFolders(srcDirs: List<File>, dstDir: File) {
		runCatching { dstDir.mkdirs() }
		println("PROCESSORS:")
		for (processor in processorsByExtension) {
			println(" - $processor")
		}

		for (srcDir in srcDirs) {
			for (srcFile in srcDir.walkTopDown()) {
				if (srcFile.isFile) {
					val dstFile = File(dstDir, srcFile.relativeTo(srcDir).path)
					val srcDirectory = srcFile.parentFile
					val dstDirectory = dstFile.parentFile
					val processor = ResourceProcessor.processorsByExtension[srcFile.extension]
					if (processor != null) {
						runBlocking {
							try {
								runCatching { dstDirectory.mkdirs() }

								val srcVfsFile = srcDirectory.toVfs().jail()[srcFile.name]
								val dstVfsDir = dstDirectory.toVfs().jail()

								println("$processor: srcFile=$srcFile -> dstDirectory=$dstDirectory...")
								processor.process(srcVfsFile, dstVfsDir)
							} catch (e: Throwable) {
								e.printStackTrace()
							}
						}
					}
				}
			}
		}
		//ResourceProcessor.process(srcDirs, dstDir)
	}
}
