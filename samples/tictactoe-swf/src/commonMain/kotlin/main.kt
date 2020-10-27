import com.soywiz.korge.*
import com.soywiz.korge.animate.*
import com.soywiz.korge.animate.serialization.*
import com.soywiz.korge.input.*
import com.soywiz.korge.scene.*
import com.soywiz.korge.view.*
import com.soywiz.korim.font.*
import com.soywiz.korinject.*
import com.soywiz.korio.async.*
import com.soywiz.korio.file.std.*
import com.soywiz.korio.lang.*
import com.soywiz.korma.geom.*
import com.soywiz.korma.geom.ds.*

//suspend fun main() = Korge(Korge.Config(module = TicTacToeModule, debug = true))
suspend fun main() = Korge(Korge.Config(module = TicTacToeModule))

object TicTacToeModule : Module() {
	override val mainScene = TicTacToeMainScene::class
	override val title: String = "tic-tac-toe"
	override val icon: String = "icon.png"
	override val size: SizeInt = SizeInt(640, 480)

	override suspend fun AsyncInjector.configure() {
		mapPrototype { TicTacToeMainScene() }
		//Fonts.fonts.registerFont("Times New Roman", Fonts.defaultFont)
		//Fonts.fonts.registerFont("Times New Roman", resourcesVfs["fonts/font1.fnt"].readBitmapFont())
	}
}

// Controller
class TicTacToeMainScene : Scene() {
	private lateinit var mainLibrary: AnLibrary

	val board = Board(3, 3)
	lateinit var game: Game

	override suspend fun Container.sceneInit() {
		mainLibrary = resourcesVfs["main.ani"].readAni(AnLibrary.Context(views))
	}

	override suspend fun Container.sceneMain() {

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

