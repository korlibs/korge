@file:Suppress("EXPERIMENTAL_FEATURE_WARNING")

package korlibs.audio.format

import korlibs.datastructure.DoubleArrayList
import korlibs.datastructure.binarySearch
import korlibs.time.TimeSpan
import korlibs.time.measureTimeWithResult
import korlibs.time.microseconds
import korlibs.time.seconds
import korlibs.io.annotations.Keep
import korlibs.io.lang.*
import korlibs.io.stream.AsyncStream
import korlibs.io.stream.openSync
import korlibs.io.stream.readBytesExact
import korlibs.io.stream.readBytesUpTo
import korlibs.io.stream.readStream
import korlibs.io.stream.readString
import korlibs.io.stream.toSyncOrNull
import korlibs.memory.*

@Keep
open class MP3 : MP3Base() {
	companion object : MP3()
}

open class MP3Base : AudioFormat("mp3") {
	override suspend fun tryReadInfo(data: AsyncStream, props: AudioDecodingProps): Info? {
        try {
            val header = data.readBytesExact(4)
            val h0 = header.toUByteArray()[0].toInt()
            val h1 = header.toUByteArray()[1].toInt()
            val h2 = header.toUByteArray()[2].toInt()

            val isId3 = header.readStringz(0, 3) == "ID3"
            val isSync = (h0 == 0xFF) &&
                (((h1 and 0xF0) == 0xF0) || ((h1 and 0xFE) == 0xE2)) &&
                (h1.extract2(1) != 0) &&
                (h2.extract4(4) != 15) &&
                (h2.extract2(2) != 3)

            if (!isId3 && !isSync) return null

            val parser = Parser(data, data.getLength())
            val (duration, decodingTime) = measureTimeWithResult {
                when (props.exactTimings) {
                    null -> parser.getDurationExact() // Try to guess what's better based on VBR?
                    true -> parser.getDurationExact()
                    else -> parser.getDurationEstimate()
                }
            }
            return Info(duration, parser.info?.channelMode?.channels ?: 2, decodingTime)
        } catch (e: Throwable) {
            //e.printStackTrace()
            return null
        }
    }

    class SeekingTable(
        val microseconds: DoubleArrayList,
        val filePositions: DoubleArrayList,
        val rate: Int = 44100
    ) {
        val lengthTime: TimeSpan get() = microseconds[microseconds.size - 1].microseconds
        val lengthSamples: Long get() = (lengthTime.seconds * rate).toLong()

        fun locate(time: TimeSpan): Long {
            val searchMicro = time.microseconds
            val result = microseconds.binarySearch(searchMicro)
            return filePositions[result.nearIndex].toLong()
        }

        fun locateSample(sample: Long): Long {
            return locate((sample.toDouble() / rate).seconds)
        }
    }

	class Parser(val data: AsyncStream, val dataLength: Long) {
		var info: Mp3Info? = null

		//Read first mp3 frame only...  bind for CBR constant bit rate MP3s
		suspend fun getDurationEstimate() = _getDuration(use_cbr_estimate = true)

		suspend fun getDurationExact() = _getDuration(use_cbr_estimate = false)

        suspend fun getSeekingTable(rate: Int = 44100): SeekingTable {
            val times = DoubleArrayList()
            val filePositions = DoubleArrayList()
            _getDuration(use_cbr_estimate = false, emit = { filePos, totalMicro, info ->
                times.add(totalMicro)
                filePositions.add(filePos.toDouble())
            })
            return SeekingTable(times, filePositions, rate)
        }

