package samples.pong

import korlibs.korge.input.*
import korlibs.korge.scene.*
import korlibs.korge.ui.*
import korlibs.korge.view.*
import korlibs.image.color.*
import korlibs.math.geom.*

class MenuScene() : Scene() {
	override suspend fun SContainer.sceneMain() {
		// set a background color
		//views.clearColor = Colors.BLACK

		// Add a text to show the name of the game
		var gameNameText = text("Super Pong Bros II") {
			position(sceneWidth / 2 - 128, sceneHeight / 2 - 128)
		}

		var playButton = uiButton(size = Size(256.0, 32.0)) {
			text = "Play"
			position(sceneWidth / 2 - 128, sceneHeight / 2 - 64)
			onClick {
				sceneContainer.changeTo<PlayScene>()
			}
		}
		var exitButton = uiButton(size = Size(256.0, 32.0)) {
			text = "Exit"
			position(sceneWidth / 2 - 128, sceneHeight / 2)
			onClick {
				views.gameWindow.close()
			}
		}
	}

}