package com.soywiz.korge.samples.minesweeper

import com.soywiz.klock.*
import com.soywiz.korau.sound.*
import com.soywiz.korge.html.*
import com.soywiz.korge.render.*
import com.soywiz.korge.view.*
import com.soywiz.korim.bitmap.*
import com.soywiz.korio.lang.*
import com.soywiz.korma.random.*
import kotlin.random.*

// Process of the board
class Board(
	parent: Container,
	val imageSet: BmpSlice,
	val imageSlices: List<BmpSlice>,
	val click: NativeSound,
	val boom: NativeSound,
	// Width, height and quantity of mines are established
	// Board characteristics: width, height, number of mines
	val bwidth: Int,
	val bheight: Int,
	var mineNumber: Int
) : Process(parent) {
	// Matrix with board
	var board: Array<IntArray> = arrayOf()
	// Mask matrix (indicates which parts of the board are uncovered)
	var mask: Array<BooleanArray> = arrayOf()
	// Marking matrix (indicates which parts of the board are marked as "possible mine") (right click)
	var mark: Array<BooleanArray> = arrayOf()

	// Variables used for the counter
	var tstart: DateTime = DateTime.EPOCH
	var tstop: DateTime = DateTime.EPOCH
	var timeText: Text

	var lastx: Int = 0
	var lasty: Int = 0

	override var width = bwidth * imageSet.height.toDouble()
	override var height = bheight * imageSet.height.toDouble()

	init {
		// Creating text with time
		//timeText = new Text("", 50, 50, Text.Align.center, Text.Align.middle, Color.white, new Font("Arial", 40));
		//timeText = Text("", 50, 50, Text.Align.center, Text.Align.middle, Color.white, Font.fromResource("font.ttf", 40));
		val FONT_HEIGHT = 24.0
		timeText = text("00:00", textSize = FONT_HEIGHT).apply {
			centerXBetween(0.0, this@Board.width)
			y = -FONT_HEIGHT - 5
			format.align = Html.Alignment.CENTER
		}

		//The board is centered on the screen
		//x = screen.width / 2 - width / 2
		//y = screen.height / 2 - (height - 10 - FONT_HEIGHT) / 2
		centerOnStage()
		y += FONT_HEIGHT / 2

		// Restart board
		restart()
	}

	// Destructor, the text is removed when the board is being destroyed
	override fun onDestroy() {
		timeText.removeFromParent()
	}

	// Returns the current time
	val time: DateTime get() = DateTime.now()

	fun resetTimer() {
		tstart = time
		tstop = DateTime.EPOCH
	}

	fun stopTimer() {
		tstop = time
	}

	// Returns the time in seconds that has passed since the timer started
	val elapsed: Int
		get() = run {
			var ctime = time
			if (tstop != DateTime.EPOCH) ctime = tstop
			return (ctime - tstart).seconds.toInt()
		}

	// Update the counter text in the format %02d:%02d MM:SS
	fun updateTimeText() {
		timeText.text = "%02d:%02d".format(elapsed / 60, elapsed % 60)
	}

	// Function that is responsible for deleting the board and creating a new one
	fun restart() {
		//Now that we are going to delete a new board (and that we are going to create a new game)
		//we reset the timer
		resetTimer()

		// We create the matrices with the board, the vision mask and the marking mask (of possible mines)
		board = Array(bheight) { IntArray(bwidth) }
		mask = Array(bheight) { BooleanArray(bwidth) }
		mark = Array(bheight) { BooleanArray(bwidth) }

		// We check that there are no attempts to place more mines than there are positions, thus avoiding an infinite loop
		// actually only a maximum number of mines equal to the board positions - 1 are placed
		if (mineNumber > bwidth * bheight - 1) mineNumber = bwidth * bheight - 1

		// Now we will proceed to place the mines on the board
		repeat(mineNumber) {
			// We declare px and py that we will use to store the temporary positions of a mine
			var px: Int
			var py: Int
			do {
				// We get a possible mine position
				px = Random[0, bwidth - 1]
				py = Random[0, bheight - 1]
				// We check if there is a mine in that position and we will be looking for positions until
				// there is no mine in that position
			} while (board[py][px] == 10)

			// Now that we know that there is no mine in that position, we place it
			board[py][px] = 10
		}

		// Now that we have placed the mines, we are going to place the numbers around them
		// This is an interesting part of the minesweeper, here the numbers are placed around the mines

		// We go over the entire board
		for (y in 0 until bheight) {
			for (x in 0 until bwidth) {
				// We check that there is no mine in that position, if there is a mine, we "pass":
				// we continue to the next position without processing this one
				if (board[y][x] == 10) continue

				// Now we are going to count the mines around this position
				// (since there is no mine in this position and we may have to put a number if it has a contiguous mine)
				var count = 0
				for (y1 in -1..+1) {
					for (x1 in -1..+1) {
						// Now x + x1 and y + y1 will take matrix positions contiguous to the current position
						// from x - 1, y - 1 to x + 1, y + 1
						// We check that the position is inside the matrix, since for example in position 0
						// 0 - 1, 0 - 1 would be -1, -1, which is not inside the matrix
						// so if it is not inside the limits of the matrix, we pass
						if (!inBounds(x + x1, y + y1)) continue
						// If there is a mine in this adjacent position, we increase the counter
						if (board[y + y1][x + x1] == 10) count++
					}
				}

				// We introduce the new image on the board (since the image with 0 positions is 1
				// and the following are 1, 2, 3, 4, 5, 6, 7, 8) we put the corresponding image to count + 1
				board[y][x] = count + 1
			}
		}

		// Now we have the board ready
	}

	// Indicates if a position is inside the matrix
	fun inBounds(px: Int, py: Int): Boolean {
		// If the x position is negative or if the position is more to the right of the width of the board,
		// it returns false (it is not inside)
		if (px < 0 || px >= bwidth) return false
		// If the same thing happens with the y position, we also return false
		if (py < 0 || py >= bheight) return false
		// If we have not already returned false, it means the position is inside the board, so we return true
		return true
	}

	var fillpos = 0

	// Fill in a position (recursively, the clearest and simplest form)
	suspend fun fill(px: Int, py: Int) {
		if (!inBounds(px, py)) return
		if (mask[py][px] || mark[py][px]) return
		mask[py][px] = true

		if (fillpos % 7 == 0) audio.play(click)
		frame()
		fillpos++

		if (board[py][px] != 1) return
		fill(px - 1, py)
		fill(px + 1, py)
		fill(px, py - 1)
		fill(px, py + 1)
		fill(px - 1, py - 1)
		fill(px + 1, py + 1)
		fill(px + 1, py - 1)
		fill(px - 1, py + 1)
	}

	suspend fun showBoardLose() {
		// It's a sub-function that unmasks a position after checking for correctness
		fun unmask(x: Int, y: Int): Boolean {
			if (!inBounds(x, y)) return false
			mask[y][x] = true
			return true
		}

		// Diamond shaped propagation
		var dist = 0
		while (true) {
			var drawing = false

			for (n in 0..dist) {
				if (unmask(lastx - n + dist, lasty - n)) drawing = true
				if (unmask(lastx + n - dist, lasty - n)) drawing = true
				if (unmask(lastx - n + dist, lasty + n)) drawing = true
				if (unmask(lastx + n - dist, lasty + n)) drawing = true
			}

			if (!drawing) break

			dist++
			frame()
		}
	}

	suspend fun showBoardWin() {
		for (y in 0 until bheight) {
			for (x in 0 until bwidth) {
				if (board[y][x] == 10) {
					mask[y][x] = false
					mark[y][x] = true
					frame()
				} else {
					mask[y][x] = true
				}
			}
		}
	}

	suspend fun check(px: Int, py: Int): Boolean {
		if (!inBounds(px, py)) return false

		// We save the last position that was clicked
		lastx = px; lasty = py

		// The next is a mine
		if (board[py][px] == 10) return true

		// The next is an empty box
		if (board[py][px] == 1) {
			fps = 140.0
			fillpos = 0
			fill(px, py)
			fps = 60.0
			return false
		}

		if (!mask[py][px]) {
			mask[py][px] = true
			audio.play(click)
		}

		return false
	}

	// Check if the board is in a state when we can consider the game won
	fun checkWin(): Boolean {
		var count = 0
		for (y in 0 until bheight) {
			for (x in 0 until bwidth) {
				if (mask[y][x]) count++
			}
		}

		return (count == bwidth * bheight - mineNumber)
	}

	// The main action redirects to the game action
	override suspend fun main() = action(::play)

	// The main game action that is responsible for managing mouse clicks
	suspend fun play() {
		while (true) {
			//println("Mouse.x: ${Mouse.x}, x=$x")
			if (mouse.x >= x && mouse.x < x + bwidth * imageSet.height) {
				if (mouse.y >= y && mouse.y < y + bheight * imageSet.height) {
					val px = ((mouse.x - x) / imageSet.height).toInt()
					val py = ((mouse.y - y) / imageSet.height).toInt()

					if (mouse.released[0]) {
						if (!mark[py][px]) {
							if (check(px, py)) {
								action(::lose)
							} else if (checkWin()) {
								action(::win)
							}
						}
					} else if (mouse.released[1] || mouse.released[2]) {
						mark[py][px] = !mark[py][px]
					}
				}
			}

			frame()
		}
	}

	// Board action that occurs when the player has lost
	suspend fun lose() {
		audio.play(boom, 0)
		stopTimer()
		showBoardLose()

		while (true) {
			if (mouse.left || mouse.right) {
				restart()
				for (n in 0 until 10) frame()
				action(::play)
			}
			frame()
		}
	}

	// Board action that occurs when the player has won
	suspend fun win() {
		stopTimer()
		showBoardWin()

		while (true) {
			if (mouse.left || mouse.right) {
				restart()
				for (n in 0 until 10) frame()
				action(::play)
			}
			frame()
		}
	}

	val images = Array(bheight) { py ->
		Array(bwidth) { px ->
			image(Bitmaps.transparent).xy(px * imageSet.height, py * imageSet.height).scale(0.9)
		}
	}

	override fun renderInternal(ctx: RenderContext) {
		for (py in 0 until bheight) {
			for (px in 0 until bwidth) {
				val image = if (!mask[py][px]) {
					imageSlices[if (mark[py][px]) 11 else 0]
				} else {
					imageSlices[board[py][px]]
				}

				images[py][px].texture = image
			}
		}
		super.renderInternal(ctx)
	}
}