		//Read entire file, frame by frame... ie: Variable Bit Rate (VBR)
		private suspend fun _getDuration(use_cbr_estimate: Boolean, emit: ((filePosition: Long, totalMicroseconds: Double, info: Mp3Info) -> Unit)? = null): TimeSpan {
			data.position = 0
			val fd = data.duplicate()
            val len = fd.getLength()

			var durationMicroseconds = 0.0
			val offset = this.skipID3v2Tag(fd.readStream(100))
            var pos = offset

			var info: Mp3Info? = null

            var nframes = 0
            val block2 = UByteArrayInt(ByteArray(10))

            val fdbase = fd.base
            val fdsync = fdbase.toSyncOrNull()

            var nreads = 0
            var nskips = 0
            var nasync = 0

            //println("fdbase: $fdbase")

            try {
                while (pos < len) {
                    val block2Size = when {
                        fdsync != null -> fdsync.read(pos, block2.bytes, 0, 10)
                        else -> {
                            nasync++
                            fd.position = pos
                            fd.readBytesUpTo(block2.bytes, 0, 10)
                        }
                    }
                    nreads++
                    if (block2Size < 10) break
                    pos += block2Size

                    when {
                        block2[0] == 0xFF && ((block2[1] and 0xe0) != 0) -> {
                            val framePos = fd.position
                            info = parseFrameHeader(block2)
                            emit?.invoke(framePos, durationMicroseconds, info)
                            nframes++
                            //println("FRAME: $nframes")
                            this.info = info
                            if (info.frameSize == 0) return durationMicroseconds.microseconds
                            pos += info.frameSize - 10
                            durationMicroseconds += (info.samples * 1_000_000L) / info.samplingRate
                        }
                        block2.bytes.openSync().readString(3) == "TAG" -> {
                            pos += 128 - 10 //skip over id3v1 tag size
                        }
                        else -> {
                            pos -= 9
                            nskips++
                        }
                    }

                    if ((info != null) && use_cbr_estimate) {
                        return estimateDuration(info.bitrate, info.channelMode.channels, offset.toInt()).microseconds
                    }
                }
            } finally {
                //println("MP3.Parser._getDuration: nreads=$nreads, nskips=$nskips, nasync=$nasync")
                //printStackTrace()
            }
			return durationMicroseconds.microseconds
		}

		private suspend fun estimateDuration(bitrate: Int, channels: Int, offset: Int): Long {
			val kbps = (bitrate * 1_000) / 8
			val dataSize = dataLength - offset
			return dataSize * (2 / channels) * 1_000_000L / kbps
		}

		private suspend fun skipID3v2Tag(block: AsyncStream): Long {
			val b = block.duplicate()

			if (b.readString(3, Charsets.LATIN1) == "ID3") {
                val bb = b.readBytesExact(7)
                val id3v2_major_version = bb.getU8(0)
                val id3v2_minor_version = bb.getU8(1)
                val id3v2_flags = bb.getU8(2)
                val z0 = bb.getU8(3)
                val z1 = bb.getU8(4)
                val z2 = bb.getU8(5)
                val z3 = bb.getU8(6)

                val flag_unsynchronisation = id3v2_flags.extract(7)
                val flag_extended_header = id3v2_flags.extract(6)
                val flag_experimental_ind = id3v2_flags.extract(5)
                val flag_footer_present = id3v2_flags.extract(4)

                if (((z0 and 0x80) == 0) && ((z1 and 0x80) == 0) && ((z2 and 0x80) == 0) && ((z3 and 0x80) == 0)) {
                    val header_size = 10
                    val tag_size =
                        ((z0 and 0x7f) * 2097152) + ((z1 and 0x7f) * 16384) + ((z2 and 0x7f) * 128) + (z3 and 0x7f)
                    val footer_size = if (flag_footer_present) 10 else 0
                    return (header_size + tag_size + footer_size).toLong()//bytes to skip
                }
            }
			return 0L
		}

