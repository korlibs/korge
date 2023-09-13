package korlibs.audio.format.mp3

import korlibs.audio.format.AudioFormat
import korlibs.audio.sound.NativeMp3DecoderAudioFormat

actual val FastMP3Decoder: AudioFormat get() = NativeMp3DecoderAudioFormat
