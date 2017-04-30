package com.soywiz.korge.ext.lipsync

import com.soywiz.korau.format.AudioData
import com.soywiz.korau.format.toWav
import com.soywiz.korio.serialization.json.Json
import com.soywiz.korio.vfs.TempVfs

object RhubarbLipSyncProcessor {
	data class Metadata(val soundFile: String, val duration: Double)
	data class MouthCue(val start: Double, var end: Double, var value: String)
	//data class MouthCue(val start: Double, var end: Double, var value: Char)

	data class RhubarbFile(val metadata: Metadata, val mouthCues: List<MouthCue>) {
		val totalTime: Double by lazy { mouthCues.map { it.end }.max() ?: 0.0 }
		fun findCue(time: Double): MouthCue? = mouthCues.getOrNull(mouthCues.binarySearch { if (time < it.start) +1 else if (time >= it.end) -1 else 0 })

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

	suspend fun process(data: AudioData): RhubarbFile {
		val tempFile = TempVfs()["rhubarb-file.wav"]
		try {
			tempFile.write(data.toWav())
			val result = TempVfs().execToString(listOf("c:\\dev\\rhubarb-lip-sync-1.4.2-win32\\rhubarb.exe", "-f", "json", tempFile.absolutePath))
			return Json.decodeToType<RhubarbFile>(result)
		} finally {
			tempFile.delete()
		}
	}
}
