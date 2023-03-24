package samples.minesweeper

import korlibs.datastructure.setExtra
import korlibs.event.Key
import korlibs.korge.component.docking.dockedTo
import korlibs.korge.scene.ScaledScene
import korlibs.korge.view.Container
import korlibs.korge.view.SContainer
import korlibs.korge.view.image
import korlibs.image.bitmap.*
import korlibs.math.geom.Anchor
import korlibs.math.geom.ScaleMode
import korlibs.math.geom.slice.*

// Ported from here: https://github.com/soywiz/lunea/tree/master/samples/busca
class MainMineSweeper : ScaledScene(640, 480) {
    override suspend fun SContainer.sceneMain() {
        val scene = this@MainMineSweeper
        this.name = "process.root"
        this.setExtra("scene", scene)
        cancellables += scene.registerProcessSystem()
        MainProcess(this)
    }
}

class MainProcess(parent: Container) : Process(parent) {
	val lights = arrayListOf<RandomLight>()

	override suspend fun main() {
		image(readImage("bg.jpg")).dockedTo(Anchor.TOP_LEFT, ScaleMode.EXACT)
		val light = readImage("light.png")
		val imageSet = readImage("cells.png")
		val images = imageSet.splitInRows(imageSet.height, imageSet.height)
		val click = readSound("click.wav")
		val boom = readSound("boom.wav")

		repeat(20) {
			lights += RandomLight(this, light)
		}

		val board = Board(this, imageSet, images, click, boom, 22, 15, 40)

		while (true) {
			if (key[Key.ESCAPE]) {
				views.gameWindow.close()
			}
			if (key[Key.UP]) {
				lights += RandomLight(this, light)
			}
			if (key[Key.DOWN]) {
				if (lights.isNotEmpty()) {
					lights.removeAt(lights.size - 1).destroy()
				}
			}
			board.updateTimeText()
			frame()
		}
	}
}
