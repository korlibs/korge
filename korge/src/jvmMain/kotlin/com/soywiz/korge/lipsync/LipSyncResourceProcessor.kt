package com.soywiz.korge.lipsync

import com.soywiz.korau.format.*
import com.soywiz.korau.format.mp3.FastMP3Decoder
import com.soywiz.korau.sound.AudioData
import com.soywiz.korau.sound.readAudioData
import com.soywiz.korge.resources.ResourceProcessor
import com.soywiz.korio.dynamic.mapper.Mapper
import com.soywiz.korio.dynamic.serialization.parseTyped
import com.soywiz.korio.file.VfsFile
import com.soywiz.korio.file.baseName
import com.soywiz.korio.file.std.openAsZip
import com.soywiz.korio.file.std.tempVfs
import com.soywiz.korio.serialization.json.Json
import com.soywiz.korio.util.AsyncOnce
import com.soywiz.korio.util.OS
import com.soywiz.korio.util.basename
import com.soywiz.korio.util.jvmFallback
import com.soywiz.krypto.encoding.*
import com.soywiz.krypto.sha1
import java.io.File
import java.net.URL
import java.util.*

open class LipsyncResourceProcessor : ResourceProcessor("voice.wav", "voice.mp3", "voice.ogg") {
	companion object : LipsyncResourceProcessor() {
		private val nativeAudioFormats = AudioFormats().register(
            WAV, FastMP3Decoder,
			//ServiceLoader.load(com.soywiz.korau.format.AudioFormat::class.java).toList()
		)
	}

	override val version: Int = 0
	override val outputExtension: String = "lipsync"

	override suspend fun processInternal(inputFile: VfsFile, outputFile: VfsFile) {
		inputFile.copyTo(outputFile.parent[inputFile.baseName])
		outputFile.writeString(processAudioData(inputFile.readAudioData(nativeAudioFormats)).toLipString())
	}

	data class Config(val url: URL, val folder: String, val sha1: ByteArray, val exe: String)

	val config by lazy {
        val base = Config(
            URL("https://github.com/korlibs/korge-tools/releases/download/rhubarb-lip-sync-1.13/Rhubarb-Lip-Sync-1.13.0-all.zip"),
            "rhubarb-lip-sync-1.13.0-all",
            "6A1850D829D361E36C09D7561C817C47C2D16FD0".unhex,
            "rhubarb"
        )
		when {
			OS.isMac -> base.copy(exe = "rhubarb-mac")
			OS.isLinux -> base.copy(exe = "rhubarb-linux")
			OS.isWindows -> base.copy(exe = "rhubarb-win.exe")
			else -> error("Operating system '${OS.rawName}', '${OS.platformName}' not supported")
		}
	}


	data class Tool(val rhubarb: VfsFile)

	private val toolCache = AsyncOnce<Tool>()

	suspend fun getRhubarbTool(): Tool = toolCache {
		val toolsRoot = BINARY_ROOT
		val rootOutputFolder = toolsRoot
		val outputFolder = toolsRoot[config.folder]
		val localZipFile = toolsRoot[config.url.basename]

		if (!outputFolder.exists()) {
			if (!localZipFile.exists()) {
				println("Downloading ${config.url} ...")
				localZipFile.writeBytes(config.url.openStream().use {
					val data = it.readBytes()
					val expectedSha1 = config.sha1.hex
					val actualSha1 = data.sha1().hex
					if (expectedSha1 != actualSha1) {
						error("Downloaded file ${config.url} sha1 $actualSha1 doesn't match the expected sha1 $expectedSha1")
					}
					data
				})
			}


			//val mem = MemoryVfs()
			println("Extracting $localZipFile ...")
			val zip = localZipFile.openAsZip()
			//localZipFile.openAsZip().copyToTree(rootOutputFolder)
			zip.copyToTree(rootOutputFolder)

			println("Done")

			//val executableFile = File(rootOutputFolder[config.exe].absolutePath)

			//println("Making executable $executableFile ...")
			//executableFile.setExecutable(true, false)
		}



		//val zip = LocalVfs("c:/temp/rhubarb-lip-sync-1.4.2-win32.zip").openAsZip()
		//println(zip["rhubarb-lip-sync-1.4.2-osx"].exists())
		//println(zip.list().toList())
		//zip.copyToTree(KorgeBuildTools.BINARY_ROOT())

		return@toolCache Tool(outputFolder[config.exe])
	}

	fun VfsFile.toJvmFile() = File(this.absolutePath)

	suspend fun processWav(wavFile: VfsFile): String {
		val rhubarb = getRhubarbTool().rhubarb

		rhubarb.toJvmFile().setExecutable(true, false)
		val result = rhubarb.parent.execToString(rhubarb.absolutePath, wavFile.absolutePath)
		return result
	}

	suspend fun processAudioData(data: AudioData): RhubarbFile {
		val rhubarb = getRhubarbTool().rhubarb
		val tempFile = tempVfs["rhubarb-file.wav"]
		try {
			tempFile.write(data.toWav())
			rhubarb.toJvmFile().setExecutable(true, false)
			val result = tempVfs.execToString(listOf(rhubarb.absolutePath, "-f", "json", tempFile.absolutePath))
			Mapper.jvmFallback()
			return Json.parseTyped<RhubarbFile>(result, Mapper)
		} finally {
			tempFile.delete()
		}
	}

	data class Metadata(val soundFile: String, val duration: Double)
	data class MouthCue(val start: Double, var end: Double, var value: String)
	//data class MouthCue(val start: Double, var end: Double, var value: Char)

	data class RhubarbFile(val metadata: Metadata, val mouthCues: List<MouthCue>) {
		val totalTime: Double by lazy { mouthCues.map { it.end }.maxOrNull() ?: 0.0 }
		fun findCue(time: Double): MouthCue? =
			mouthCues.getOrNull(mouthCues.binarySearch { if (time < it.start) +1 else if (time >= it.end) -1 else 0 })

		fun toLipString(): String {
			var out = ""
			val totalMs = (totalTime * 1000).toInt()
			for (ms in 0 until totalMs step 16) {
				val s = findCue(ms.toDouble() / 1000)
				out += s?.value ?: "X"
			}
			return out
		}
	}
}
