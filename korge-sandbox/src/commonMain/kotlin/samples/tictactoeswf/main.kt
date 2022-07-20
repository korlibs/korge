package samples.tictactoeswf

import com.soywiz.korge.animate.AnLibrary
import com.soywiz.korge.ext.swf.readSWF
import com.soywiz.korge.input.mouse
import com.soywiz.korge.scene.ScaledScene
import com.soywiz.korge.view.SContainer
import com.soywiz.korge.view.descendantsWithPropInt
import com.soywiz.korge.view.get
import com.soywiz.korge.view.setText
import com.soywiz.korio.async.Signal
import com.soywiz.korio.async.waitOne
import com.soywiz.korio.file.std.resourcesVfs
import com.soywiz.korio.lang.invalidOp
import com.soywiz.korma.geom.PointInt
import com.soywiz.korma.geom.ds.get

// Controller
class MainTicTacToeSwf : ScaledScene(640, 480) {
	private lateinit var mainLibrary: AnLibrary

	val board = Board(3, 3)
	lateinit var game: Game

	override suspend fun SContainer.sceneInit() {
		mainLibrary = resourcesVfs["tic-tac-toe-swf/main.swf"].readSWF(AnLibrary.Context(views))
	}

	override suspend fun SContainer.sceneMain() {

		sceneView += mainLibrary.createMainTimeLine()

		for ((rowView, row) in sceneView.descendantsWithPropInt("row")) {
			for ((cellView, cell) in rowView.descendantsWithPropInt("cell")) {
				board.cells[row, cell].init(cellView)
			}
		}

		val p1 = InteractivePlayer(board, Chip.CROSS)
		val p2 = BotPlayer(board, Chip.CIRCLE)
		//val p2 = InteractivePlayer(board, Chip.CIRCLE)

		game = Game(board, listOf(p1, p2))
		while (true) {
			game.board.reset()
			val result = game.game()

			println(result)

			val results = mainLibrary.createMovieClip("Results")
			//(results["result"] as AnTextField).format?.face = Html.FontFace.Bitmap(font)
			when (result) {
				is Game.Result.DRAW -> results["result"].setText("DRAW")
				is Game.Result.WIN -> {
					results["result"].setText("WIN")
					for (cell in result.cells) cell.highlight(true)
					for (cell in game.board.cells.toList() - result.cells) cell.lowlight(true)
				}
			}
			sceneView += results
			results["hit"]?.firstOrNull?.mouse?.click?.waitOne()
			//sceneView -= results
			results.removeFromParent()
		}
	}
}

interface Player {
	val chip: Chip
	suspend fun move(): PointInt
}

class Game(val board: Board, val players: List<Player>) {
	interface Result {
		object DRAW : Result
		class WIN(val player: Player?, val cells: List<Board.Cell>) : Result
	}

	suspend fun game(): Result {
		var turn = 0
		while (board.moreMovements) {
			val currentPlayer = players[turn % players.size]
			while (true) {
				val pos = currentPlayer.move()
				println(pos)
				if (board.cells[pos].value == Chip.EMPTY) {
					board.cells[pos].setAnimate(currentPlayer.chip)
					break
				}
			}
			if (board.winner != null) return Result.WIN(currentPlayer, board.winnerLine ?: listOf())
			turn++
		}
		return Result.DRAW
	}
}

class BotPlayer(val board: Board, override val chip: Chip) : Player {
	override suspend fun move(): PointInt {
		for (cell in board.cells) {
			if (cell.value == Chip.EMPTY) {
				return cell.pos
			}
		}
		invalidOp("No more movements")
	}
}

class InteractivePlayer(val board: Board, override val chip: Chip) : Player {
	val clicked = Signal<PointInt>()

	init {
		for (cell in board.cells) {
			cell.onPress {
				clicked(cell.pos)
			}
		}
	}

	override suspend fun move(): PointInt = clicked.waitOne()
}

