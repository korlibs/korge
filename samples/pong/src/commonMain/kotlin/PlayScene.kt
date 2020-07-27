import com.soywiz.korev.Key
import com.soywiz.korge.scene.Scene
import com.soywiz.korge.input.*
import com.soywiz.korge.view.*
import com.soywiz.korim.color.Colors
import kotlin.math.*
import kotlin.random.Random.Default.nextDouble

// Define the various states that the game can be in
enum class GameStates {
	Starting,   // The game is just starting and some help text will be shown. Ball will not be moving
	Playing,    // in this state the ball will be moving and player can score against each other
	Scored,     // in this state the game will pause and show that a goal was cored
}

class PlayScene() : Scene() {
	suspend override fun Container.sceneInit() {
		// Initialize the variables which will capture the game state
		var scorePlayerLeft = 0
		var scorePlayerRight = 0
		var playState = GameStates.Starting

		// Initialize the starting game state values
		val paddlePosYAtStart = views.virtualHeight / 2 - 50.0
		val ballPosXAtStart = views.virtualWidth / 2 - 10.0
		val ballPosYAtStart = views.virtualWidth / 2 + 10.0
		val ballSpeedAtStart = 200.0

		// Initialize the variables defining the paddle and ball parameters
		val paddleWidth = 10.0
		val paddleHeight = 100.0
		val paddleDistanceFromWall = 10.0
		val paddleMoveSpeed = 10.0
		val ballRadius = 10.0
		val ballSpeedIncrease = 50.0

		// Add a HUD for reporting the FPS
		val fpsText = text("FPS: 0") {
			position(10, 30)
			addUpdater {
				text = "FPS: " + views.gameWindow.fps.toString()
			}
		}

		// Add a HUD for reporting the ticks/frame length
		val tickSizeText = text("Tick: 0") {
			position(10, 50)
			addUpdater {
				text = "Tick: " + views.gameWindow.timePerFrame.toString()
			}
		}

		// Add a help text which explains the rules of the game
		val helpText = text("") {
			position(10, 100)
			addUpdater {
				// this text is only visible if the game is not in Playing state
				visible = (playState != GameStates.Playing)

				// show a different text if the game is just starting
				if (playState == GameStates.Starting) {
					text = "Welcome to the PONG Game!\n\n" +
						"-- To move the Left Paddle, use the keys [W] and [S].\n\n" +
						"-- To move the Right Paddle, use the keys [UP] and [DOWN]\n\n" +
						"-- To go back to Main Menu, use [ESC]\n\n" +
						"-- To Start the game, use [SPACE]"
					// show a different text if the game is in Scored state
				} else if (playState == GameStates.Scored) {
					text = "Press [SPACE] for the next round!"
				}
			}
		}

		// A simple flavour text informing that a goal was scored
		val scoredYellText = text("SCORED!!!\n\n") {
			position(views.virtualWidth / 2 - 100, views.virtualHeight / 2 - 20)
			addUpdater {
				// this is only visible when the game is in Scored state
				visible = (playState == GameStates.Scored)
			}
		}

		// text to show the score of the player on the Left side
		val scoreLeftText = text("0") {
//            textSize = 24.0
			position(views.virtualWidth / 4, views.virtualHeight / 2)
			addUpdater {
				text = scorePlayerLeft.toString()
			}
		}
		// text to show the score of the player on the Right side
		val scoreRightText = text("0") {
			position(views.virtualWidth * 3 / 4, views.virtualHeight / 2)
			addUpdater {
				text = scorePlayerRight.toString()
			}
		}

		// the left paddle
		val paddleLeft = solidRect(paddleWidth, paddleHeight, Colors.RED) {
			position(paddleDistanceFromWall, paddlePosYAtStart)
			addUpdater {
				// move the paddle up or down as long as it doesn't leaves the bounds of the game window
				val keys = views.input.keys
				if (keys[Key.W] && y > 0) {
					y -= paddleMoveSpeed
				}
				if (keys[Key.S] && y < views.virtualHeight - paddleHeight) {
					y += paddleMoveSpeed
				}
			}
		}

		// the right paddle
		val paddleRight = solidRect(paddleWidth, paddleHeight, Colors.BLUE) {
			position(views.virtualWidth - paddleDistanceFromWall - paddleWidth, paddlePosYAtStart)
			addUpdater {
				// move the paddle up or down as long as it doesn't leaves the bounds of the game window
				val keys = views.input.keys
				if (keys[Key.UP] && y > 0) {
					y -= paddleMoveSpeed
				}
				if (keys[Key.DOWN] && y < views.virtualHeight - paddleHeight) {
					y += paddleMoveSpeed
				}
			}
		}

		val ball = circle(ballRadius, Colors.WHITE) {
			position(ballPosXAtStart, ballPosYAtStart)

			// mutable data defining the ball state
			var spd = ballSpeedAtStart
			var ang = nextDouble() * 2 * PI

			// function to reset the ball
			fun resetRound() {
				x = ballPosXAtStart
				y = ballPosYAtStart
				spd = ballSpeedAtStart
				ang = nextDouble() * 2 * PI

				// Change game state to Scored. Game will be paused till players start the next round.
				playState = GameStates.Scored
			}

			addUpdater {
				// only move ball if the game is in Playing state
				if (playState == GameStates.Playing) {

					// convert the ball's velocity vector (speed, angle) to a point to move to
					x += spd * cos(ang) * it.seconds;
					y += spd * sin(ang) * it.seconds;

					// if the ball hits the paddles, flip its direction and increase speed
					if ((x < paddleLeft.x + 10 && y > paddleLeft.y && y < paddleLeft.y + 100) ||
						(x > paddleRight.x - 20 && y > paddleRight.y && y < paddleRight.y + 100)) {
						spd += ballSpeedIncrease
						ang = PI - ang
					}

					// if ball hits the walls, flip its direction and increase speed
					if (y < 0 || y > views.virtualHeight - 20) {
						spd += 10
						ang *= -1
					}

					// if ball goes through the vertical walls/goalpost, handle scoring and reset the round
					if (x < -20) {
						// Reset the Ball
						resetRound()

						// Update the score
						scorePlayerRight++
						scoredYellText.text = "Right SCORED!!!"
					} else if (x > views.virtualWidth) {
						// Reset the Ball
						resetRound()

						// Update the score
						playState = GameStates.Scored
						scorePlayerLeft++
						scoredYellText.text = "Left SCORED!!!"
					}
				}
			}
		}

		// Add the keys needed to run the game
		onKeyDown {
			when (it.key) {
				Key.ESCAPE -> sceneContainer.changeTo<MenuScene>()
				Key.SPACE -> playState = GameStates.Playing
				else -> Unit
			}
		}
	}
}
