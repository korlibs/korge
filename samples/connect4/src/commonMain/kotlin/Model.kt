// Input

sealed class Operation {
    data class Place(val chip: Chip, val column: Int) : Operation()
}

// Output

data class BoardResult(val board: Board, val actionsLists: List<List<Action>>)

sealed class Action {
    data class Drop(val column: Int, val row: Int, val chip: Chip) : Action()
    data class Finish(val win: WinResult) : Action()
}

data class Board(val rows: List<String>) {
    companion object {
        val EMPTY = Board(
                ".......",
                ".......",
                ".......",
                ".......",
                ".......",
                "......."
        )
    }

    val numRows get() = rows.size
    val numCols get() = rows[0].length
    constructor(vararg rows: String) : this(rows.toList())

    @OptIn(ExperimentalStdlibApi::class)
    fun with(column: Int, row: Int, chip: Chip): Board {
        return Board(rows.mapIndexed { nrow, rowString ->
            val rowChars = rowString.toCharArray()
            if (nrow == row) {
                rowChars[column] = chip.char
            }
            String(rowChars)
        })
    }

    fun isValid(column: Int, row: Int): Boolean {
        return (column in 0 until numCols) && (row in 0 until numRows)
    }

    operator fun get(column: Int, row: Int): Chip {
        if (!isValid(column, row)) return Chip.EMPTY
        val c = rows[row][column]
        return Chip.map[c] ?: error("Invalid character '$c'")
    }

    fun getAvailableOperations(chip: Chip): List<Operation> = (0 until numCols)
            .map { Operation.Place(chip, it) }
            .filter { apply(it) != null }

    fun apply(operation: Operation): BoardResult? {
        when (operation) {
            is Operation.Place -> {
                val column = operation.column
                if (column !in (0 until numCols)) return null
                val height = height(column)
                if (height >= numRows) return null
                val row = numRows - 1 - height
                val chip = operation.chip
                val newBoard = with(column, row, chip)
                return BoardResult(
                        board = newBoard,
                        actionsLists = ArrayList<List<Action>>().apply {
                            add(listOf(Action.Drop(column, row, chip)))
                            newBoard.winner()?.let {
                                add(listOf(Action.Finish(it)))
                            }
                        }
                )
            }
        }
        TODO()
    }

    private fun height(column: Int): Int {
        for (n in 0 until numRows) {
            if (get(column, numRows - 1 - n) == Chip.EMPTY) {
                return n
            }
        }
        return numRows
    }

    override fun toString(): String {
        return rows.joinToString("\n")
    }

    fun boardIsFull(): Boolean = (0 until numCols).all { height(it) == numRows }

    fun check(column: Int, row: Int, dx: Int, dy: Int): WinResult? {
        val chip = this[column, row]
        if (chip == Chip.EMPTY) return null
        val out = arrayListOf<Position>()
        for (n in 0 until 4) {
            val c = column + dx * n
            val r = row + dy * n
            if (this[c, r] != chip) return null
            out.add(Position(c, r))
        }
        return WinResult(chip, out)
    }

    fun winner(): WinResult? {
        for (row in 0 until numRows) {
            for (col in 0 until numCols) {
                val win0 = check(col, row, +1, 0)
                val win1 = check(col, row, 0, +1)
                val win2 = check(col, row, +1, -1)
                val win3 = check(col, row, +1, +1)
                if (win0 != null) return win0
                if (win1 != null) return win1
                if (win2 != null) return win2
                if (win3 != null) return win3
            }
        }
        return if (boardIsFull()) WinResult(Chip.EMPTY, listOf()) else null
    }
}

data class Position(val column: Int, val row: Int)

data class WinResult(val chip: Chip?, val positions: List<Position>)

enum class Chip(val char: Char) {
    EMPTY('.'), RED('r'), YELLOW('y');
    companion object {
        val map = values().associateBy { it.char }
    }
}