		companion object {
            suspend operator fun invoke(data: AsyncStream) = Parser(data, data.getLength())

			enum class ChannelMode(val id: Int, val channels: Int) {
				STEREO(0b00, 2),
				JOINT_STEREO(0b01, 1),
				DUAL_CHANNEL(0b10, 2),
				SINGLE_CHANNEL(0b11, 1);

				companion object {
					val BY_ID = values().map { it.id to it }.toMap()
				}
			}

			val versions = arrayOf("2.5", "x", "2", "1")
			val layers = intArrayOf(-1, 3, 2, 1)

			val bitrates: Map<Int, IntArray> = mapOf(
                getBitrateKey(1, 1) to intArrayOf(0, 32, 64, 96, 128, 160, 192, 224, 256, 288, 320, 352, 384, 416, 448),
                getBitrateKey(1, 2) to intArrayOf(0, 32, 48, 56, 64, 80, 96, 112, 128, 160, 192, 224, 256, 320, 384),
                getBitrateKey(1, 3) to intArrayOf(0, 32, 40, 48, 56, 64, 80, 96, 112, 128, 160, 192, 224, 256, 320),
                getBitrateKey(2, 1) to intArrayOf(0, 32, 48, 56, 64, 80, 96, 112, 128, 144, 160, 176, 192, 224, 256),
                getBitrateKey(2, 2) to intArrayOf(0, 8, 16, 24, 32, 40, 48, 56, 64, 80, 96, 112, 128, 144, 160),
                getBitrateKey(2, 3) to intArrayOf(0, 8, 16, 24, 32, 40, 48, 56, 64, 80, 96, 112, 128, 144, 160)
			)

            fun getBitrateKey(version: Int, layer: Int): Int {
                return version * 10 + layer
            }

			val sampleRates = mapOf(
				"1" to intArrayOf(44100, 48000, 32000),
				"2" to intArrayOf(22050, 24000, 16000),
				"2.5" to intArrayOf(11025, 12000, 8000)
			)

			val samples = mapOf(
				1 to mapOf(1 to 384, 2 to 1152, 3 to 1152), // MPEGv1,     Layers 1,2,3
				2 to mapOf(1 to 384, 2 to 1152, 3 to 576)   // MPEGv2/2.5, Layers 1,2,3
			)

			data class Mp3Info(
				val version: String,
				val layer: Int,
				val bitrate: Int,
				val samplingRate: Int,
				val channelMode: ChannelMode,
				val frameSize: Int,
				val samples: Int
			)

			fun parseFrameHeader(f4: UByteArrayInt): Mp3Info {
				val b0 = f4[0]
				val b1 = f4[1]
				val b2 = f4[2]
				val b3 = f4[3]
				if (b0 != 0xFF) invalidOp

				val version = versions[b1.extract(3, 2)]
				val simple_version = if (version == "2.5") 2 else version.toInt()

				val layer = layers[b1.extract(1, 2)]

				val protection_bit = b1.extract(0, 1)
				val bitrate_key = getBitrateKey(simple_version, layer)
				val bitrate_idx = b2.extract(4, 4)

				val bitrate = bitrates[bitrate_key]?.getOrNull(bitrate_idx) ?: 0
				val sample_rate = sampleRates[version]?.getOrNull(b2.extract(2, 2)) ?: 0
				val padding_bit = b2.extract(1, 1)
				val private_bit = b2.extract(0, 1)
				val channelMode = ChannelMode.BY_ID[b3.extract(6, 2)]!!
				val mode_extension_bits = b3.extract(4, 2)
				val copyright_bit = b3.extract(3, 1)
				val original_bit = b3.extract(2, 1)
				val emphasis = b3.extract(0, 2)

				return Mp3Info(
					version = version,
					layer = layer,
					bitrate = bitrate,
					samplingRate = sample_rate,
					channelMode = channelMode,
					frameSize = this.framesize(layer, bitrate, sample_rate, padding_bit),
					samples = samples[simple_version]?.get(layer) ?: 0
				)
			}

			private fun framesize(layer: Int, bitrate: Int, sample_rate: Int, padding_bit: Int): Int {
                if (sample_rate == 0) error("division by 0")
				return if (layer == 1) {
					((12 * bitrate * 1000 / sample_rate) + padding_bit) * 4
				} else {
					//layer 2, 3
					((144 * bitrate * 1000) / sample_rate) + padding_bit
				}
			}
		}
	}
}
