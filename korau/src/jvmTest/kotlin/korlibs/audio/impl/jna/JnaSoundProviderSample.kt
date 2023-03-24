package korlibs.audio.impl.jna

import korlibs.time.seconds
import korlibs.audio.format.AudioDecodingProps
import korlibs.audio.format.mp3.MP3Decoder
import korlibs.audio.format.readSoundInfo
import korlibs.audio.sound.SoundChannelGroup
import korlibs.audio.sound.attachTo
import korlibs.audio.sound.await
import korlibs.audio.sound.playbackTimes
import korlibs.audio.sound.readAudioStream
import korlibs.audio.sound.readSound
import korlibs.io.file.VfsFile
import korlibs.io.file.fullName
import korlibs.io.file.std.MemoryVfs
import korlibs.io.file.std.resourcesVfs
import korlibs.io.stream.openAsync
import kotlinx.coroutines.runBlocking

fun ByteArray.asMemoryVfsFile(name: String = "temp.bin"): VfsFile = MemoryVfs(mapOf(name to openAsync()))[name]
suspend fun VfsFile.cachedToMemory(): VfsFile = this.readAll().asMemoryVfsFile(this.fullName)

object JnaSoundProviderSample {
    @JvmStatic
    fun main(args: Array<String>) {
        runBlocking {
            //val data = resourcesVfs["monkey_drama.mp3"].readNativeMusic()
            val group = SoundChannelGroup(volume = 0.2)
            println(resourcesVfs["monkey_drama.mp3"].cachedToMemory().readSoundInfo(props = AudioDecodingProps(exactTimings = false))?.decodingTime)
            println(resourcesVfs["monkey_drama.mp3"].cachedToMemory().readSoundInfo(props = AudioDecodingProps(exactTimings = true))?.decodingTime)
            //val data = resourcesVfs["mp31_joint_stereo_vbr.mp3"].readNativeMusic()
            //val data = resourcesVfs["mp31_joint_stereo_vbr.mp3"].readNativeMusic()
            val stream = resourcesVfs["monkey_drama.mp3"].readAudioStream(MP3Decoder)
            println(resourcesVfs["monkey_drama.mp3"].readAll().asMemoryVfsFile("temp.mp3").readSoundInfo(MP3Decoder)!!.duration)
            //val data = resourcesVfs["mp31_joint_stereo_vbr.mp3"].readNativeMusic()
            //val data = resourcesVfs["click.wav"].readNativeMusic()
            //val data = resourcesVfs["click.wav"].readMusic()
            val data = resourcesVfs["click.wav"].readSound()
            //val data = resourcesVfs["click.mp3"].readMusic()
            //val data = resourcesVfs["monkey_drama.mp3"].readNativeSound()
            //val data = resourcesVfs["mp31_joint_stereo_vbr.mp3"].readNativeSound()

            //println(data.length)
            //val result = data.playForever().attachTo(group)
            println(data.length)
            //val result = data.play(2.playbackTimes, startTime = 50.2.seconds).attachTo(group)
            val result = data.play(10.playbackTimes, startTime = 0.seconds).attachTo(group)
            //result.current = 50.seconds
            println(result.total)
            //group.volume = 0.2

            //group.pitch = 1.5
            group.volume = 0.4
            group.pitch = 1.5
            for (n in -10 .. +10) {
                group.panning = n.toDouble() / 10.0
                //group.panning = -1.0
                //result.panning = n.toDouble() / 10.0
                println(group.panning)
                korlibs.io.async.delay(0.1.seconds)
            }
            korlibs.io.async.delay(2.seconds)
            println("Waiting...")
            group.await()
            println("Stop...")
            //korlibs.io.async.delay(1.seconds)
            group.stop()
        }
    }
}