import com.soywiz.kds.*
import com.soywiz.korge.*
import com.soywiz.korge.animate.*
import com.soywiz.korge.input.*
import com.soywiz.korge.tween.*
import com.soywiz.korge.view.*
import com.soywiz.korim.bitmap.*
import com.soywiz.korim.color.*
import com.soywiz.korio.async.*
import com.soywiz.korma.geom.*
import com.soywiz.korma.geom.vector.*

suspend fun main() = Korge(width = 448, height = 384, bgcolor = Colors["#2b2b2b"]) {
    val redChip = Bitmap32(64, 64).context2d {
        fill(Colors.RED) { circle(32.0, 32.0, 26.0) }
    }
    val yellowChip = Bitmap32(64, 64).context2d {
        fill(Colors.YELLOW) { circle(32.0, 32.0, 26.0) }
    }
    val emptyChip = Bitmap32(64, 64).context2d {
    }

    val chipImages = mapOf(
            Chip.EMPTY to emptyChip,
            Chip.YELLOW to yellowChip,
            Chip.RED to redChip
    )

    val nrows = Board.EMPTY.numRows
    val ncols = Board.EMPTY.numCols
    val skeleton = Bitmap32(64 * ncols, 64 * nrows).apply {
        val bitmap = this
        context2d {
            fill(Colors.BLUE) {
                rect(0.0, 0.0, bitmap.width.toDouble(), bitmap.height.toDouble())
                for (row in 0 until nrows) {
                    for (col in 0 until ncols) {
                        circle(32.0 + col * 64, 32.0 + row * 64, 24.0)
                    }
                }
            }
        }
    }

    var board = Board.EMPTY
    val container = container {
    }
    image(skeleton)

    fun getPosition(column: Int, row: Int): Point {
        return Point(column * 64.0 + 32.0, row * 64.0 + 32.0)
    }

    fun createChip(column: Int, row: Int, chip: Chip): Image {
        return container.image(chipImages[chip]!!, 0.5, 0.5).position(getPosition(column, row))
    }

    var turn = Chip.RED
    val views = Array2<View>(ncols, nrows) { DummyView() }
    var gameFinished = false

    suspend fun drop(chip: Chip, column: Int) {
        animate(speed = 128.0) {
            val result = board.apply(Operation.Place(chip, column))
            if (result != null) {
                if (result.board.winner() != null) {
                    gameFinished = true
                }
                board = result.board
                sequence {
                    for (actions in result.actionsLists) {
                        parallel {
                            for (action in actions) {
                                when (action) {
                                    is Action.Drop -> {
                                        val c = action.column
                                        val r = action.row
                                        val pos = getPosition(c, r)
                                        val chipView = createChip(column, -1, action.chip)
                                        chipView.moveTo(pos.x, pos.y)
                                        views[c, r] = chipView
                                    }
                                    is Action.Finish -> {
                                        for (pos in action.win.positions) {
                                            val view = views[pos.column, pos.row]
                                            tween({ view::colorAdd[ColorAdd(255, 255, 255, 0).rgba] }, time = this.time)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    val thread = AsyncThread()

    for (col in 0 until ncols) {
        val columnIndicator = solidRect(64, 64 * nrows, Colors.WHITE).alpha(0.0).position(64 * col, 0)

        fun updateIndicatorColor() {
            columnIndicator.color = (if (turn == Chip.RED) Colors.RED else Colors.YELLOW).withA(columnIndicator.color.a)

        }

        columnIndicator.mouse {
            over {
                if (board.winner() == null && board.apply(Operation.Place(turn, col)) != null) {
                    updateIndicatorColor()
                    columnIndicator.alpha(0.2)
                }
            }
            out {
                columnIndicator.alpha(0.0)
            }
            click {
                launchImmediately {
                    if (gameFinished) {
                        board = Board.EMPTY
                        container.removeChildren()
                        gameFinished = false
                    } else {
                        //thread.queue {
                        val currentTurn = turn
                        val newTurn = if (turn == Chip.RED) Chip.YELLOW else Chip.RED
                        turn = newTurn
                        updateIndicatorColor()
                        drop(currentTurn, col)
                        //}
                    }
                }
            }
        }
    }
}

