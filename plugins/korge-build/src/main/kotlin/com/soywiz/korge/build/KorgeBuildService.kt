package com.soywiz.korge.build

import com.soywiz.korge.build.ResourceProcessor.Companion.processorsByExtension
import com.soywiz.korio.file.std.toVfs
import kotlinx.coroutines.runBlocking
import java.io.File

// Used at korge-gradle-plugin
@Suppress("unused")
object KorgeBuildService {
	//override fun version(): String = Korge.VERSION
	//fun korgeVersion(): String = BuildVersions.KORGE
	//fun kormaVersion(): String = BuildVersions.KORMA
	//fun korioVersion(): String = BuildVersions.KORIO
	//fun korimVersion(): String = BuildVersions.KORIM
	//fun korauVersion(): String = BuildVersions.KORAU
	//fun koruiVersion(): String = BuildVersions.KORUI
	//fun korevVersion(): String = BuildVersions.KOREV
	//fun korgwVersion(): String = BuildVersions.KORGW
	//fun kotlinVersion(): String = BuildVersions.KOTLIN

	fun processResourcesFolder(src: File, dst: File) {
		if (!src.exists()) return // Ignore empty folders

		runBlocking {
			runCatching { dst.mkdirs() }
			println("PROCESSORS:")
			for (processor in processorsByExtension) {
				println(" - $processor")
			}
			println("FILES:")
			val s = src.toVfs().jail()
			val d = dst.toVfs().jail()

			ResourceProcessor.process(listOf(s), d)

			/*
            println("$s -> $d")
            for (file in s.listRecursive()) {
                if (!file.isDirectory()) {
                    println(" -- $file")
                    ResourceProcessor.process(listOf(file), d)
                }
            }
            */
		}
	}
}

//interface IKorgeBuildService {
//    fun init()
//    fun korgeVersion(): String
//    fun kormaVersion(): String
//    fun korioVersion(): String
//    fun korimVersion(): String
//    fun korauVersion(): String
//    fun koruiVersion(): String
//    fun korevVersion(): String
//    fun korgwVersion(): String
//    fun kotlinVersion(): String
//    fun processResourcesFolder(src: File, dst: File)
//}
