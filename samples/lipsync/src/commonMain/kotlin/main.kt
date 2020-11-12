import com.soywiz.klock.*
import com.soywiz.korev.*
import com.soywiz.korge.*
import com.soywiz.korge.input.*
import com.soywiz.korge.lipsync.*
import com.soywiz.korge.view.*
import com.soywiz.korim.atlas.*
import com.soywiz.korio.async.*
import com.soywiz.korio.file.std.*

suspend fun main() = Korge {
	val atlas = resourcesVfs["lips.atlas.json"].readAtlas()
	val lips = image(atlas["lisa-A.png"])
	val lips2 = image(atlas["lisa-A.png"]).position(400, 0)
	addEventListener<LipSyncEvent> {
		println(it)
		if (it.name == "lisa") {
			lips2.bitmap = atlas["lisa-${it.lip}.png"]
		}
	}
	var playing = true
	fun play() = launchImmediately {
		fun handler(event: LipSyncEvent) {
			views.dispatch(event)
			lips.bitmap = atlas["lisa-${event.lip}.png"]
			playing = event.time > 0.milliseconds
		}

		resourcesVfs["001.voice.wav"].readVoice().play("lisa") { handler(it) }
		//resourcesVfs["002.voice.wav"].readVoice().play("lisa") { handler(it) }
		//resourcesVfs["003.voice.wav"].readVoice().play("lisa") { handler(it) }
		//resourcesVfs["004.voice.wav"].readVoice().play("lisa") { handler(it) }
		//resourcesVfs["simple.voice.mp3"].readVoice().play("lisa") { handler(it) }
	}

	onClick {
		if (!playing) play()
	}
	play()
}
