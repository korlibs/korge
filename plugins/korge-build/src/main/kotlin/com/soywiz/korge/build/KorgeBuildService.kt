package com.soywiz.korge.build

import com.soywiz.korau.format.*
import com.soywiz.korge.*
import com.soywiz.korge.build.atlas.*
import com.soywiz.korge.build.lipsync.*
import com.soywiz.korge.build.swf.*
import com.soywiz.korio.file.std.*
import com.soywiz.korio.serialization.*
import com.soywiz.korio.util.*
import kotlinx.coroutines.*
import java.io.*

// Used at korge-gradle-plugin
@Suppress("unused")
class KorgeBuildService {
    fun init() {
        defaultResourceProcessors.register(AtlasResourceProcessor, SwfResourceProcessor)
        try {
            defaultResourceProcessors.register(LipsyncResourceProcessor)
        } catch (e: Throwable) {
            e.printStackTrace()
        }
        Mapper.jvmFallback()
        defaultAudioFormats.registerStandard().registerMp3Decoder().registerOggVorbisDecoder()
    }

    fun version(): String = Korge.VERSION

    fun processResourcesFolder(src: File, dst: File) {
        runBlocking {
            runCatching { dst.mkdirs() }
            ResourceProcessor.process(listOf(src.toVfs()), dst.toVfs())
        }
    }
}
