package samples.pong

import korlibs.event.*
import korlibs.image.color.*
import korlibs.korge.input.*
import korlibs.korge.scene.*
import korlibs.korge.view.*
import korlibs.time.*
import kotlin.math.*
import kotlin.random.Random.Default.nextDouble

// Define the various states that the game can be in
enum class GameStates {
	Starting,   // The game is just starting and some help text will be shown. Ball will not be moving
	Playing,    // in this state the ball will be moving and player can score against each other
	Scored,     // in this state the game will pause and show that a goal was cored
}

class PlayScene() : Scene() {
	override suspend fun SContainer.sceneMain() {
		// Initialize the variables which will capture the game state
		var scorePlayerLeft = 0
		var scorePlayerRight = 0
		var playState = GameStates.Starting

		// Initialize the starting game state values
		val paddlePosYAtStart = sceneHeight / 2 - 50.0
		val ballPosXAtStart = sceneWidth / 2 - 10.0
		val ballPosYAtStart = sceneWidth / 2 + 10.0
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
			addFastUpdater {
				text = "FPS: " + views.gameWindow.fps.toString()
			}
		}

		// Add a HUD for reporting the ticks/frame length
		val tickSizeText = text("Tick: 0") {
			position(10, 50)
			addFastUpdater {
				text = "Tick: " + views.gameWindow.timePerFrame.toString()
			}
		}

		// Add a help text which explains the rules of the game
		val helpText = text("") {
			position(10, 100)
			addFastUpdater {
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
			position(sceneWidth / 2 - 100, sceneHeight / 2 - 20)
			addFastUpdater {
				// this is only visible when the game is in Scored state
				visible = (playState == GameStates.Scored)
			}
		}

		// text to show the score of the player on the Left side
		val scoreLeftText = text("0") {
//            textSize = 24.0
			position(sceneWidth / 4, sceneHeight / 2)
			addFastUpdater {
				text = scorePlayerLeft.toString()
			}
		}
		// text to show the score of the player on the Right side
		val scoreRightText = text("0") {
			position(sceneWidth * 3 / 4, sceneHeight / 2)
			addFastUpdater {
				text = scorePlayerRight.toString()
			}
		}

		// the left paddle
		val paddleLeft = solidRect(paddleWidth, paddleHeight, Colors.RED) {
			position(paddleDistanceFromWall, paddlePosYAtStart)
			addFastUpdater {
				// move the paddle up or down as long as it doesn't leaves the bounds of the game window
				val keys = views.input.keys
				if (keys[Key.W] && y > 0) {
					y -= paddleMoveSpeed
				}
				if (keys[Key.S] && y < sceneHeight - paddleHeight) {
					y += paddleMoveSpeed
				}
			}
		}

		// the right paddle
		val paddleRight = solidRect(paddleWidth, paddleHeight, Colors.BLUE) {
			position(sceneWidth - paddleDistanceFromWall - paddleWidth, paddlePosYAtStart)
			addFastUpdater {
				// move the paddle up or down as long as it doesn't leaves the bounds of the game window
				val keys = views.input.keys
				if (keys[Key.UP] && y > 0) {
					y -= paddleMoveSpeed
				}
				if (keys[Key.DOWN] && y < sceneHeight - paddleHeight) {
					y += paddleMoveSpeed
				}
			}
		}

		val ball = circle(ballRadius.toDouble(), Colors.WHITE) {
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

			addFastUpdater {
				// only move ball if the game is in Playing state
				if (playState == GameStates.Playing) {

					// convert the ball's velocity vector (speed, angle) to a point to move to
					x += spd * cos(ang) * it.seconds
					y += spd * sin(ang) * it.seconds

					// if the ball hits the paddles, flip its direction and increase speed
					if ((x < paddleLeft.x + 10 && y > paddleLeft.y && y < paddleLeft.y + 100) ||
						(x > paddleRight.x - 20 && y > paddleRight.y && y < paddleRight.y + 100)) {
						spd += ballSpeedIncrease
						ang = PI - ang
					}

					// if ball hits the walls, flip its direction and increase speed
					if (y < 0 || y > sceneHeight - 20) {
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
					} else if (x > sceneWidth) {
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
        keys {
            down {
                when (it.key) {
                    Key.ESCAPE -> sceneContainer.changeTo<MenuScene>()
                    Key.SPACE -> playState = GameStates.Playing
                    else -> Unit
                }
            }
        }
	}
}
