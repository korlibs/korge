package com.soywiz.korge.build

import com.soywiz.korge.*
import com.soywiz.korge.build.ResourceProcessor.Companion.processorsByExtension
import com.soywiz.korio.file.std.*
import kotlinx.coroutines.*
import java.io.*

// Used at korge-gradle-plugin
@Suppress("unused")
class KorgeBuildService : IKorgeBuildService {
    override fun init() {
        KorgeManualServiceRegistration.register()
    }

    //override fun version(): String = Korge.VERSION
    override fun korgeVersion(): String = BuildVersions.KORGE
    override fun kormaVersion(): String = BuildVersions.KORMA
    override fun korioVersion(): String = BuildVersions.KORIO
    override fun korimVersion(): String = BuildVersions.KORIM
    override fun korauVersion(): String = BuildVersions.KORAU
    override fun koruiVersion(): String = BuildVersions.KORUI

    override fun processResourcesFolder(src: File, dst: File) {
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

interface IKorgeBuildService {
    fun init()
    fun korgeVersion(): String
    fun kormaVersion(): String
    fun korioVersion(): String
    fun korimVersion(): String
    fun korauVersion(): String
    fun koruiVersion(): String
    fun processResourcesFolder(src: File, dst: File)
}
