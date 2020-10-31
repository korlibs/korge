package com.soywiz.korau.format.opus

import com.soywiz.korau.format.org.concentus.*
import com.soywiz.korau.format.org.concentus.internal.*
import com.soywiz.korau.format.org.gragravarr.opus.*
import com.soywiz.korio.async.*
import com.soywiz.korio.lang.*
import com.soywiz.korio.stream.*
import com.soywiz.korio.file.*
import kotlinx.coroutines.*
import java.io.*

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 * @author lostromb
 */
object Program {
	@JvmStatic fun main(args: Array<String>) {
		test()
	}

	/**
	 * @param args the command line arguments
	 */
	@JvmStatic fun main2(args: Array<String>) {
		//val fileIn = FileInputStream("C:\\Users\\lostromb\\Documents\\Visual Studio 2015\\Projects\\Concentus-git\\AudioData\\48Khz Stereo.raw")
		val fileIn = FileInputStream("/tmp/sample.raw")
		val encoder = OpusEncoder(48000, 2, OpusApplication.OPUS_APPLICATION_AUDIO)
		encoder.bitrate = 96000
		encoder.signalType = OpusSignal.OPUS_SIGNAL_MUSIC
		encoder.complexity = 10

		val fileOut =
			MemorySyncStream()
		val info = OpusInfo()
		info.numChannels = 2
		info.setSampleRate(48000)
		val tags = OpusTags()
		//tags.setVendor("Concentus");
		//tags.addComment("title", "A test!");
		val file = OpusFile(fileOut, info, tags, warningProcessor = { println(it) })
		val packetSamples = 960
		val inBuf = ByteArray(packetSamples * 2 * 2)
		val data_packet = ByteArray(1275)
		val start = System.currentTimeMillis()
		while (fileIn.available() >= inBuf.size) {
			val bytesRead = fileIn.read(inBuf, 0, inBuf.size)
			val pcm = BytesToShorts(inBuf, 0, inBuf.size)
			val bytesEncoded = encoder.encode(pcm, 0, packetSamples, data_packet, 0, 1275)
			val packet = ByteArray(bytesEncoded)
			System.arraycopy(data_packet, 0, packet, 0, bytesEncoded)
			val data = OpusAudioData(packet)
			file.writeAudioData(data)
		}
		file.close()

		val end = System.currentTimeMillis()
		println("Time was " + (end - start) + "ms")
		fileIn.close()
		runBlocking {
			(fileOut.base as MemorySyncStreamBase).data.toByteArray().writeToFile("/tmp/out.bin")
		}
		//fileOut.close();
		println("Done!")
	}

	fun test() {
		try {
			val fileIn =
				//FileInputStream("C:\\Users\\lostromb\\Documents\\Visual Studio 2015\\Projects\\Concentus-git\\AudioData\\48Khz Stereo.raw")
				FileInputStream("/tmp/sample.raw")
			val encoder = OpusEncoder(48000, 2, OpusApplication.OPUS_APPLICATION_AUDIO)
			encoder.bitrate = 96000
			encoder.forceMode = OpusMode.MODE_CELT_ONLY
			encoder.signalType = OpusSignal.OPUS_SIGNAL_MUSIC
			encoder.complexity = 0

			val decoder = OpusDecoder(48000, 2)

			val fileOut =
				//FileOutputStream("C:\\Users\\lostromb\\Documents\\Visual Studio 2015\\Projects\\Concentus-git\\AudioData\\out_j.raw")
				FileOutputStream("/tmp/out_j.raw")
			val packetSamples = 960
			val inBuf = ByteArray(packetSamples * 2 * 2)
			val data_packet = ByteArray(1275)
			val start = System.currentTimeMillis()
			while (fileIn.available() >= inBuf.size) {
				val bytesRead = fileIn.read(inBuf, 0, inBuf.size)
				val pcm = BytesToShorts(inBuf, 0, inBuf.size)
				val bytesEncoded = encoder.encode(pcm, 0, packetSamples, data_packet, 0, 1275)
				//System.out.println(bytesEncoded + " bytes encoded");

				val samplesDecoded = decoder.decode(data_packet, 0, bytesEncoded, pcm, 0, packetSamples, false)
				//System.out.println(samplesDecoded + " samples decoded");
				val bytesOut = ShortsToBytes(pcm)
				fileOut.write(bytesOut, 0, bytesOut.size)
			}

			val end = System.currentTimeMillis()
			println("Time was " + (end - start) + "ms")
			fileIn.close()
			fileOut.close()
			println("Done!")
		} catch (e: IOException) {
			println(e.message)
		} catch (e: Throwable) {
			println(e.message)
		}

	}

	/// <summary>
	/// Converts interleaved byte samples (such as what you get from a capture device)
	/// into linear short samples (that are much easier to work with)
	/// </summary>
	/// <param name="input"></param>
	/// <returns></returns>
	fun BytesToShorts(input: ByteArray, offset: Int = 0, length: Int = input.size): ShortArray {
		val processedValues = ShortArray(length / 2)
		for (c in processedValues.indices) {
			val a = (input[c * 2 + offset].toInt() and 0xFF).toShort()
			val b = (input[c * 2 + 1 + offset].toInt() shl 8).toShort()
			processedValues[c] = (a or b).toShort()
		}

		return processedValues
	}

	/// <summary>
	/// Converts linear short samples into interleaved byte samples, for writing to a file, waveout device, etc.
	/// </summary>
	/// <param name="input"></param>
	/// <returns></returns>
	fun ShortsToBytes(input: ShortArray, offset: Int = 0, length: Int = input.size): ByteArray {
		val processedValues = ByteArray(length * 2)
		for (c in 0 until length) {
			processedValues[c * 2] = (input[c + offset].toInt() and 0xFF).toByte()
			processedValues[c * 2 + 1] = (input[c + offset].toInt() shr 8 and 0xFF).toByte()
		}

		return processedValues
	}
}/// <summary>
/// Converts interleaved byte samples (such as what you get from a capture device)
/// into linear short samples (that are much easier to work with)
/// </summary>
/// <param name="input"></param>
/// <returns></returns>
/// <summary>
/// Converts linear short samples into interleaved byte samples, for writing to a file, waveout device, etc.
/// </summary>
/// <param name="input"></param>
/// <returns></returns>
