package com.soywiz.korau.mod

import com.soywiz.korio.file.*
import com.soywiz.korio.lang.*
import com.soywiz.korio.stream.*
import com.soywiz.korio.file.*

// http://www.onicos.com/staff/iz/formats/guspat.html

suspend fun VfsFile.readGusPatch(): SoundFont = readAll().openSync().readGusPatch()

fun SyncStream.readGusPatch(): SoundFont {
	val magic = readStringz(22, ASCII)
	val description = readStringz(60, ASCII)
	val ninstruments = readU8()
	val voices = readU8()
	val channels = readU8()
	val waveforms = readS16LE()
	val masterVolume = readS16LE()
	val dataSize = readS32LE()
	readBytesExact(36) // reserved
	val instrumentId = readS16LE()
	val instrumentName = readStringz(16, ASCII)
	val instrumentSize = readS32LE()
	val layers = readU8()
	readBytesExact(40) // reserved
	val layerdup = readU8()
	val layer = readU8()
	val layerSize = readS32LE()
	val nsamples = readU8()
	readBytesExact(40) // reserved
	for (n in 0 until nsamples) {
		val name = readStringz(7, ASCII)
		val fractions = readU8()
		val sampleDataSize = readS32LE()
		val loopStart = readS32LE()
		val loopEnd = readS32LE()
		val sampleRate = readU16LE()
		val lowFreq = readS32LE()
		val highFreq = readS32LE()
		val rootFreq = readS32LE()
		val tune = readS16LE()
		val panning = readU8()
		val envelopeRatesOn = readUByteArray(3)
		val envelopeRatesOff = readUByteArray(3)
		val envelopeOffsetsOn = readUByteArray(3)
		val envelopeOffsetsOff = readUByteArray(3)
		val tremoloSweep = readU8()
		val tremoloRate = readU8()
		val tremoloDepth = readU8()

		val vibratoSweep = readU8()
		val vibratoRate = readU8()
		val vibratoDepth = readU8()
		val samplingModes = readU8()
		val scaleFreq = readS16LE()
		val scaleFactor = readS16LE()
		readBytesExact(36) // reserved
		val sampleData = readBytesExact(sampleDataSize)
		Unit
	}
	return object : SoundFont {
		override fun get(patch: Int): SoundPatch {
			TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
		}
	}
}

class GusPatch {

}

/*
GUS/patch format
Byte Order: Little-endian
Offset   Length   Contents
  0     22 bytes  "GF1PATCH110\0ID#000002\0" or
                  "GF1PATCH100\0ID#000002\0"
 22     60 bytes  Discription (in ASCII)
 82      1 byte   Number of instruments (To some patch makers, 0 means 1)
 83      1 byte   Voices (Always 14?)
 84      1 byte   Channels
 85      2 bytes  Waveforms
 87      2 bytes  Master volume [0..127]
 89      4 bytes  Data size
 93     36 bytes  Reserved
129      2 bytes  Instrument ID [0..0xFFFF]
131     16 bytes  Instrument name (in ASCII)
147      4 bytes  Instrument size
151      1 byte   Layers
152     40 bytes  Reserved
192      1 byte   Layer duplicate
193      1 byte   Layer
194      4 bytes  Layer size
198      1 byte   Number of samples
199     40 bytes  Reserved
[ // Samples
         7 bytes  Wave name (in ASCII)
         1 byte   Fractions
                      bit 0..3: Loop offset start fractions [0/16 .. 15/16]
                      bit 4..7: Loop offset end fractions [0/16 .. 15/16]
         4 bytes  Sample data size (s)
         4 bytes  Loop start
         4 bytes  Loop end
         2 bytes  Sample rate
         4 bytes  Low frequency
         4 bytes  High frequency
         4 bytes  Root frequency
         2 bytes  Tune (Always 1, not used anymore)
         1 byte   Panning [0:left .. 15:right]
         3 bytes  Envelope rates (on)   (stage 0,1,2)
         3 bytes  Envelope rates (off)  (stage 3,4,5)
         3 bytes  Envelope offsets (on) (stage 0,1,2)
         3 bytes  Envelope offsets (off)(stage 3,4,5)
                      stage 0: Attack
                            1: Decay
                            2: Sustain
                            3,4,5: Release
         1 byte   Tremolo sweep
         1 byte   Tremolo rate
         1 byte   Tremolo depth
         1 byte   Vibrato sweep
         1 byte   Vibrato rate
         1 byte   Vibrato depth
         1 byte   Sampling modes
                      bit 0: 16-bit (versus 8-bit)
                      bit 1: Unsigned (versus signed)
                      bit 2: Looping
                      bit 3: Pingpong
                      bit 4: Reverse
                      bit 5: Sustein
                      bit 6: Envelope
                      bit 7: Clamped release (6th point of envelope)
         2 bytes  Scale frequency
         2 bytes  Scale factor [0..2048] (1024 is normal)
        36 bytes  Reserved
        (s)bytes  Sample data
]*
Appendix:
Envelope rate table
 */