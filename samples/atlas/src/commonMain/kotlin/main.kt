import com.soywiz.korge.*
import com.soywiz.korge.view.*
import com.soywiz.korim.atlas.*
import com.soywiz.korio.file.std.*

suspend fun main() = Korge(width = 640, height = 480, virtualWidth = 320, virtualHeight = 240) {
	atlasMain()
}

suspend fun Stage.atlasMain() {
	val logos = resourcesVfs["logos.atlas.json"].readAtlas()
	image(logos["korau.png"]).position(0, 0)
	image(logos["korim.png"]).position(64, 32)
	image(logos["korge.png"]).position(128, 64)
}
