package samples.connect4

import korlibs.datastructure.*
import korlibs.image.bitmap.*
import korlibs.image.color.*
import korlibs.io.async.*
import korlibs.korge.animate.*
import korlibs.korge.input.*
import korlibs.korge.scene.*
import korlibs.korge.tween.*
import korlibs.korge.view.*
import korlibs.math.geom.*

class MainConnect4 : ScaledScene(448, 384) {
    override suspend fun SContainer.sceneMain() {
        //sceneContainer.setSize(300.0, 300.0)
        val redChip = Bitmap32Context2d(64, 64) {
            fill(Colors.RED) { circle(Point(32, 32), 26.0) }
        }
        val yellowChip = Bitmap32Context2d(64, 64) {
            fill(Colors.YELLOW) { circle(Point(32, 32), 26.0) }
        }
        val emptyChip = Bitmap32Context2d(64, 64) {
        }

        val chipImages = mapOf(
            Chip.EMPTY to emptyChip,
            Chip.YELLOW to yellowChip,
            Chip.RED to redChip
        )

        val nrows = Board.EMPTY.numRows
        val ncols = Board.EMPTY.numCols
        val skeleton = Bitmap32Context2d(64 * ncols, 64 * nrows) {
            fill(Colors.BLUE) {
                rect(0.0, 0.0, width.toDouble(), height.toDouble())
                for (row in 0 until nrows) {
                    for (col in 0 until ncols) {
                        circle(Point(32.0 + col * 64, 32.0 + row * 64), 24.0)
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
            return container.image(chipImages[chip]!!, Anchor.CENTER).position(getPosition(column, row))
        }

        var turn = Chip.RED
        val views = Array2<View>(ncols, nrows) { DummyView() }
        var gameFinished = false

        suspend fun drop(chip: Chip, column: Int) {
            animate(defaultSpeed = 128.0) {
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
                                            moveTo(chipView, pos.x, pos.y)
                                            views[c, r] = chipView
                                        }
                                        is Action.Finish -> {
                                            for (pos in action.win.positions) {
                                                val view = views[pos.column, pos.row]
                                                //tweenLazy({ view::colorAdd[ColorAdd(255, 255, 255, 0)] })
                                                tweenLazy({ view::alpha[1.0] })
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
}
