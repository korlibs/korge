import kotlin.test.*

class ModelTest {
    @Test
    fun testBoardGet() {
        val board = Board(
                "y.",
                ".r"
        )

        assertEquals(Chip.YELLOW, board[0, 0])
        assertEquals(Chip.EMPTY, board[1, 0])
        assertEquals(Chip.EMPTY, board[0, 1])
        assertEquals(Chip.RED, board[1, 1])
    }

    @Test
    fun testPlaceOnEmptyBoard() {
        assertEquals(
                Board(
                        ".......",
                        ".......",
                        ".......",
                        ".......",
                        ".......",
                        "......r"
                ),
                Board(
                        ".......",
                        ".......",
                        ".......",
                        ".......",
                        ".......",
                        "......."
                ).apply(Operation.Place(chip = Chip.RED, column = 6))?.board
        )
    }

    @Test
    fun testPlaceOverAnotherChip() {
        assertEquals(
                Board(
                        ".......",
                        ".......",
                        ".......",
                        ".......",
                        "......y",
                        "......r"
                ),
                Board(
                        ".......",
                        ".......",
                        ".......",
                        ".......",
                        ".......",
                        "......r"
                ).apply(Operation.Place(chip = Chip.YELLOW, column = 6))?.board
        )
    }

    val boardEmpty = Board(
            ".......",
            ".......",
            ".......",
            ".......",
            ".......",
            "......."
    )

    val boardWithFullLastColumn = Board(
            "......y",
            "......y",
            "......r",
            "......y",
            "......y",
            "......r"
    )

    val boardWinRed = Board(
            ".......",
            ".......",
            ".......",
            ".......",
            ".......",
            "rrrr..."
    )

    val boardFullTie = Board(
            "yyyrrry",
            "rrryyyr",
            "yyyrrry",
            "rrryyyr",
            "yyyrrry",
            "rrryyyr"
    )

    @Test
    fun testInvalidPlacement() {
        assertEquals(null, boardWithFullLastColumn.apply(Operation.Place(chip = Chip.RED, column = 6))?.board)
        assertEquals(null, boardWithFullLastColumn.apply(Operation.Place(chip = Chip.RED, column = -1))?.board)
        assertEquals(null, boardWithFullLastColumn.apply(Operation.Place(chip = Chip.RED, column = 7))?.board)
    }

    @Test
    fun testAvailableOperations() {
        assertEquals(listOf(0, 1, 2, 3, 4, 5), boardWithFullLastColumn.getAvailableOperations(Chip.YELLOW).filterIsInstance<Operation.Place>().map { it.column })
        assertEquals(listOf(0, 1, 2, 3, 4, 5, 6), boardEmpty.getAvailableOperations(Chip.YELLOW).filterIsInstance<Operation.Place>().map { it.column })
    }

    @Test
    fun testBoardIsFull() {
        assertEquals(false, boardEmpty.boardIsFull())
        assertEquals(false, boardWinRed.boardIsFull())
        assertEquals(false, boardWithFullLastColumn.boardIsFull())
        assertEquals(true, boardFullTie.boardIsFull())
    }

    @Test
    fun testWinner() {
        assertEquals(null, boardEmpty.winner()?.chip)
        assertEquals(Chip.RED, boardWinRed.winner()?.chip)
        assertEquals(Chip.EMPTY, boardFullTie.winner()?.chip)
        assertEquals(listOf(Position(0, 5), Position(1, 5), Position(2, 5), Position(3, 5)), boardWinRed.winner()?.positions)
    }
}
